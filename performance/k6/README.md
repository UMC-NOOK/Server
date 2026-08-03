# NOOK k6 Performance Tests

NOOK 서버의 인증 이후 API 병목을 확인하기 위한 k6 테스트 구성입니다.
로컬 또는 배포 환경을 대상으로 smoke, seed, mixed-read journey, 도메인별 API 시나리오를 실행하고, 결과는 k6 CLI 출력과 Grafana 대시보드에서 확인합니다.

## 구성

```text
performance/k6/
├── config/      # k6 options, thresholds, arrival-rate profile
├── env/         # 환경별 실행 preset. 실제 *.env 파일은 Git에서 제외
├── lib/         # 인증, HTTP, seed, summary 공통 로직
├── reports/     # 로컬 JSON 결과. *.json 파일은 Git에서 제외
├── scenarios/   # k6 실행 시나리오
├── scripts/     # Docker Compose 기반 실행 스크립트
└── state/       # 마지막 seed run id. 상태 파일은 Git에서 제외
```

## 테스트 종류

| 명령 | 목적 |
|---|---|
| `make k6-smoke` | 서버 연결, 인증, 기본 API 응답 확인. k6는 실행 결과를 Prometheus remote write로 전송 |
| `make k6-seed` | mixed-read 테스트에 사용할 테스트 유저와 책/기록/집중 데이터 생성 |
| `make k6-mixed-read JOURNEYS_PER_SECOND=1` | 주요 조회 API 묶음을 초당 1회 시작하여 사용자 journey의 p95, p99, error rate 확인 |
| `make k6-global-search` | GLOBAL 도서 검색과 Aladin 외부 API 연동 구간 확인 |
| `make k6-scenario SCENARIO=<이름>` | 개별 도메인 시나리오 실행 |

`k6-scenario`에서 사용할 수 있는 이름은 다음과 같습니다.

| 시나리오 | 목적 |
|---|---|
| `books-user` | 사용자 직접 등록 도서 생성, 상세 조회, 수정 |
| `books-search-library` | 내 서재 검색, 검색 기록, 서재 검색 홈 |
| `books-search-global` | GLOBAL 검색과 Aladin 외부 API 연동 |
| `onboarding` | 온보딩 완료, 상태 조회, 목표 수정 |
| `timeline-core` | 책 등록 타임라인 조회와 상세 |
| `timeline-producers` | 상태 변경, 기록, 집중 세션 기반 타임라인 생성 |

## 최초 준비

백엔드 서버를 먼저 실행합니다. 로컬 서버는 기본적으로 `http://localhost:8080`에서 실행된다고 가정합니다. k6는 Docker 컨테이너 안에서 실행되므로 기본 `BASE_URL`은 다음 값을 사용합니다.

```text
http://host.docker.internal:8080
```

모니터링 스택을 실행합니다.

```bash
make k6-up
```

Grafana는 기본적으로 아래 주소에서 확인합니다.

```text
http://localhost:3001
```

Grafana 계정은 `performance/k6/env/monitoring.env`에서 설정합니다. 예시 파일의 비밀번호는 로컬 기본값이므로 실제 공유 환경에서는 반드시 변경합니다.

환경별 env 파일이 없으면 `make` 실행 시 `performance/k6/env/*.env.example`을 복사해 자동 생성합니다. 실제 `*.env` 파일은 Git에 커밋하지 않습니다.

## 로컬 실행

처음에는 smoke 테스트로 연결을 확인합니다.

```bash
make k6-smoke
```

mixed-read 테스트용 데이터를 한 번 생성합니다.

```bash
make k6-seed
```

seed가 성공하면 마지막 seed run id가 `performance/k6/state/last-seed-local`에 저장됩니다. DB와 state 파일이 유지되는 동안에는 seed를 매번 다시 실행하지 않아도 됩니다.

혼합 조회 journey를 실행합니다.

