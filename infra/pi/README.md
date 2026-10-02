# Pi 배포 설정

기존 `docker-compose.yml`의 앱 설정을 유지하고 `docker-compose.pi.yml`을 겹쳐 MySQL만 추가한다. 앱 서비스 `app`, 컨테이너 `orbit-server`, 네트워크 `orbit-network`는 그대로 사용한다.

## Actions 배포 흐름

`.github/workflows/ci-cd.yml`은 ARM 러너에서 Docker Hub에 이미지를 게시하고 두 Compose 파일과 모니터링 설정을 Pi에 복사한다. 서버에서는 앱 이미지를 pull하고 기존 MySQL 컨테이너를 유지한 채 `healthy` 상태를 확인한 다음 앱만 갱신한다. Compose에서 실제 앱 호스트 포트를 조회해 `/actuator/health`를 확인하고 기존 Prometheus·Alertmanager·Grafana 설정을 기동한다. 앱 배포에서 `docker compose down`은 실행하지 않는다. MySQL 설정이나 이미지 변경은 별도 유지보수 절차에서 반영한다.

SSH 연결은 `SERVER_HOST`, `SERVER_USER`, `SERVER_PORT`, `SERVER_SSH_KEY` Secret을 사용한다. Docker Hub에는 `DOCKER_USERNAME`, `DOCKER_REPO`, `DOCKER_PWD`가 필요하다. 앱·MySQL 설정은 `ENV_FILE`, Firebase 서비스 계정 JSON 원문은 `FIREBASE_CREDENTIALS_JSON`, 모니터링은 `MONITORING_ENV_FILE`과 `MONITORING_DISCORD_WEBHOOK_URL`에서 받는다.

`infra/pi/env.example`은 `ENV_FILE`의 키 예시다. 워크플로가 `APP_IMAGE`와 `FIREBASE_CREDENTIALS_HOST_PATH`를 배포 시 추가한다. Firebase JSON은 `umask 077`을 적용해 `~/deploy/firebase-credentials.json`에 저장한다.

## Compose 설정

Pi 오버레이는 MySQL `8.4.11`과 `orbit-mysql-data` Docker 볼륨을 사용한다. MySQL은 `orbit-network`의 `mysql:3306`에서 앱에 연결되며 호스트에 3306 포트를 공개하지 않는다. 앱은 MySQL healthcheck가 통과한 뒤 시작한다.

두 파일을 함께 검사할 때는 다음 명령을 사용한다. 이 명령은 컨테이너를 실행하지 않는다.

```bash
docker compose --env-file .env -f docker-compose.yml -f docker-compose.pi.yml config --quiet
```

현재 `DB_ROOT_PASSWORD`도 `ENV_FILE`에 있어 기본 Compose의 `app.env_file`을 통해 앱에 전달된다. 앱 환경에서 root 암호를 분리하는 작업이 남아 있다. RDS 데이터 복원, 외부 백업, MinIO 이전도 후속 작업이다.

현재 커밋의 Actions 배포는 아직 재실행하지 않았다. 이전 Actions 성공은 더미 Firebase를 사용한 앱·MySQL 기동 확인이므로, 실제 Firebase Secret과 모니터링까지 새 워크플로에서 검증해야 한다.
