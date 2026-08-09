# NOOK k6 테스트

NOOK 서버의 인증 이후 API 흐름을 검증하고 조회 병목 후보를 찾기 위한 k6 구성입니다. 로컬 결과는 시나리오와 관측 체계를 검증하는 자료이며, 운영 용량이나 SLO 근거로 사용하지 않습니다. 용량 판단은 운영과 사양 차이가 문서화된 staging에서 같은 commit과 데이터 조건으로 반복 측정한 뒤 수행합니다.

## 지원 범위

| 실행 | 성격 | 용도 |
|---|---|---|
| `make k6-smoke` | smoke | 연결, 인증, Actuator와 핵심 API 확인 |
| `make k6-seed` | 로컬 준비 작업 | namespace별 고정 규모 데이터를 생성하거나 기존 데이터 검증 |
| `make k6-seed-cleanup` | 로컬 정리 작업 | namespace에 속한 테스트 사용자와 데이터를 함께 삭제 |
| `make k6-mixed-read JOURNEYS_PER_SECOND=1` | arrival-rate | 주요 조회 흐름의 지연, 실패율, dropped iteration 확인 |
| `make k6-global-search` | 낮은 arrival-rate | GLOBAL 검색과 Aladin 외부 연동 확인 |
| `make k6-scenario SCENARIO=<이름>` | 기능 journey | 도메인 흐름을 1 VU, 1 iteration으로 검증 |

도메인 journey는 `books-user`, `books-search-library`, `onboarding`, `timeline-core`, `timeline-producers`입니다. 이들은 여러 API를 연결한 기능·smoke 검증이며 API별 최대 RPS나 안정 처리량을 측정하는 baseline이 아닙니다.

`performance/k6/`는 다음 역할만 나눕니다.

```text
config/      k6 options와 threshold
env/         환경별 예시 설정; 실제 *.env는 Git 제외
lib/         인증, HTTP, check, seed, summary
reports/     로컬 JSON 결과; Git 제외
scenarios/   실행 시나리오
scripts/     러너, 컨테이너 안전 정책, 검증 스크립트
state/       seed manifest와 마지막 namespace 포인터; Git 제외
```

## 준비와 로컬 실행

백엔드를 먼저 실행합니다. k6는 컨테이너에서 실행되므로 기본 API 주소는 `http://host.docker.internal:8080`입니다.

```bash
make k6-up
make k6-smoke
make k6-seed
make k6-mixed-read JOURNEYS_PER_SECOND=1
```

Grafana는 기본적으로 `http://localhost:3001`에서 확인합니다. 계정 설정은 `performance/k6/env/monitoring.env`에 두며 실제 env 파일은 커밋하지 않습니다. 파일이 없으면 Make target이 대응하는 `.env.example`을 복사합니다.

seed는 `SEED_NAMESPACE`별 전용 DEV 사용자를 만들고, 성공한 뒤에만 `performance/k6/state/seeds/`에 manifest를 저장합니다. 같은 profile과 namespace로 다시 실행하면 데이터를 추가하지 않고 실제 책·기록·집중 개수가 manifest와 정확히 같은지 검증합니다. mixed-read는 명시한 namespace 또는 마지막으로 성공한 namespace를 재사용합니다.

profile 규모는 재현성을 위해 고정되어 있습니다.

| profile | 책 | 책당 기록 | 집중 세션 |
|---|---:|---:|---:|
| `light` | 5 | 2 | 2 |
| `normal` | 30 | 3 | 10 |
| `heavy` | 300 | 10 | 100 |

작은 데이터로 전체 생명주기를 확인하는 예시는 다음과 같습니다.

```bash
SEED_PROFILE=light SEED_NAMESPACE=issue-305 make k6-seed
SEED_PROFILE=light SEED_NAMESPACE=issue-305 make k6-seed
SEED_NAMESPACE=issue-305 make k6-mixed-read JOURNEYS_PER_SECOND=1
SEED_NAMESPACE=issue-305 make k6-seed-cleanup
```

생성 중 실패해 manifest가 없는 경우에도 같은 `SEED_NAMESPACE`로 cleanup을 실행해 부분 생성된 전용 사용자를 정리할 수 있습니다. seed 생성·재사용·정리는 토큰 주입을 받지 않으며 로컬 대상에서만 허용됩니다.

도메인 journey와 GLOBAL 검색은 다음처럼 실행합니다.

```bash
make k6-scenario SCENARIO=books-user
make k6-scenario SCENARIO=timeline-core
make k6-global-search
```

Docker 명령만 확인하려면 `make k6-dry-mixed-read JOURNEYS_PER_SECOND=1`을 사용합니다.

## mixed-read 해석

`JOURNEYS_PER_SECOND`는 HTTP RPS가 아니라 journey 시작률입니다. 모든 detail 대상이 있으면 한 journey가 최대 18개 요청을 순차 실행합니다.

| 그룹 | 최대 요청 수 |
|---|---:|
| book/search | 3 |
| library | 6 |
| records | 4 |
| focus | 2 |
| timeline | 3 |

따라서 1 journey/s는 최대 약 18 HTTP RPS, 5 journey/s는 최대 약 90 HTTP RPS입니다. 선택할 record나 timeline detail이 없으면 실제 요청 수는 더 적습니다. 러너가 실행 전에 journey rate와 예상 최대 HTTP RPS를 출력합니다.

```bash
make k6-mixed-read JOURNEYS_PER_SECOND=5
make k6-mixed-read JOURNEYS_PER_SECOND=10
```

mixed-read는 전체 지표와 함께 18개 요청의 `name` 태그별 p95와 실패율을 각각 판정합니다. 기본 `P95_THRESHOLD_MS=1000`, `FAILED_RATE_THRESHOLD=0.01`은 staging baseline 전의 임시 engineering gate입니다.

