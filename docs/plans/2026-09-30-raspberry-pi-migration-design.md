# AWS에서 라즈베리파이로 이전: 설계 초안

- 작성일: 2026-09-30
- 현황 갱신: 2026-10-02
- 범위: 단계별 이전 설계. 앱·MySQL Compose와 CI/CD 변경은 구현했으며 MinIO와 실제 데이터 이전은 후속 단계다.

## 1. 목표와 전제

- 애플리케이션, MySQL, MinIO, 기존 모니터링 스택을 라즈베리파이의 Docker Compose로 운영한다.
- 현재 RDS 데이터와 S3 객체를 보존하고, 앱의 이미지 업로드 및 조회 동작을 이전 후에도 유지한다.
- 앱 배포가 DB와 객체 저장소를 재시작하거나 삭제하지 않도록 한다.
- Firebase 알림과 외부 OCR API는 계속 외부 서비스를 사용한다. 이들의 이전은 이번 범위에 없다.
- Pi의 CPU 아키텍처는 `aarch64`로 확인했다. 모델, RAM, 저장 장치, 외부 접속 방식, 현재 데이터 크기는 추가 확인이 필요하다.

## 2. 저장소에서 확인한 현재 상태

| 영역 | 현재 구현 | 이전 시 영향 |
| --- | --- | --- |
| 앱 배포 | 기존 `docker-compose.yml`의 앱 정의에 `docker-compose.pi.yml`을 겹쳐 Pi에서 실행한다. Firebase JSON은 GitHub Secret에서 호스트 파일로 만든다. | Actions 배포와 파일 읽기 검사 및 초기화 실패 로그 부재를 확인했다. 실제 알림 발송은 추가 확인이 필요하다. |
| DB | Pi 오버레이에 MySQL `8.4.11`, `orbit-mysql-data` 볼륨, `mysql:3306` 앱 연결과 healthcheck가 있다. `application-prod.yml`은 Flyway와 JPA `validate`를 사용한다. | RDS 데이터와 `flyway_schema_history` 복원, MySQL 버전 호환성, 외부 백업은 아직 확인해야 한다. |
| 스키마 | `V1`은 기존 운영 DB를 위한 baseline 설명이 있고 `baseline-on-migrate: true`, `baseline-version: 1`이다. 현재 V11까지 있다. | 빈 DB와 기존 덤프 복원 DB의 Flyway 동작을 각각 검증해야 한다. 기존 V1을 수정하지 않는다. |
| 객체 저장 | `S3Config`, `S3Uploader`, 두 URL resolver가 `prod`에 고정되어 있다. 업로드는 AWS SDK 기본 자격 증명 체인을 쓴다. | MinIO 내부 endpoint, 인증, path-style 접근, 브라우저용 URL을 분리해야 한다. |
| 아이콘 | `icons.icon_key`와 `icons.url`이 저장된다. API는 주로 `icon_key`로 S3 공개 URL을 만들고, 백오피스 화면은 저장된 `icons.url`을 직접 읽는다. | 기존 키의 객체 이전과 저장된 AWS URL의 변환을 모두 설계해야 한다. |
| 영수증 | 업로더는 `receipts/...` **객체 키**를 반환한다. `AnalyzeReceiptResponse.receiptImageUrl`과 `items.receipt_image_url`에 이 값이 전달/저장된다. | 필드명은 URL이지만 현재 값은 키다. 데이터 변환 없이 일괄 URL 치환하면 안 된다. 조회 API의 실제 사용처를 확인해야 한다. |
| CI/CD | ARM 러너의 Docker Hub `latest` 푸시와 모니터링 배포를 유지한다. Pi에는 기존 Compose와 오버레이를 복사하고 앱 이미지를 pull한다. `down`과 전체 `--force-recreate`는 제거했다. | 실제 Actions 배포는 성공했다. SHA 태그, 롤백과 검증 게이트가 남아 있다. |
| 검증 | `.github/workflows/harness.yml`은 PR/main에서 `./gradlew build`를 실행한다. 배포 워크플로에는 검증 통과 의존성이 없다. | 배포 전에 검증이 통과하도록 워크플로 의존 관계 또는 브랜치 보호 규칙을 정해야 한다. |
| 모니터링 | 기존 `infra/monitoring/docker-compose.yml`과 Secrets 생성 단계를 유지하고 Pi의 `orbit-network`에 연결한다. | 새 워크플로에서 세 컨테이너 실행을 확인했다. `app:8080` 수집은 추가 확인이 필요하다. |
| 설정 예시 | 루트의 미추적 `.env.example`은 현재 운영 설정과 다른 항목이 있다. Pi용 설정 계약은 `infra/pi/env.example`에 별도로 기록한다. | 실제 `ENV_FILE` 값과 서비스 환경 변수의 차이를 확인해야 한다. |

