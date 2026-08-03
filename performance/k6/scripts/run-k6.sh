#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage:
  performance/k6/scripts/run-k6.sh smoke
  performance/k6/scripts/run-k6.sh seed
  performance/k6/scripts/run-k6.sh mixed-read [jps<positive integer>]
  performance/k6/scripts/run-k6.sh books-user
  performance/k6/scripts/run-k6.sh books-search-library
  performance/k6/scripts/run-k6.sh books-search-global
  performance/k6/scripts/run-k6.sh onboarding
  performance/k6/scripts/run-k6.sh timeline-core
  performance/k6/scripts/run-k6.sh timeline-producers

Common environment overrides:
  K6_ENV                      Environment preset name. Default: local
  K6_ENV_FILE                 Environment preset file. Default: performance/k6/env/<K6_ENV>.env
  BASE_URL                    Target API URL. Default: http://host.docker.internal:8080
  MANAGEMENT_BASE_URL          Optional Actuator URL for smoke health checks.
  RUN_PREFIX                  Prefix for generated RUN_ID. Default: <K6_ENV>
  RUN_ID                      Explicit run id. Default is generated per scenario.
  ENV_FILE                    Grafana env file for local compose. Default: performance/k6/env/monitoring.env
  GRAFANA_PORT                Local Grafana port. Default: 3001
  K6_ACCESS_TOKEN             Prepared access token for deployed environments.
  K6_REFRESH_TOKEN            Prepared refresh token for later OAuth/refresh-token auth support.
  SEED_RUN_ID                 Seed run id used to derive local read-test email.
  K6_USER_EMAIL               Explicit test user email.
  K6_USER_NICKNAME            Explicit test user nickname.
  JOURNEYS_PER_SECOND         Mixed-read journey arrivals per second. Default: 1.
  SEED_BOOKS                  Number of seed books. Default: 30 from run-k6, or env preset value.
  SEED_RECORDS_PER_BOOK       Number of records per seed book. Default: 3 from run-k6, or env preset value.
  SEED_FOCUS_SESSIONS         Number of focus sessions. Default: 10 from run-k6, or env preset value.
  K6_GLOBAL_SEARCH_KEYWORDS    Comma-separated keywords for GLOBAL/Aladin search.
  K6_GLOBAL_P95_THRESHOLD_MS   GLOBAL/Aladin p95 threshold. Default: 5000.
  K6_GLOBAL_FAILED_RATE_THRESHOLD
                              GLOBAL/Aladin failure-rate threshold. Default: 0.05.
  K6_GLOBAL_USER_POOL_SIZE     Number of prepared users for GLOBAL/Aladin concurrent runs.
  K6_ENABLE_EXTERNAL_API=yes   Required by GLOBAL/Aladin search. Set automatically by books-search-global alias.
  K6_DRY_RUN=1                Print resolved docker compose command without running it.
  CONFIRM_PROD_LOADTEST=yes    Required for any non-local target, including staging and prod.

Examples:
  performance/k6/scripts/run-k6.sh smoke
  performance/k6/scripts/run-k6.sh seed
  SEED_RUN_ID=local-seed-20260729-0038 performance/k6/scripts/run-k6.sh mixed-read jps1
  performance/k6/scripts/run-k6.sh books-search-global
  CONFIRM_PROD_LOADTEST=yes K6_ENV=staging performance/k6/scripts/run-k6.sh mixed-read jps5
USAGE
}

die() {
  echo "error: $*" >&2
  exit 2
}

is_yes() {
  [[ "${1:-}" =~ ^(1|true|yes|y|on)$ ]]
}

normalize_base_url() {
  printf '%s' "${1:-}" | tr '[:upper:]' '[:lower:]'
}