필요한 VU를 확보하지 못해 시작되지 않은 journey는 `dropped_iterations`입니다. 기본 `MAX_DROPPED_ITERATIONS=0`은 하나라도 누락되면 실패합니다. 탐색 실행에서만 명시적으로 허용치를 바꿉니다.

```bash
MAX_DROPPED_ITERATIONS=5 make k6-mixed-read JOURNEYS_PER_SECOND=10
```

## 원격 환경 안전 정책

staging 등 모든 비로컬 HTTP 대상은 정확히 `CONFIRM_PROD_LOADTEST=yes`를 전달해야 합니다. `staging.env`와 `prod.env`에는 각각 승인된 `BASE_URL`, `MANAGEMENT_BASE_URL` 정규식 허용 목록을 설정해야 하며, 둘 중 하나라도 목록과 맞지 않으면 실행하지 않습니다.

```bash
cp performance/k6/env/staging.env.example performance/k6/env/staging.env
CONFIRM_PROD_LOADTEST=yes make k6-smoke ENV=staging
CONFIRM_PROD_LOADTEST=yes make k6-mixed-read ENV=staging JOURNEYS_PER_SECOND=5
CONFIRM_PROD_LOADTEST=yes make k6-global-search ENV=staging
```

seed는 로컬 환경과 로컬 URL에서만 허용됩니다. staging 데이터는 별도로 준비한 합성 데이터와 전용 계정을 사용합니다. production은 확인 변수가 있어도 smoke만 허용합니다.

```bash
CONFIRM_PROD_LOADTEST=yes make k6-smoke ENV=prod
```

로컬에서는 `localhost`, `127.0.0.1`, `[::1]`, `host.docker.internal`만 허용합니다. 사설망 주소도 비로컬로 취급합니다. 러너를 우회해 컨테이너를 직접 실행해도 entrypoint가 API와 management URL 모두에 같은 정책을 검사합니다.

## GLOBAL/Aladin 검색

`books-search-global`에는 Aladin 외부 API 지연과 검색 기록 쓰기가 포함됩니다. 내부 API의 최대 처리량으로 해석하지 않고 낮은 RPS에서 외부 연동 지연, 실패율, timeout을 확인합니다.

| 변수 | 기본값 | 의미 |
|---|---:|---|
| `TARGET_RPS` | 1 | GLOBAL 검색 요청 RPS |
| `DURATION` | 1m | 실행 시간 |
| `K6_GLOBAL_USER_POOL_SIZE` | 20 | VU별 테스트 사용자 풀 |
| `K6_GLOBAL_P95_THRESHOLD_MS` | 5000 | 외부 API 포함 p95 기준 |
| `K6_GLOBAL_FAILED_RATE_THRESHOLD` | 0.05 | 외부 API 포함 실패율 기준 |

```bash
K6_GLOBAL_SEARCH_KEYWORDS=자바,클린코드,해리포터 make k6-global-search
```

단일 `TOKEN` 또는 `K6_ACCESS_TOKEN`을 사용할 때는 사용자별 자격 증명이 없으므로 `K6_GLOBAL_USER_POOL_SIZE=1`이어야 합니다.

## 인증

로컬 기본값은 dev login입니다. 테스트 대상은 OAuth 로그인 과정이 아니라 인증 이후 API이므로 준비된 토큰도 사용할 수 있습니다.

```bash
CONFIRM_PROD_LOADTEST=yes \
K6_ACCESS_TOKEN=... \
make k6-mixed-read ENV=staging JOURNEYS_PER_SECOND=5
```

토큰은 dry-run 출력과 summary에서 마스킹되며 env 파일이나 JSON report를 커밋하지 않습니다.

## 검증과 시나리오 추가

Docker와 `jq`가 준비된 환경에서 전체 정적·실행 경계 검증을 수행합니다.

```bash
make k6-test
```

이 명령은 셸 구문, 러너의 시나리오 매핑과 원격 안전 정책, 11개 시나리오의 k6 options 파싱, seed profile·manifest 생명주기, 결과 메타데이터, mixed-read 18개 요청별 threshold, Compose 구성을 검사합니다.

시나리오를 추가할 때는 다음만 수정합니다.

1. `scenarios/`에 스크립트를 추가하고 `config/`와 `lib/`의 기존 로직을 재사용합니다.
2. `run-k6.sh`의 시나리오 매핑과 기본 실행값을 추가합니다.
3. `verify-runner.sh`의 CLI 매핑과 `verify-options.sh`의 지원 파일 목록을 갱신합니다.
4. 자주 쓰는 진입점만 Make target으로 추가하고, 공유할 기본값만 env example에 둡니다.

러너가 시나리오 환경 변수를 컨테이너에 전달하므로 Compose에 시나리오별 변수를 반복 등록하지 않습니다.

## 결과 확인

| 지표 | 의미 |
|---|---|
| `http_req_failed` | HTTP 실패율 |
| `http_req_duration` | 응답 시간과 p95, p99 |
| `checks` | 상태 코드와 API 응답 형식 검증 |
| `dropped_iterations` | 목표 도착률을 따라가지 못한 iteration 수 |

threshold를 넘으면 k6와 Make가 non-zero로 종료됩니다. JSON 결과에는 실행 ID, 테스트 이름, 환경, 대상 URL, 코드 commit, seed commit/profile/namespace가 함께 기록됩니다. 파일은 `performance/k6/reports/`에 저장되며 Git에서 제외됩니다.

Grafana에서는 k6 API별 latency·failure·dropped iteration과 같은 시간대의 Spring HTTP latency, Hikari connection 사용량과 timeout을 함께 확인합니다. 로컬 측정값은 코드 변경 전후의 상대 비교와 시나리오 검증에만 사용합니다.