```bash
make k6-mixed-read JOURNEYS_PER_SECOND=1
```

부하를 높이고 싶으면 초당 journey 시작 횟수를 양의 정수로 올립니다.

```bash
make k6-mixed-read JOURNEYS_PER_SECOND=5
make k6-mixed-read JOURNEYS_PER_SECOND=10
```

`JOURNEYS_PER_SECOND`는 HTTP 요청 RPS가 아니라 mixed-read iteration 도착률입니다. target이 모두 존재하면 한 journey는 다음과 같이 최대 18개의 HTTP 요청을 순차 실행합니다.

| 그룹 | 최대 요청 수 |
|---|---:|
| book/search | 3 |
| library | 6 |
| records | 4 |
| focus | 2 |
| timeline | 3 |

따라서 `JOURNEYS_PER_SECOND=1`은 최대 약 18 HTTP RPS, `JOURNEYS_PER_SECOND=5`는 최대 약 90 HTTP RPS에 해당합니다. 선택 가능한 record나 timeline detail target이 없으면 실제 요청 수는 이보다 적습니다. runner는 실행 전에 journey rate, journey당 최대 요청 수, 예상 최대 HTTP RPS를 출력합니다.

API별 병목 테스트에서는 한 iteration이 목표 API 한 번만 호출하도록 구성하고, 이때만 `TARGET_RPS`를 실제 HTTP 요청 RPS로 사용합니다. mixed journey와 API별 RPS 결과를 같은 의미로 비교하지 않습니다.

mixed-read는 전체 요청의 p95·실패율뿐 아니라 위 18개 `name` 태그 각각에 같은 기준을 독립 적용합니다. 따라서 특정 API 하나만 느리거나 실패해도 다른 빠른 API 표본에 가려지지 않습니다. 현재 `P95_THRESHOLD_MS=1000`, `FAILED_RATE_THRESHOLD=0.01`은 staging baseline 전까지 사용하는 임시 engineering gate이며 실제 SLO는 staging 측정 후 조정합니다.

arrival-rate 실행에서 필요한 VU를 확보하지 못해 시작하지 못한 journey는 `dropped_iterations`로 집계합니다. 기본값 `MAX_DROPPED_ITERATIONS=0`은 하나라도 누락되면 테스트를 실패시키며, 탐색 실행에서만 명시적으로 허용치를 올립니다.

```bash
MAX_DROPPED_ITERATIONS=5 make k6-mixed-read JOURNEYS_PER_SECOND=10
```

GLOBAL 도서 검색과 Aladin 연동 구간을 확인합니다.

```bash
make k6-global-search
```

특정 도메인 시나리오를 실행할 수도 있습니다.

```bash
make k6-scenario SCENARIO=books-user
make k6-scenario SCENARIO=timeline-core
```

실행 명령만 확인하고 싶으면 dry-run을 사용합니다.

```bash
make k6-dry-mixed-read JOURNEYS_PER_SECOND=1
```

mixed-read의 18개 request tag별 threshold와 dropped 기준이 options에 포함됐는지는 다음 명령으로 검사합니다. Docker와 `jq`가 필요합니다.

```bash
make k6-test-options
```

## 배포 환경 실행

배포 환경별 설정은 `performance/k6/env/<환경>.env`에 둡니다.

```bash
cp performance/k6/env/staging.env.example performance/k6/env/staging.env
```

`staging.env`의 `BASE_URL`, 필요하면 `MANAGEMENT_BASE_URL`, 테스트 계정, threshold 값을 환경에 맞게 수정합니다.

```bash
CONFIRM_PROD_LOADTEST=yes make k6-smoke ENV=staging
CONFIRM_PROD_LOADTEST=yes make k6-seed ENV=staging
CONFIRM_PROD_LOADTEST=yes make k6-mixed-read ENV=staging JOURNEYS_PER_SECOND=5
CONFIRM_PROD_LOADTEST=yes make k6-global-search ENV=staging
```

