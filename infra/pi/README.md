# Pi 배포 설정

기존 `docker-compose.yml`의 앱 설정을 유지하고 `docker-compose.pi.yml`을 겹쳐 MySQL만 추가한다. 앱 서비스 `app`, 컨테이너 `orbit-server`, 네트워크 `orbit-network`는 그대로 사용한다.

## Actions 배포 흐름

`.github/workflows/ci-cd.yml`은 ARM 러너에서 Docker Hub에 이미지를 게시하고 두 Compose 파일과 모니터링 설정을 Pi에 복사한다. 서버에서는 앱 이미지를 pull하고 기존 MySQL 컨테이너를 유지한 채 `healthy` 상태를 확인한 다음 앱만 갱신한다. Compose에서 실제 앱 호스트 포트를 조회해 `/actuator/health`를 확인하고 기존 Prometheus·Alertmanager·Grafana 설정을 기동한다. 앱 배포에서 `docker compose down`은 실행하지 않는다. MySQL 설정이나 이미지 변경은 별도 유지보수 절차에서 반영한다.

SSH 연결은 `SERVER_HOST`, `SERVER_USER`, `SERVER_PORT`, `SERVER_SSH_KEY` Secret을 사용한다. Docker Hub에는 `DOCKER_USERNAME`, `DOCKER_REPO`, `DOCKER_PWD`가 필요하다. 앱·MySQL 설정은 `ENV_FILE`, Firebase 서비스 계정 JSON 원문은 `FIREBASE_CREDENTIALS_JSON`, 모니터링은 `MONITORING_ENV_FILE`과 `MONITORING_DISCORD_WEBHOOK_URL`에서 받는다.

`infra/pi/env.example`은 `ENV_FILE`의 키 예시다. 워크플로는 `DB_ROOT_PASSWORD`를 MySQL 전용 `.env.mysql`로 옮기고 앱 `.env`에서 제거한 뒤, `APP_IMAGE`와 `FIREBASE_CREDENTIALS_HOST_PATH`를 추가한다. Firebase JSON은 `umask 077`을 적용해 `~/deploy/firebase-credentials.json`에 저장하고, 앱 이미지의 사용자에게 파일 소유권을 넘겨 `0600` 권한으로 읽을 수 있게 한다.
`DB_ROOT_PASSWORD`는 `ENV_FILE`에서 따옴표 없는 `KEY=value` 형식으로 입력한다. Pi의 Compose는 MySQL 전용 파일을 `raw` 형식으로 읽어 `$` 등의 문자를 보간하지 않는다(Compose 2.30 이상 필요; 확인한 Pi 버전은 5.5.1).

## Compose 설정

Pi 오버레이는 MySQL `8.4.11`과 `orbit-mysql-data` Docker 볼륨을 사용한다. MySQL은 `orbit-network`의 `mysql:3306`에서 앱에 연결되며 호스트에 3306 포트를 공개하지 않는다. 앱은 MySQL healthcheck가 통과한 뒤 시작한다.

두 파일을 함께 검사할 때는 다음 명령을 사용한다. 이 명령은 컨테이너를 실행하지 않는다.

```bash
docker compose --env-file .env -f docker-compose.yml -f docker-compose.pi.yml config --quiet
```

앱 `.env`와 MySQL `.env.mysql`은 서버에서만 보관한다. RDS 데이터 복원과 외부 백업은 후속 작업이다.

## 파일 저장소

`STORAGE_TYPE`으로 저장소를 고른다. 기본값 `filesystem`은 Pi 디스크에, `s3`는 기존 AWS S3에 저장한다(S3 코드는 되돌릴 수 있게 유지한다). `filesystem`에서 앱은 `orbit-storage-data` Docker 볼륨을 `/app/storage`에 마운트하고 업로드 키(`category-icon/...`, `receipts/...`)를 그대로 상대 경로로 쓴다. 아이콘만 `GET /files/category-icon/**`로 공개하고 URL은 `STORAGE_PUBLIC_BASE_URL` + `/files/` + 키로 만든다. 영수증은 공개 경로가 없다.

`filesystem`에서는 `STORAGE_PUBLIC_BASE_URL`이 `ENV_FILE`에 필수다. 없으면 앱이 기동하지 않으므로 이 변경을 배포하기 전에 Secret에 추가한다. `S3_BUCKET`, `S3_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`는 `STORAGE_TYPE=s3`일 때만 필요하다.

### S3 백업 복원

S3 백업 폴더는 버킷 구조 그대로 `category-icon/`과 `receipts/`를 담고 있어 DB의 `icon_key`를 바꾸지 않아도 된다. 앱을 한 번 배포해 Compose가 볼륨을 만든 뒤 진행한다.

```bash
# 로컬에서 백업을 Pi로 복사
scp -P <SERVER_PORT> -r ~/Downloads/s3-backup <SERVER_USER>@<SERVER_HOST>:~/s3-backup

# Pi의 ~/deploy에서 볼륨에 복사하고 앱 사용자에게 소유권을 준다
app_image=$(grep '^APP_IMAGE=' .env | cut -d= -f2)
app_uid=$(docker run --rm --network none --entrypoint id "$app_image" -u)
docker run --rm --network none --user 0 \
  --mount type=volume,source=orbit-storage-data,target=/app/storage \
  --mount type=bind,source=$HOME/s3-backup,target=/backup,readonly \
  --entrypoint sh "$app_image" -c "cp -a /backup/. /app/storage/ && chown -R $app_uid /app/storage"

# 파일 수 확인 (백업 기준 아이콘 81, 영수증 83)
docker exec orbit-server sh -c 'find /app/storage/category-icon -type f | wc -l; find /app/storage/receipts -type f | wc -l'
```

RDS 데이터를 복원한 뒤 백오피스가 직접 읽는 `icons.url`의 S3 주소를 새 주소로 바꾼다. `<STORAGE_PUBLIC_BASE_URL>`은 `.env`의 값으로 바꿔 넣는다.

```sql
UPDATE icons
SET url = CONCAT('<STORAGE_PUBLIC_BASE_URL>/files/', icon_key)
WHERE url LIKE 'https://%.amazonaws.com/%';
```

확인: `curl -I <STORAGE_PUBLIC_BASE_URL>/files/<icon_key>`가 200, `/files/receipts/<파일>`이 200이 아니어야 한다. 백오피스에서 아이콘을 하나 업로드해 볼륨 쓰기 권한도 확인한다.

[Firebase Secret을 사용한 Actions 배포](https://github.com/depromeet/18th-team6-server/actions/runs/36987283270)가 성공했다. 배포 후 앱·모니터링 실행과 기존 MySQL 컨테이너 유지 및 `healthy` 상태를 읽기 전용으로 확인했다. Firebase 알림 실제 발송, 모니터링 데이터 수집, DB 데이터 유지 검증은 남아 있다.
