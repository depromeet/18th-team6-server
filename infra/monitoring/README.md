# Obrit Monitoring Stack

Prometheus + Alertmanager + Grafana 사이드카로 RED 메트릭 수집과 다운 알림을 처리한다.

## 구성

```
infra/monitoring/
├── docker-compose.yml                       # 3개 컨테이너를 루트 orbit-network에 외부 참여
├── prometheus.yml                           # 15초 주기 스크랩 / alerts.yml 룰 / alertmanager:9093으로 발사
├── alerts.yml                               # ObritDown(up==0, 5m) -> 서버 다운 감지, ObritDbDown(obrit_db_up==0, 2m) -> DB 다운 감지
├── alertmanager.yml                         # group_by/title/text 템플릿, slack_api_url_file로 Discord 주입
├── grafana/
│   ├── provisioning/datasources/            # Prometheus를 Grafana datasource로 자동 등록
│   ├── provisioning/dashboards/             # /var/lib/grafana/dashboards 자동 로드
│   └── dashboards/spring-boot.json          # Grafana 공식 대시보드 ID 4701
└── 
```

## 흐름

```
Spring App (/actuator/prometheus)
        ↑ scrape (app:8080)
Prometheus  ──룰 평가──→  Alertmanager  ──slack-호환 webhook──→  Discord
        ↑ query
     Grafana
```

- 모든 컨테이너는 루트 `docker-compose.yml`이 만드는 `orbit-network`에 참여한다. Prometheus는 `app:8080`을 서비스 이름으로 직접 스크랩 (호스트 포트 노출 불필요).
- Alertmanager는 yaml 내 `${VAR}` 환경변수 치환을 지원하지 않아 Discord webhook은 `slack_api_url_file` + compose `secrets:` 패턴으로 주입한다.
- Discord의 Slack-호환 엔드포인트는 `title_link` 빈 값 / 키워드 색상(`danger`/`good`)에 400을 반환하므로 명시적 URL과 hex 색상을 사용한다.

## 알람 정책

| Alert | Expr | for | 의도 |
|-------|------|-----|------|
| `ObritDown` | `up{job="obrit"} == 0` | 5m | 앱 프로세스 다운. 배포/재시작(<5m) 흡수 |
| `ObritDbDown` | `obrit_db_up == 0` | 2m | 앱은 살아있는데 DB 끊긴 부분 장애 |

## 시크릿

- 로컬: `.env`와 `secrets/discord_webhook_url`을 직접 작성 (`.env.example` 참고).
- 운영: `.github/workflows/ci-cd.yml`이 GitHub Secrets(`MONITORING_ENV_FILE`, `MONITORING_DISCORD_WEBHOOK_URL`)을 EC2에 자동 주입.
- Discord webhook URL은 끝에 `/slack`을 붙여야 Slack 호환 포맷을 받는다.