운영 환경은 실수 방지를 위해 확인 변수가 필요합니다.

```bash
CONFIRM_PROD_LOADTEST=yes make k6-smoke ENV=prod
CONFIRM_PROD_LOADTEST=yes make k6-mixed-read ENV=prod JOURNEYS_PER_SECOND=1
```

`run-k6.sh`를 직접 실행해도 비로컬 URL은 동일한 확인 변수를 요구합니다.
`docker compose run k6`로 직접 실행해도 k6 컨테이너 entrypoint에서 같은 확인 절차를 수행합니다.
`CONFIRM_PROD_LOADTEST`는 이름에 `PROD`가 남아 있지만, 실제 의미는 staging과 prod를 포함한 비로컬 대상에 부하테스트를 의도적으로 실행한다는 확인입니다.
확인 없이 허용되는 host는 `localhost`, `127.0.0.1`, `[::1]`, `host.docker.internal`뿐입니다. `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16` 같은 사설망 주소도 비로컬 대상으로 취급합니다.

## GLOBAL/Aladin 검색

`books-search-global`은 내부 DB 조회만 보는 테스트가 아니라 외부 Aladin API 호출을 포함합니다. 따라서 고부하로 한계를 찾기보다는 낮은 RPS에서 외부 API 연동 지연, 실패율, timeout 여부를 분리해 확인하는 용도로 사용합니다.

검색어는 env 파일 또는 실행 시점에 지정합니다.

```bash
K6_GLOBAL_SEARCH_KEYWORDS=자바,클린코드,해리포터 make k6-global-search
```

기본 threshold는 내부 mixed-read 테스트보다 느슨합니다.

| 변수 | 기본값 | 의미 |
|---|---:|---|
| `TARGET_RPS` | 1 | GLOBAL 검색 요청 RPS |
| `DURATION` | 1m | 테스트 지속 시간 |
| `K6_GLOBAL_USER_POOL_SIZE` | 20 | 병렬 실행 시 사용할 테스트 유저 풀 크기 |
| `K6_GLOBAL_P95_THRESHOLD_MS` | 5000 | 외부 API 포함 p95 기준 |
| `K6_GLOBAL_FAILED_RATE_THRESHOLD` | 0.05 | 외부 API 포함 실패율 기준 |

GLOBAL 검색은 첫 페이지 검색 시 검색 기록을 저장합니다. 병렬 실행에서 한 유저가 같은 키워드를 반복 저장하면 외부 API 측정과 검색 기록 저장 충돌이 섞일 수 있으므로, 시나리오는 setup 단계에서 테스트 유저 풀을 만들고 VU별로 다른 유저를 사용합니다.

단일 `TOKEN` 또는 `K6_ACCESS_TOKEN`을 사용하는 경우 `K6_GLOBAL_USER_POOL_SIZE=1`이어야 합니다. 2명 이상의 사용자 풀은 사용자별 credential 구성이 필요하며 현재는 지원하지 않습니다.

## Seed 규모 조절

seed 데이터 양은 env 파일 또는 실행 시 환경변수로 조절할 수 있습니다.

| 변수 | 의미 |
|---|---|
| `SEED_BOOKS` | 생성할 책/서재 데이터 수 |
| `SEED_RECORDS_PER_BOOK` | 책 1권당 생성할 기록 수 |
| `SEED_FOCUS_SESSIONS` | 생성할 집중 세션 수 |
| `SEED_BOOK_TITLE_PREFIX` | seed 책 제목 prefix |
| `SEED_RECORD_PREFIX` | seed 기록 내용 prefix |

기본값은 다음과 같습니다.

| 환경 | 책 | 기록 | 집중 세션 |
|---|---:|---:|---:|
| local | 30 | 90 | 10 |
| staging | 300 | 900 | 100 |
| prod | 30 | 90 | 10 |

실행 시 한 번만 덮어쓸 수도 있습니다.

