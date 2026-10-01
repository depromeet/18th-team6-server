# #196 / 02: Pi CI/CD 및 모니터링 배포

상태: 워크플로 구현 및 설정 검사 완료, 현재 커밋의 Actions 배포 검증 대기

## 목표

기존 Docker Hub 빌드·푸시와 모니터링 배포 흐름을 유지하면서 배포 대상을 Pi로 바꾼다. 앱 배포가 MySQL 컨테이너나 데이터를 내리지 않도록 한다.

## 배포 흐름

1. ARM 러너에서 이미지를 빌드해 Docker Hub의 `latest` 태그로 게시
2. SSH로 기존 Compose, Pi 오버레이, 모니터링 설정을 Pi의 `~/deploy`에 복사
3. GitHub Secrets로 앱·모니터링 `.env`, Discord webhook 파일, Firebase 자격증명 JSON 생성
4. 두 Compose 파일의 병합 설정을 검사하고 앱 이미지를 pull
5. MySQL을 기동한 뒤 앱을 갱신하고 `/actuator/health` 및 Firebase 초기화 실패 로그 확인
6. 기존 Prometheus·Alertmanager·Grafana Compose 기동

SSH 포트는 `SERVER_PORT` Secret만 사용한다. 배포 경로에는 `docker compose down`과 전체 서비스 `--force-recreate`가 없다.

## 필요한 GitHub Secrets

| 용도 | 이름 |
| --- | --- |
| Pi 접속 | `SERVER_HOST`, `SERVER_USER`, `SERVER_PORT`, `SERVER_SSH_KEY` |
| Docker Hub | `DOCKER_USERNAME`, `DOCKER_REPO`, `DOCKER_PWD` |
| 앱·MySQL 설정 | `ENV_FILE` |
| Firebase 서비스 계정 JSON 원문 | `FIREBASE_CREDENTIALS_JSON` |
| 모니터링 | `MONITORING_ENV_FILE`, `MONITORING_DISCORD_WEBHOOK_URL` |

`FIREBASE_CREDENTIALS_JSON`이 비어 있으면 빌드 전에 실패한다. 워크플로는 JSON의 서비스 계정 필드를 확인하고 `umask 077`을 적용해 `~/deploy/firebase-credentials.json`으로 저장한 뒤 앱에 마운트한다. JSON과 `.env`는 저장소나 Docker 이미지에 넣지 않는다.

## 검증 기록과 남은 확인

- 워크플로 YAML과 셸 문법 검사 통과
- Pi에서 두 Compose 파일의 병합 `config --quiet` 통과
- Pi에서 Docker Hub 이미지 manifest 접근 확인
- 이전 워크플로의 Actions 배포 성공은 더미 Firebase 파일과 앱·MySQL만 확인한 결과임
- 현재 커밋은 Actions 재실행 전이므로 실제 Docker Hub pull, Firebase 초기화, 모니터링 기동은 미검증

실제 배포 후 앱·MySQL·모니터링 컨테이너 상태와 Firebase 알림을 확인한다. MinIO 배포, RDS/S3 데이터 이전, SHA 이미지 태그와 롤백은 후속 범위다.