is_local_base_url() {
  local base_url
  base_url="$(normalize_base_url "$1")"

  [[ "$base_url" =~ ^https?://(localhost|127\.0\.0\.1|host\.docker\.internal)(:[0-9]+)?(/|$) ]] && return 0
  [[ "$base_url" =~ ^https?://\[::1\](:[0-9]+)?(/|$) ]]
}

is_remote_http_url() {
  local base_url
  base_url="$(normalize_base_url "$1")"

  [[ "$base_url" =~ ^https?:// ]]
}

requires_confirmation() {
  local normalized_base_url
  normalized_base_url="$(normalize_base_url "$BASE_URL")"

  if [[ "$K6_ENV" == "prod" || "${RUN_PREFIX:-}" == "prod" ]] || is_yes "${K6_REQUIRE_CONFIRM:-}"; then
    return 0
  fi

  if is_remote_http_url "$BASE_URL" && ! is_local_base_url "$BASE_URL"; then
    return 0
  fi

  if [[ -n "${K6_PROD_BASE_URL_PATTERN:-}" && "$BASE_URL" =~ ${K6_PROD_BASE_URL_PATTERN} ]]; then
    return 0
  fi

  [[ "$normalized_base_url" =~ (^https?://api\.|[./-]prod[./:-]|production) ]]
}

assert_loadtest_allowed() {
  if requires_confirmation && [[ "${CONFIRM_PROD_LOADTEST:-}" != "yes" ]]; then
    die "non-local or production-like load test requires CONFIRM_PROD_LOADTEST=yes"
  fi
  return 0
}

load_env_file() {
  local file="$1"
  [[ -f "$file" ]] || return 0

  while IFS= read -r line || [[ -n "$line" ]]; do
    line="${line%$'\r'}"
    line="${line#"${line%%[![:space:]]*}"}"
    line="${line%"${line##*[![:space:]]}"}"

    [[ -z "$line" || "$line" == \#* ]] && continue
    [[ "$line" == *=* ]] || continue

    local key="${line%%=*}"
    local value="${line#*=}"

    key="${key#"${key%%[![:space:]]*}"}"
    key="${key%"${key##*[![:space:]]}"}"
    value="${value#"${value%%[![:space:]]*}"}"
    value="${value%"${value##*[![:space:]]}"}"

    [[ "$key" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]] || continue

    if [[ "$value" == \"*\" && "$value" == *\" ]]; then
      value="${value:1:${#value}-2}"
    elif [[ "$value" == \'*\' && "$value" == *\' ]]; then
      value="${value:1:${#value}-2}"
    fi

    if [[ -z "${!key+x}" ]]; then
      export "$key=$value"
    fi
  done < "$file"
}

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
server_dir="$(cd "$script_dir/../../.." && pwd)"
cd "$server_dir"

scenario="${1:-}"
profile="${2:-jps1}"
if [[ "$profile" =~ ^[0-9]+$ ]]; then
  profile="jps${profile}"
fi

if [[ -z "$scenario" || "$scenario" == "-h" || "$scenario" == "--help" ]]; then
  usage
  exit 0
fi

K6_ENV="${K6_ENV:-${ENV:-local}}"
K6_ENV_FILE="${K6_ENV_FILE:-performance/k6/env/${K6_ENV}.env}"
K6_STATE_DIR="${K6_STATE_DIR:-performance/k6/state}"
K6_SEED_STATE_FILE="${K6_SEED_STATE_FILE:-${K6_STATE_DIR}/last-seed-${K6_ENV}}"

load_env_file "$K6_ENV_FILE"

timestamp="$(date +%Y%m%d-%H%M%S)-$$"
run_prefix="${RUN_PREFIX:-$K6_ENV}"

ENV_FILE="${ENV_FILE:-performance/k6/env/monitoring.env}"
GRAFANA_PORT="${GRAFANA_PORT:-3001}"
K6_DOCKER_USER="${K6_DOCKER_USER:-$(id -u):$(id -g)}"
BASE_URL="${BASE_URL:-http://host.docker.internal:8080}"
MANAGEMENT_BASE_URL="${MANAGEMENT_BASE_URL:-}"
P95_THRESHOLD_MS="${P95_THRESHOLD_MS:-}"
FAILED_RATE_THRESHOLD="${FAILED_RATE_THRESHOLD:-}"

assert_loadtest_allowed

configure_internal_scenario() {
  local script="$1"
  local report="$2"

  K6_SCRIPT="${K6_SCRIPT:-$script}"
  K6_REPORT_NAME="${K6_REPORT_NAME:-$report}"
  RUN_ID="${RUN_ID:-${run_prefix}-${report}-${timestamp}}"
  VUS="${VUS:-1}"
  ITERATIONS="${ITERATIONS:-1}"
  MAX_DURATION="${MAX_DURATION:-2m}"
  P95_THRESHOLD_MS="${P95_THRESHOLD_MS:-800}"
  FAILED_RATE_THRESHOLD="${FAILED_RATE_THRESHOLD:-0.01}"
}

case "$scenario" in
  smoke)
    K6_SCRIPT="${K6_SCRIPT:-performance/k6/scenarios/smoke.js}"
    K6_REPORT_NAME="${K6_REPORT_NAME:-smoke}"
    RUN_ID="${RUN_ID:-${run_prefix}-smoke-${timestamp}}"
    ;;
  seed | prepare-seed)
    K6_SCRIPT="${K6_SCRIPT:-performance/k6/scenarios/prepare-seed.js}"
    K6_REPORT_NAME="${K6_REPORT_NAME:-seed}"
    RUN_ID="${RUN_ID:-${run_prefix}-seed-${timestamp}}"
    SEED_BOOKS="${SEED_BOOKS:-30}"
    SEED_RECORDS_PER_BOOK="${SEED_RECORDS_PER_BOOK:-3}"
    SEED_FOCUS_SESSIONS="${SEED_FOCUS_SESSIONS:-10}"
    ;;
  mixed-read)
    K6_SCRIPT="${K6_SCRIPT:-performance/k6/scenarios/mixed-read-journey.js}"
    K6_REPORT_NAME="${K6_REPORT_NAME:-mixed-read-journey}"

    [[ "$profile" =~ ^jps([1-9][0-9]*)$ ]] || die "unknown mixed-read profile '$profile' (expected jps<positive integer>)"
    JOURNEYS_PER_SECOND="${JOURNEYS_PER_SECOND:-${BASH_REMATCH[1]}}"
    [[ "$JOURNEYS_PER_SECOND" =~ ^[1-9][0-9]*$ ]] || die "JOURNEYS_PER_SECOND must be a positive integer"

    if (( JOURNEYS_PER_SECOND < 5 )); then
      DURATION="${DURATION:-1m}"
      PRE_ALLOCATED_VUS="${PRE_ALLOCATED_VUS:-5}"
      MAX_VUS="${MAX_VUS:-20}"
    elif (( JOURNEYS_PER_SECOND < 10 )); then
      DURATION="${DURATION:-5m}"
      PRE_ALLOCATED_VUS="${PRE_ALLOCATED_VUS:-20}"
      MAX_VUS="${MAX_VUS:-80}"
    else
      DURATION="${DURATION:-10m}"
      PRE_ALLOCATED_VUS="${PRE_ALLOCATED_VUS:-40}"
      MAX_VUS="${MAX_VUS:-150}"
    fi

    RUN_ID="${RUN_ID:-${run_prefix}-mixed-read-jps${JOURNEYS_PER_SECOND}-${timestamp}}"

    P95_THRESHOLD_MS="${P95_THRESHOLD_MS:-1000}"
    FAILED_RATE_THRESHOLD="${FAILED_RATE_THRESHOLD:-0.01}"

    if [[ -z "${K6_ACCESS_TOKEN:-}" && -z "${TOKEN:-}" && -z "${K6_USER_EMAIL:-}" ]]; then
      if [[ -z "${SEED_RUN_ID:-}" && -f "$K6_SEED_STATE_FILE" ]]; then
        SEED_RUN_ID="$(<"$K6_SEED_STATE_FILE")"
      fi
      [[ -n "${SEED_RUN_ID:-}" ]] || die "mixed-read scenario needs K6_ACCESS_TOKEN, TOKEN, K6_USER_EMAIL, or SEED_RUN_ID. Run seed first or provide one of them."
      K6_USER_EMAIL="seed-${SEED_RUN_ID}-0-0@test.com"
      K6_USER_NICKNAME="${K6_USER_NICKNAME:-k6read}"
    fi
    ;;
  books-user)
    configure_internal_scenario "performance/k6/scenarios/books-user.js" "books-user"
    ;;
  books-search-library | search-library)
    configure_internal_scenario "performance/k6/scenarios/books-search-library.js" "books-search-library"
    ;;
  books-search-global | global-search | aladin)
    K6_SCRIPT="${K6_SCRIPT:-performance/k6/scenarios/books-search-global.js}"
    K6_REPORT_NAME="${K6_REPORT_NAME:-books-search-global}"
    RUN_ID="${RUN_ID:-${run_prefix}-books-search-global-${timestamp}}"
    K6_ENABLE_EXTERNAL_API="${K6_ENABLE_EXTERNAL_API:-yes}"
    TARGET_RPS="${TARGET_RPS:-1}"
    DURATION="${DURATION:-1m}"
    PRE_ALLOCATED_VUS="${PRE_ALLOCATED_VUS:-5}"
    MAX_VUS="${MAX_VUS:-20}"
    K6_GLOBAL_SEARCH_KEYWORDS="${K6_GLOBAL_SEARCH_KEYWORDS:-자바,클린코드,해리포터}"
    K6_GLOBAL_USER_POOL_SIZE="${K6_GLOBAL_USER_POOL_SIZE:-$MAX_VUS}"
    P95_THRESHOLD_MS="${K6_GLOBAL_P95_THRESHOLD_MS:-5000}"
    FAILED_RATE_THRESHOLD="${K6_GLOBAL_FAILED_RATE_THRESHOLD:-0.05}"
    ;;
  onboarding)
    configure_internal_scenario "performance/k6/scenarios/onboarding.js" "onboarding"
    ;;
  timeline-core)
    configure_internal_scenario "performance/k6/scenarios/timeline-core.js" "timeline-core"
    ;;
  timeline-producers)
    configure_internal_scenario "performance/k6/scenarios/timeline-producers.js" "timeline-producers"
    ;;
  *)
    die "unknown scenario '$scenario' (expected smoke, seed, mixed-read, books-user, books-search-library, books-search-global, onboarding, timeline-core, or timeline-producers)"
    ;;
esac

compose_cmd=(
  docker compose
  -f docker-compose.monitoring.yml
  --profile loadtest
  run --rm k6
)

env_cmd=(
  ENV_FILE="$ENV_FILE"
  GRAFANA_PORT="$GRAFANA_PORT"
  K6_DOCKER_USER="$K6_DOCKER_USER"
  BASE_URL="$BASE_URL"
  MANAGEMENT_BASE_URL="$MANAGEMENT_BASE_URL"
  K6_SCRIPT="$K6_SCRIPT"
  RUN_ID="$RUN_ID"
  K6_REPORT_NAME="$K6_REPORT_NAME"
  P95_THRESHOLD_MS="$P95_THRESHOLD_MS"
  FAILED_RATE_THRESHOLD="$FAILED_RATE_THRESHOLD" MAX_DROPPED_ITERATIONS="${MAX_DROPPED_ITERATIONS:-}"
  VUS="${VUS:-}"
  ITERATIONS="${ITERATIONS:-}"
  MAX_DURATION="${MAX_DURATION:-}"
  JOURNEYS_PER_SECOND="${JOURNEYS_PER_SECOND:-}"
  TARGET_RPS="${TARGET_RPS:-}"
  DURATION="${DURATION:-}"
  PRE_ALLOCATED_VUS="${PRE_ALLOCATED_VUS:-}"
  MAX_VUS="${MAX_VUS:-}"
  K6_ENV="$K6_ENV"
  RUN_PREFIX="${RUN_PREFIX:-}"
  K6_REQUIRE_CONFIRM="${K6_REQUIRE_CONFIRM:-}"
  K6_PROD_BASE_URL_PATTERN="${K6_PROD_BASE_URL_PATTERN:-}"
  CONFIRM_PROD_LOADTEST="${CONFIRM_PROD_LOADTEST:-}"
  K6_ENABLE_EXTERNAL_API="${K6_ENABLE_EXTERNAL_API:-}"
  K6_GLOBAL_SEARCH_KEYWORDS="${K6_GLOBAL_SEARCH_KEYWORDS:-}"
  K6_GLOBAL_P95_THRESHOLD_MS="${K6_GLOBAL_P95_THRESHOLD_MS:-}"
  K6_GLOBAL_FAILED_RATE_THRESHOLD="${K6_GLOBAL_FAILED_RATE_THRESHOLD:-}"
  K6_GLOBAL_USER_POOL_SIZE="${K6_GLOBAL_USER_POOL_SIZE:-}"
  SEED_BOOKS="${SEED_BOOKS:-}"
  SEED_RECORDS_PER_BOOK="${SEED_RECORDS_PER_BOOK:-}"
  SEED_FOCUS_SESSIONS="${SEED_FOCUS_SESSIONS:-}"
  K6_USER_EMAIL="${K6_USER_EMAIL:-}"
  K6_USER_NICKNAME="${K6_USER_NICKNAME:-}"
  K6_ACCESS_TOKEN="${K6_ACCESS_TOKEN:-}"
  K6_REFRESH_TOKEN="${K6_REFRESH_TOKEN:-}"
  TOKEN="${TOKEN:-}"
  K6_BOOK_ID="${K6_BOOK_ID:-}"
  K6_LIBRARY_ID="${K6_LIBRARY_ID:-}"
  K6_RECORD_ID="${K6_RECORD_ID:-}"
  K6_TIMELINE_ID="${K6_TIMELINE_ID:-}"
  K6_SEARCH_KEYWORD="${K6_SEARCH_KEYWORD:-}"
)

redacted_env_cmd=()
for entry in "${env_cmd[@]}"; do
  key="${entry%%=*}"
  value="${entry#*=}"
  case "$key" in
    TOKEN | K6_ACCESS_TOKEN | K6_REFRESH_TOKEN)
      if [[ -n "$value" ]]; then
        redacted_env_cmd+=("$key=[REDACTED]")
      else
        redacted_env_cmd+=("$key=")
      fi
      ;;
    *)
      redacted_env_cmd+=("$entry")
      ;;
  esac
done

echo "k6_env=$K6_ENV"
if [[ -f "$K6_ENV_FILE" ]]; then
  echo "k6_env_file=$K6_ENV_FILE"
fi
echo "scenario=$scenario"
echo "run_id=$RUN_ID"
echo "script=$K6_SCRIPT"
echo "base_url=$BASE_URL"
if [[ "$scenario" == "mixed-read" ]]; then
  echo "journeys_per_second=$JOURNEYS_PER_SECOND"
  echo "max_requests_per_journey=18"
  echo "expected_max_http_rps=$((JOURNEYS_PER_SECOND * 18))"
fi

if [[ "${K6_DRY_RUN:-}" == "1" ]]; then
  printf 'dry_run_command='
  printf '%q ' env "${redacted_env_cmd[@]}" "${compose_cmd[@]}"
  printf '\n'
  exit 0
fi

if env "${env_cmd[@]}" "${compose_cmd[@]}"; then
  if [[ "$scenario" == "seed" || "$scenario" == "prepare-seed" ]]; then
    mkdir -p "$K6_STATE_DIR"
    printf '%s\n' "$RUN_ID" > "$K6_SEED_STATE_FILE"
    echo "saved_seed_run_id=$K6_SEED_STATE_FILE"
  fi
  exit 0
else
  exit $?
fi
