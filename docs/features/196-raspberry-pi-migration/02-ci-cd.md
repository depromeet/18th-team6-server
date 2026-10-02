# #196 / 02: Pi CI/CD 및 모니터링 배포

상태: Firebase Secret을 사용한 Pi Actions 배포 검증 완료

## 목표

기존 Docker Hub 빌드·푸시와 모니터링 배포 흐름을 유지하면서 배포 대상을 Pi로 바꾼다. 앱 배포가 MySQL 컨테이너나 데이터를 내리지 않도록 한다.

## 배포 흐름

1. ARM 러너에서 이미지를 빌드해 Docker Hub의 `latest` 태그로 게시
2. SSH로 기존 Compose, Pi 오버레이, 모니터링 설정을 Pi의 `~/deploy`에 복사
3. GitHub Secrets로 앱·모니터링 `.env`, MySQL root 전용 `.env.mysql`, Discord webhook 파일, Firebase 자격증명 JSON 생성
4. 두 Compose 파일의 병합 설정을 검사하고 앱 이미지를 pull
5. Firebase JSON을 `0600`으로 유지하면서 이미지의 앱 사용자에게 소유권을 넘기고 읽기 가능 여부를 확인
6. MySQL은 기존 컨테이너를 재생성하지 않고 기동해 `healthy`를 확인한 뒤, 의존 서비스를 건드리지 않고 앱만 갱신
7. Compose에서 앱의 실제 호스트 포트를 조회해 `/actuator/health` 및 Firebase 초기화 실패 로그 확인
8. 기존 Prometheus·Alertmanager·Grafana Compose 기동

SSH 포트는 `SERVER_PORT` Secret만 사용한다. 배포 경로에는 `docker compose down`과 전체 서비스 `--force-recreate`가 없다.
MySQL 설정이나 이미지 변경은 일반 앱 배포에서 적용되지 않으며 별도 유지보수 절차가 필요하다.
`ENV_FILE`의 `DB_ROOT_PASSWORD`는 서버에서 `.env.mysql`의 `MYSQL_ROOT_PASSWORD`로 옮기므로 앱 컨테이너에는 전달되지 않는다.

## 필요한 GitHub Secrets

| 용도 | 이름 |
| --- | --- |
| Pi 접속 | `SERVER_HOST`, `SERVER_USER`, `SERVER_PORT`, `SERVER_SSH_KEY` |
| Docker Hub | `DOCKER_USERNAME`, `DOCKER_REPO`, `DOCKER_PWD` |
| 앱·MySQL 설정 | `ENV_FILE` |
| Firebase 서비스 계정 JSON 원문 | `FIREBASE_CREDENTIALS_JSON` |
| 모니터링 | `MONITORING_ENV_FILE`, `MONITORING_DISCORD_WEBHOOK_URL` |

`FIREBASE_CREDENTIALS_JSON`이 비어 있으면 빌드 전에 실패한다. 워크플로는 JSON의 서비스 계정 필드를 확인하고 `umask 077`을 적용해 `~/deploy/firebase-credentials.json`으로 저장한 뒤 앱에 마운트한다. JSON과 `.env`는 저장소나 Docker 이미지에 넣지 않는다.
앱 이미지는 비루트 사용자로 실행되므로, 배포할 때 이미지에서 사용자 UID를 조회해 자격증명 파일의 소유자로 설정한다.

## 검증 기록과 남은 확인

- 워크플로 YAML과 셸 문법 검사 통과
- Pi에서 두 Compose 파일의 병합 `config --quiet` 통과
- 이전 워크플로의 Actions 배포 성공은 더미 Firebase 파일과 앱·MySQL만 확인한 결과임
- 첫 실제 Firebase Secret 배포는 호스트 파일의 소유자가 앱 사용자와 달라 초기화에 실패함. 읽기 전용 SSH에서 `Permission denied`를 확인하고 파일 소유권을 수정함
- [수정 후 Actions 배포](https://github.com/depromeet/18th-team6-server/actions/runs/36987283270) 성공: Docker Hub pull, Firebase 파일 읽기 검사, MySQL `healthy`, 앱 `/actuator/health` `UP`, 모니터링 기동 명령 통과
- 배포 후 읽기 전용 SSH 확인: Firebase 파일은 앱 사용자 소유 `0600`, 앱·모니터링 컨테이너 실행 중, MySQL 컨테이너는 기존 기동 시각 유지

Firebase 알림 실제 발송, 모니터링 데이터 수집, DB 읽기·쓰기와 데이터 유지 검증은 남아 있다. MinIO 배포, RDS/S3 데이터 이전, SHA 이미지 태그와 롤백은 후속 범위다.