```bash
SEED_BOOKS=100 \
SEED_RECORDS_PER_BOOK=10 \
SEED_FOCUS_SESSIONS=50 \
make k6-seed
```

## 인증

현재 기본 인증 방식은 dev login입니다. 이 테스트의 목적은 OAuth 로그인 플로우가 아니라 인증 이후 API 병목 확인이므로, 서버가 발급한 JWT를 사용해 주요 API를 호출합니다.

환경에 따라 준비된 토큰을 직접 사용할 수도 있습니다.

```bash
K6_ACCESS_TOKEN=... make k6-mixed-read ENV=staging JOURNEYS_PER_SECOND=5
```

나중에 dev login을 제거하면 `performance/k6/lib/auth.js`의 인증 획득 로직을 refresh token 또는 테스트 OAuth 계정 기반으로 전환합니다.

## 시나리오 추가 방법

새 k6 시나리오는 `performance/k6/scenarios/`에 추가합니다. 공통 인증, HTTP, summary 로직은 `performance/k6/lib/`를 재사용하고, options는 성격에 맞게 `performance/k6/config/profiles.js`에서 가져옵니다.

공식 실행 경로에 포함하려면 다음 위치를 함께 수정합니다.

| 위치 | 수정 내용 |
|---|---|
| `performance/k6/scripts/run-k6.sh` | scenario 이름, script 경로, 기본 RUN_ID/report/threshold 등록 |
| `Makefile` | 자주 쓰는 시나리오라면 별도 target 추가. 그 외에는 `k6-scenario`로 실행 |
| `docker-compose.monitoring.yml` | 새 환경변수가 필요하면 k6 service environment에 추가 |
| `performance/k6/env/*.env.example` | 팀원이 공유할 수 있는 새 설정값 예시 추가 |
| `performance/k6/README.md` | 목적, 실행 명령, 결과 해석 기준 추가 |

## 결과 확인

k6 CLI 출력에서 threshold 결과를 바로 확인할 수 있습니다.

| 지표 | 의미 |
|---|---|
| `http_req_failed` | HTTP 실패율 |
| `http_req_duration` | 응답 시간. p95, p99 확인 |
| `checks` | 상태 코드, API 응답 형식, result 존재 여부 등 검증 |
| `dropped_iterations` | 목표 API RPS 또는 mixed journey 도착률을 따라가지 못한 iteration 수 |

threshold를 만족하지 못하면 k6가 non-zero exit code로 종료되고 `make`도 실패합니다. 이 경우 실행 자체가 멈춘 것이 아니라, 테스트 대상 API가 설정한 기준을 통과하지 못한 것으로 보고 실패한 API와 check 항목을 확인합니다.

로컬 JSON 결과는 `performance/k6/reports/` 아래에 생성됩니다. 이 파일들은 Git에 커밋하지 않습니다.

Grafana에서는 다음 항목을 중심으로 확인합니다.

| 패널 | 확인 내용 |
|---|---|
| k6 API Latency by Name | API별 p95, p99, 평균 응답 시간 |
| k6 Failure Rate and Checks | 실패율과 check 실패 |
| k6 Iterations and Dropped Iterations | 목표 API RPS 또는 journey 도착률 유지 여부 |
| Spring HTTP Latency | 서버가 관측한 API 응답 시간 |
| Hikari Connections | DB connection 사용량 |
| Hikari Connection Timeouts | DB connection timeout 발생 여부 |

## 참고 사항

threshold 실패는 k6 프로세스를 실패 상태로 종료시킵니다. 실패한 API와 check 항목은 CLI 출력, JSON report, Grafana 패널에서 확인합니다.

seed를 반복 실행하면 기존 데이터를 지우지 않고 새로운 테스트 유저와 데이터 세트를 계속 생성합니다. DB가 유지되고 마지막 seed state가 남아 있다면 mixed-read 테스트는 seed를 다시 실행하지 않고 반복할 수 있습니다.
