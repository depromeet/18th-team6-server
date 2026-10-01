# #196 / 01: Pi용 앱·MySQL Compose

상태: Compose 구현 및 설정 검사 완료, 현재 CI/CD 커밋의 실제 배포 검증 대기

## 목표

기존 `docker-compose.yml`의 앱 정의를 유지하고 `docker-compose.pi.yml`을 겹쳐 Pi에서 MySQL과 앱을 실행한다. 서비스 이름 `app`, 컨테이너 이름 `orbit-server`, 네트워크 이름 `orbit-network`를 유지한다.

## 현재 구현

- Pi 오버레이에서 앱의 `DB_URL`을 `mysql:3306`으로 지정하고 MySQL이 `healthy`가 된 뒤 앱을 시작
- MySQL `8.4.11`을 사용하고 데이터를 이름 있는 볼륨 `orbit-mysql-data`에 저장
- MySQL 3306 포트를 호스트에 공개하지 않음
- `DB_ROOT_PASSWORD`, `DB_DATABASE`, `DB_USERNAME`, `DB_PWD`를 Actions의 `ENV_FILE`에서 전달
- 앱 배포에서 Compose `down`을 실행하지 않고 MySQL 컨테이너와 볼륨을 의도적으로 내리지 않음

CI/CD와 모니터링 배포 절차는 [02-ci-cd.md](02-ci-cd.md)에 정리한다. MinIO와 RDS 데이터 복원은 후속 작업이다.

## 파일과 설정

| 파일 | 역할 |
| --- | --- |
| `docker-compose.yml` | 기존 앱 정의. 이번 변경에서 수정하지 않음 |
| `docker-compose.pi.yml` | 앱 DB 주소와 MySQL 서비스·볼륨 추가 |
| `.github/workflows/ci-cd.yml` | 두 Compose 파일을 함께 서버에 전달하고 앱·MySQL 기동 |

현재는 MySQL root 암호도 앱이 읽는 `.env`에 들어간다. 기본 Compose의 `app.env_file`이 이 파일 전체를 앱에 전달하므로, 운영 설정에서는 root 암호를 앱 환경에서 분리하는 작업이 남아 있다. 저장소에는 `.env`와 자격증명 파일을 올리지 않는다.

## 검증 기록

- Pi의 Docker Compose에서 두 파일을 합친 `config --quiet` 통과
- 이전 버전의 Actions 실행에서 Pi 앱 기동, MySQL `healthy`, `/actuator/health` `UP` 확인
- 현재 커밋의 Docker Hub pull 및 두 Compose 파일을 사용한 배포는 아직 실행하지 않음

## 완료 기준

- 현재 커밋의 Actions 배포에서 MySQL `healthy`와 앱 헬스 체크 `UP` 확인
- 앱 재배포 전후 MySQL 컨테이너와 `orbit-mysql-data` 데이터 유지 확인
- 앱의 `mysql:3306` 접속과 MySQL 호스트 포트 미공개 확인
- RDS MySQL 버전 및 기존 데이터 복원 호환성 별도 확인

## 참고

- [Docker Compose 파일 병합](https://docs.docker.com/compose/how-tos/multiple-compose-files/merge/)
- [Docker Compose 서비스 준비 순서](https://docs.docker.com/compose/how-tos/startup-order/)