[Firebase Secret을 사용한 Actions 배포](https://github.com/depromeet/18th-team6-server/actions/runs/36987283270)에서 Docker Hub pull, 앱 헬스 체크, 모니터링 기동이 통과했다. 배포 후 MySQL 컨테이너의 기존 기동 시각과 `healthy` 상태를 확인했다. 실제 Firebase 알림 발송과 모니터링 수집은 아직 확인하지 않았다.

### 권장 구성

```text
인터넷 ── HTTPS 역방향 프록시 ── app:8080
                                  ├── mysql:3306 (내부 네트워크)
                                  └── minio:9000 (내부 네트워크)
                                             └── 브라우저 이미지 조회 경로는 별도 설계

Pi 저장 장치 ── MySQL 데이터 / MinIO 객체 / 모니터링 데이터
별도 저장 위치 ── 주기적 DB·객체 백업
```

프록시, 도메인, TLS, 공유기 포트 포워딩 또는 터널 방식은 호스트 조건을 확인해 결정한다. MySQL 3306과 MinIO 관리 콘솔 9001은 인터넷에 공개하지 않는다. 앱과 모니터링의 현재 포트 공개 범위도 함께 재검토한다.

## 3. 변경 설계

### 3.1 Compose와 호스트

1. 기존 루트 Compose는 앱 정의로 유지하고 Pi 오버레이에 MySQL을 추가했다. 현재 MySQL은 이름 있는 볼륨을 사용한다. MinIO와 안정적인 호스트 저장 장치 연결은 후속 단계에서 설계한다.
2. `mysql`은 서버/DB/앱 계정과 인증 값을 운영 설정에서 받고, `DB_URL=jdbc:mysql://mysql:3306/<database>...`처럼 Compose DNS를 사용한다. 문자셋, 시간대, MySQL 버전/SQL 모드를 RDS와 비교한다. 초기화 변수는 **빈 볼륨에서만** 적용되므로 복원 절차와 구분한다.
3. `minio`는 데이터 디렉터리, 관리 자격 증명, 운영용 버킷 및 앱 전용 접근 키를 준비한다. 버킷 생성/권한 설정은 반복 실행해도 안전한 초기화 단계로 둔다. 루트 계정을 앱에서 사용하지 않는다.
4. 앱은 MySQL 준비 완료를 healthcheck로 확인한 뒤 시작한다. MinIO도 준비 상태를 확인하고 업로드/조회 스모크 테스트로 정상 동작을 검증한다. `depends_on`은 기동 시점 준비를 돕지만 운영 중 장애 복구와 재시도 설계를 대체하지 않는다.
5. 앱/DB/MinIO는 내부 네트워크로 통신한다. 앱 공개 포트는 프록시를 확정한 뒤 루프백 바인딩 또는 내부 전용으로 조정한다. 모니터링의 `orbit-network` 연결과 `app:8080` 대상은 유지한다.
6. 현재 MySQL은 `orbit-mysql-data` Docker 볼륨에 저장된다. Pi의 디스크가 SD 카드 단독 운영인지 확인하고, 외장 SSD와 백업 위치를 결정한다. 메모리와 디스크 여유를 측정해 JVM heap, MySQL buffer pool, MinIO/모니터링 용량을 정한다.

### 3.2 S3에서 MinIO로

1. 기존 `FileUploader`와 `UrlResolver` 계약을 유지하되 설정 이름을 공급자 중립적으로 정리한다. AWS SDK S3 클라이언트의 endpoint, region, path-style, access key/secret을 **명시적으로** 구성해 MinIO와 연결한다. 내부 API endpoint(`http://minio:9000`)를 브라우저용 URL로 반환하지 않는다.
2. 아이콘은 현재 공개 URL이 API 응답에 포함되므로 공개 조회 방식을 먼저 결정한다. 권장안은 HTTPS 프록시의 이미지 전용 경로/도메인으로 읽기 전용 접근을 제공하고, 쓰기와 콘솔은 내부에 둔다. 대안은 앱 프록시 또는 서명 URL이며, 캐시·만료·권한 요구에 따라 선택한다.
3. 영수증은 개인정보가 포함될 수 있어 비공개 버킷/경로를 기본으로 둔다. 현재 저장 값이 키인지 URL인지 실데이터와 클라이언트 사용처를 조사한다. 조회 기능이 필요하면 인증된 다운로드 API 또는 제한 시간 서명 URL을 별도 계약으로 정의한다. 서명 URL 방식을 택하면 서명 시 사용한 호스트가 외부 HTTPS 주소와 일치하도록 설계한다. 기존 `S3PresignedUrlResolver`는 현재 참조처가 검색되지 않았다.
4. `icons.icon_key`는 동일 키로 객체를 복사하면 DB 변경을 줄일 수 있다. 백오피스가 `icons.url`을 직접 읽으므로 기존 AWS 주소를 새 공개 주소로 바꾸는 데이터 이전을 준비한다. `items.receipt_image_url`은 값 형식을 분류한 후 필요한 데이터만 변환한다.
5. 이전 시 S3 객체 목록/크기/해시 또는 샘플 GET을 기록하고 MinIO에 복사한 뒤 대조한다. 마지막 쓰기 중지 구간에서 변경분을 재동기화한다. 버킷 정책과 CORS는 실제 클라이언트 조회 방식에 맞춰 결정한다.

### 3.3 CI/CD

1. 현재 ARM 러너에서 앱 이미지를 Docker Hub에 `latest`로 푸시하고 Pi에서 pull한다. Docker Hub 이미지 manifest는 Pi에서 접근 가능함을 확인했다. `bootJar` 후 Dockerfile에서 다시 빌드하는 중복은 남아 있다.
2. 기존 Compose와 Pi 오버레이를 함께 복사한다. `SERVER_PORT`는 Secret으로만 받으며, `ENV_FILE`의 MySQL root 암호는 MySQL 전용 `.env.mysql`로 분리한다. 모니터링 Secrets는 기존 방식을 유지한다. 새 `FIREBASE_CREDENTIALS_JSON` Secret은 서비스 계정 JSON을 검증한 뒤 Pi의 파일로 저장한다. 원격 heredoc에 Secret을 넣는 방식의 인용·로그 노출 위험은 별도 검토가 필요하다.
3. 앱 배포에서 Compose 설정 검사와 이미지 pull 후 Firebase 파일의 앱 사용자 읽기 권한을 확인한다. MySQL은 `--no-recreate --wait`로 유지·검사하고 앱은 `--no-deps`로 갱신한다. `down`과 전체 `--force-recreate`는 사용하지 않는다. 기존 모니터링 설정을 복사하고 앱 확인 후 모니터링 Compose를 기동한다.
4. 현재 성공 조건은 실제 호스트 포트의 앱 `/actuator/health` `UP`과 Firebase 초기화 실패 로그 부재다. [Firebase Secret을 사용한 Actions 배포](https://github.com/depromeet/18th-team6-server/actions/runs/36987283270)가 성공했고 기존 MySQL 컨테이너가 유지됨을 확인했다. 실제 DB 읽기/쓰기, Firebase 알림 발송, 모니터링 수집 및 객체 업로드 확인은 추가한다. Firebase Secret이 없으면 빌드 전에 실패한다.
5. 다음 CI 개선에서 커밋 SHA 이미지 태그와 롤백 경로를 마련한다. 실패 시 이전 앱으로 복귀하려면 Flyway 스키마 변경과 데이터 호환성도 함께 판단한다.
6. `harness.yml` 검증과 배포 워크플로는 여전히 분리되어 있다. 검증 완료 후 배포되도록 의존 관계 또는 브랜치 보호 규칙을 확정한다.

### 3.4 백업, 보안, 운영

- MySQL은 일관된 논리 백업 또는 물리 백업 방식을 선택하고, `flyway_schema_history`까지 포함한다. MinIO 객체와 버킷 정책/계정 설정도 백업 대상이다. 백업은 Pi 밖의 저장 위치에 두고, **복원 시험**을 완료 조건에 넣는다.
- 이전 전 RDS 스냅샷과 S3 원본 보존 기간을 정한다. 단일 Pi/단일 디스크는 장애 시 서비스 전체가 중단되는 구조이므로 전원, 저장 장치, 백업 복구 시간을 운영 기준으로 잡는다.
- 도메인/DNS, 고정 IP 여부, CGNAT 여부, TLS 인증서 자동 갱신, 방화벽/SSH 접근 방법을 확정한다. Firebase/외부 OCR/Discord 알림의 Pi outbound 접근을 검증한다.
- 기존 Prometheus는 `app:8080`의 앱 메트릭을 수집한다. MySQL/MinIO 상태와 Pi의 디스크 용량·온도·메모리 경보, 백업 실패 알림을 추가하고 모니터링 데이터 보존 기간을 Pi 용량에 맞춘다.

## 4. 실제 이전 순서와 실패 시 복귀

1. **사전 조사:** Pi 하드웨어/OS/네트워크/디스크, RDS MySQL 버전·DB 크기·계정·SQL 모드, S3 버킷 크기/객체 수, 실제 `icons.url`·`receipt_image_url` 값 형식, 외부 앱의 이미지 URL 사용처를 확인한다. 이전 허용 중단 시간을 정한다.
2. **사전 구축:** Pi에 Docker/Compose, 영속 저장 장치, HTTPS 진입점, 비밀 파일, 백업 위치를 준비한다. 고정 이미지 버전으로 Compose를 기동하고 새 빈 DB의 Flyway 동작을 별도 시험한다.
3. **리허설:** 운영 복제 데이터로 RDS 덤프를 Pi MySQL에 복원하고 S3 객체를 MinIO에 복사한다. 행 수/주요 데이터/객체 수와 이미지 GET·업로드·알림·OCR·모니터링을 비교한다. 복원 DB에서 Flyway가 기존 이력을 그대로 인식하는지 확인한다.
4. **최종 동기화:** 쓰기 경로를 중지하거나 점검 모드로 전환한다. 최종 DB 백업과 S3 변경 객체를 반영한다. 동기화 시각 이후 데이터 차이가 없음을 확인한다.
5. **전환:** Pi 앱을 검증한 뒤 DNS/프록시를 전환한다. `/actuator/health`, 읽기/쓰기 API, 아이콘/영수증 이미지, 백오피스, 알림, Prometheus를 확인한다. DNS TTL과 클라이언트 캐시를 고려해 관찰한다.
6. **복귀 조건:** 핵심 API 또는 DB/객체 무결성 검증 실패 시 새 쓰기를 중지하고 이전 AWS 앱으로 트래픽을 되돌린다. Pi에서 이미 발생한 쓰기가 있으면 RDS/S3로 역동기화할 방법을 정하기 전까지 단순 DNS 복귀로 종료하지 않는다. AWS 원본은 안정화 기간까지 유지한다.

## 5. 후속 스펙 분할과 완료 기준

| 순서 | 스펙 | 주요 산출물·완료 기준 |
| --- | --- | --- |
| 0 | 호스트·데이터 조사 및 결정 기록 | Pi 사양, 저장 장치, 외부 접속 방식, 데이터 크기/URL 값 샘플, 중단 허용 시간과 복귀 기준 확정 |
| 1 | Compose MySQL 기반 | Pi 오버레이와 healthcheck 구현, Actions 배포 성공 및 MySQL 컨테이너 유지 확인; 데이터 내용 검증 필요 |
| 2 | 객체 저장소 어댑터와 URL 계약 | MinIO 업로드/읽기, 아이콘 공개 경로, 영수증 비공개 경로; 계약 테스트와 실제 Pi 스모크 테스트 |
| 3 | CI/CD와 배포 스크립트 | Pi 배포와 모니터링 배포 반영; SHA 이미지, 검증 게이트, 롤백 및 데이터 서비스 유지 확인 필요 |
| 4 | 백업·복원과 모니터링 | 외부 백업, 복원 리허설, DB/MinIO/호스트 경보, 운영 문서 |
| 5 | RDS/S3 데이터 이전과 트래픽 전환 | 리허설 기록, 최종 동기화/검증 체크리스트, 전환 및 복귀 실행 기록 |

## 6. 먼저 확정할 결정

| 결정 | 필요한 정보 | 권장 출발점 |
| --- | --- | --- |
| Pi 기종/OS/저장 장치 | `aarch64` 확인; RAM, SSD·전원·디스크 여유 확인 필요 | 외장 SSD + 외부 백업 |
| 외부 접속 | 도메인, 공인 IP/CGNAT, 포트 포워딩 가능 여부 | HTTPS 프록시 또는 인증된 터널; DB/관리 포트 비공개 |
| 아이콘/영수증 접근 | 클라이언트가 어떤 URL을 읽는지, 영수증 접근 권한 | 아이콘 읽기 전용 공개, 영수증 인증된 비공개 조회 |
| 가용성과 복구 | 허용 다운타임, 데이터 유실 허용치, AWS 유지 기간 | 사전 리허설 + 최종 쓰기 중지 + 백업 복원 시험 |
| 이미지·DB 버전 | RDS 버전/설정, Pi 아키텍처, 각 이미지 manifest | 호환 버전 고정 후 실제 Pi에서 검증 |

## 참고한 공식 문서

- [Docker Compose 서비스 준비 순서와 healthcheck](https://docs.docker.com/compose/how-tos/startup-order/)
- [Docker Compose `up` 옵션](https://docs.docker.com/reference/cli/docker/compose/up/)
- [Docker 다중 아키텍처 이미지](https://docs.docker.com/build/building/multi-platform/)
- [MinIO 단일 노드·단일 드라이브의 제약](https://min.io/docs/minio/container/operations/install-deploy-manage/deploy-minio-single-node-single-drive.html)
- [AWS SDK for Java 2.x 자격 증명 체인](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials-chain.html)
