#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage: performance/k6/scripts/run-k6.sh <scenario> [jps<positive integer>]

Scenarios:
  smoke, seed, mixed-read, books-user, books-search-library,
  books-search-global, onboarding, timeline-core, timeline-producers

Set K6_ENV=local|staging|prod to select performance/k6/env/<name>.env.
Every non-local HTTP target requires CONFIRM_PROD_LOADTEST=yes.
Seed is local-only; production allows smoke only. Set K6_DRY_RUN=1 to inspect.
USAGE
}

die() {
  echo "error: $*" >&2
  exit 2
}

load_env_file() {
  local file="$1" line key value
  [[ -f "$file" ]] || return 0
  while IFS= read -r line || [[ -n "$line" ]]; do
    line="${line%$'\r'}"
    line="${line#"${line%%[![:space:]]*}"}"
    line="${line%"${line##*[![:space:]]}"}"
    [[ -z "$line" || "$line" == \#* || "$line" != *=* ]] && continue
    key="${line%%=*}"
    value="${line#*=}"
    key="${key//[[:space:]]/}"
    value="${value#"${value%%[![:space:]]*}"}"
    value="${value%"${value##*[![:space:]]}"}"
    [[ "$key" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]] || continue
    if [[ "$value" == \"*\" && "$value" == *\" ]] || [[ "$value" == \'*\' && "$value" == *\' ]]; then
      value="${value:1:${#value}-2}"
    fi
    [[ -n "${!key+x}" ]] || export "$key=$value"
  done < "$file"
}

configure_internal() {
  K6_SCRIPT="performance/k6/scenarios/$1.js"
  K6_REPORT_NAME="$1"
  RUN_ID="${RUN_ID:-${run_prefix}-$1-${timestamp}}"
  VUS="${VUS:-1}"
  ITERATIONS="${ITERATIONS:-1}"
  MAX_DURATION="${MAX_DURATION:-2m}"
  P95_THRESHOLD_MS="${P95_THRESHOLD_MS:-800}"
  FAILED_RATE_THRESHOLD="${FAILED_RATE_THRESHOLD:-0.01}"
}

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
server_dir="$(cd "$script_dir/../../.." && pwd)"
cd "$server_dir"
source "$script_dir/target-policy.sh"

scenario="${1:-}"
profile="${2:-jps1}"
[[ "$profile" =~ ^[0-9]+$ ]] && profile="jps$profile"
if [[ -z "$scenario" || "$scenario" == "-h" || "$scenario" == "--help" ]]; then
  usage
  exit 0
fi

K6_ENV="${K6_ENV:-${ENV:-local}}"
K6_ENV_FILE="${K6_ENV_FILE:-performance/k6/env/${K6_ENV}.env}"
load_env_file "$K6_ENV_FILE"

K6_STATE_DIR="${K6_STATE_DIR:-performance/k6/state}"
K6_SEED_STATE_FILE="${K6_SEED_STATE_FILE:-${K6_STATE_DIR}/last-seed-${K6_ENV}}"
ENV_FILE="${ENV_FILE:-performance/k6/env/monitoring.env}"
GRAFANA_PORT="${GRAFANA_PORT:-3001}"
K6_DOCKER_USER="${K6_DOCKER_USER:-$(id -u):$(id -g)}"
BASE_URL="${BASE_URL:-http://host.docker.internal:8080}"
MANAGEMENT_BASE_URL="${MANAGEMENT_BASE_URL:-}"
timestamp="$(date +%Y%m%d-%H%M%S)-$$"
run_prefix="${RUN_PREFIX:-$K6_ENV}"
RUN_PREFIX="$run_prefix"

case "$scenario" in
  smoke)
    K6_SCRIPT="performance/k6/scenarios/smoke.js"
    K6_REPORT_NAME="smoke"
    RUN_ID="${RUN_ID:-${run_prefix}-smoke-${timestamp}}"
    ;;
  seed | prepare-seed)
    K6_SCRIPT="performance/k6/scenarios/prepare-seed.js"
    K6_REPORT_NAME="seed"
    RUN_ID="${RUN_ID:-${run_prefix}-seed-${timestamp}}"
    SEED_BOOKS="${SEED_BOOKS:-30}"
    SEED_RECORDS_PER_BOOK="${SEED_RECORDS_PER_BOOK:-3}"
    SEED_FOCUS_SESSIONS="${SEED_FOCUS_SESSIONS:-10}"
    ;;
  mixed-read)
    K6_SCRIPT="performance/k6/scenarios/mixed-read-journey.js"
    K6_REPORT_NAME="mixed-read-journey"
    [[ "$profile" =~ ^jps([1-9][0-9]*)$ ]] || die "unknown mixed-read profile '$profile' (expected jps<positive integer>)"
    JOURNEYS_PER_SECOND="${JOURNEYS_PER_SECOND:-${BASH_REMATCH[1]}}"
    [[ "$JOURNEYS_PER_SECOND" =~ ^[1-9][0-9]*$ ]] || die "JOURNEYS_PER_SECOND must be a positive integer"
    if (( JOURNEYS_PER_SECOND < 5 )); then
      DURATION="${DURATION:-1m}" PRE_ALLOCATED_VUS="${PRE_ALLOCATED_VUS:-5}" MAX_VUS="${MAX_VUS:-20}"
    elif (( JOURNEYS_PER_SECOND < 10 )); then
      DURATION="${DURATION:-5m}" PRE_ALLOCATED_VUS="${PRE_ALLOCATED_VUS:-20}" MAX_VUS="${MAX_VUS:-80}"
    else
      DURATION="${DURATION:-10m}" PRE_ALLOCATED_VUS="${PRE_ALLOCATED_VUS:-40}" MAX_VUS="${MAX_VUS:-150}"
    fi
    RUN_ID="${RUN_ID:-${run_prefix}-mixed-read-jps${JOURNEYS_PER_SECOND}-${timestamp}}"
    P95_THRESHOLD_MS="${P95_THRESHOLD_MS:-1000}"
    FAILED_RATE_THRESHOLD="${FAILED_RATE_THRESHOLD:-0.01}"
    if [[ -z "${K6_ACCESS_TOKEN:-}" && -z "${TOKEN:-}" && -z "${K6_USER_EMAIL:-}" ]]; then
      [[ -n "${SEED_RUN_ID:-}" || ! -f "$K6_SEED_STATE_FILE" ]] || SEED_RUN_ID="$(<"$K6_SEED_STATE_FILE")"
      [[ -n "${SEED_RUN_ID:-}" ]] || die "mixed-read needs a token, K6_USER_EMAIL, or SEED_RUN_ID; run seed first or provide one"
      K6_USER_EMAIL="seed-${SEED_RUN_ID}-0-0@test.com"
      K6_USER_NICKNAME="${K6_USER_NICKNAME:-k6read}"
    fi
    ;;
  books-user) configure_internal "books-user" ;;
  books-search-library | search-library) configure_internal "books-search-library" ;;
  books-search-global | global-search | aladin)
    K6_SCRIPT="performance/k6/scenarios/books-search-global.js"
    K6_REPORT_NAME="books-search-global"
    RUN_ID="${RUN_ID:-${run_prefix}-books-search-global-${timestamp}}"
    K6_ENABLE_EXTERNAL_API="${K6_ENABLE_EXTERNAL_API:-yes}"
    TARGET_RPS="${TARGET_RPS:-1}"
    DURATION="${DURATION:-1m}" PRE_ALLOCATED_VUS="${PRE_ALLOCATED_VUS:-5}" MAX_VUS="${MAX_VUS:-20}"
    K6_GLOBAL_SEARCH_KEYWORDS="${K6_GLOBAL_SEARCH_KEYWORDS:-자바,클린코드,해리포터}"
    K6_GLOBAL_USER_POOL_SIZE="${K6_GLOBAL_USER_POOL_SIZE:-$MAX_VUS}"
    P95_THRESHOLD_MS="${K6_GLOBAL_P95_THRESHOLD_MS:-5000}"
    FAILED_RATE_THRESHOLD="${K6_GLOBAL_FAILED_RATE_THRESHOLD:-0.05}"
    ;;
  onboarding) configure_internal "onboarding" ;;
  timeline-core) configure_internal "timeline-core" ;;
  timeline-producers) configure_internal "timeline-producers" ;;
  *) die "unknown scenario '$scenario'" ;;
esac

k6_assert_target_allowed "$K6_SCRIPT"

forwarded_names=(
  BASE_URL MANAGEMENT_BASE_URL K6_SCRIPT RUN_ID K6_REPORT_NAME
  P95_THRESHOLD_MS FAILED_RATE_THRESHOLD MAX_DROPPED_ITERATIONS
  VUS ITERATIONS MAX_DURATION JOURNEYS_PER_SECOND TARGET_RPS DURATION PRE_ALLOCATED_VUS MAX_VUS
  K6_ENV RUN_PREFIX K6_REQUIRE_CONFIRM K6_PROD_BASE_URL_PATTERN CONFIRM_PROD_LOADTEST
  K6_ENABLE_EXTERNAL_API K6_GLOBAL_SEARCH_KEYWORDS K6_GLOBAL_USER_POOL_SIZE
  SEED_BOOKS SEED_RECORDS_PER_BOOK SEED_FOCUS_SESSIONS
  K6_USER_EMAIL K6_USER_NICKNAME K6_ACCESS_TOKEN K6_REFRESH_TOKEN TOKEN
  K6_BOOK_ID K6_LIBRARY_ID K6_RECORD_ID K6_TIMELINE_ID K6_SEARCH_KEYWORD
)
compose_cmd=(docker compose -f docker-compose.monitoring.yml --profile loadtest run --rm)
redacted_cmd=("${compose_cmd[@]}")
for key in "${forwarded_names[@]}"; do
  value="${!key-}"
  export "$key=$value"
  compose_cmd+=(--env "$key=$value")
  case "$key" in
    TOKEN | K6_ACCESS_TOKEN | K6_REFRESH_TOKEN) [[ -z "$value" ]] || value='[REDACTED]' ;;
  esac
  redacted_cmd+=(--env "$key=$value")
done
compose_cmd+=(k6)
redacted_cmd+=(k6)
export ENV_FILE GRAFANA_PORT K6_DOCKER_USER

printf 'k6_env=%s\nscenario=%s\nrun_id=%s\nscript=%s\nbase_url=%s\n' \
  "$K6_ENV" "$scenario" "$RUN_ID" "$K6_SCRIPT" "$BASE_URL"
[[ ! -f "$K6_ENV_FILE" ]] || printf 'k6_env_file=%s\n' "$K6_ENV_FILE"
if [[ "$scenario" == "mixed-read" ]]; then
  printf 'journeys_per_second=%s\nmax_requests_per_journey=18\nexpected_max_http_rps=%s\n' \
    "$JOURNEYS_PER_SECOND" "$((JOURNEYS_PER_SECOND * 18))"
fi
if [[ "${K6_DRY_RUN:-}" == "1" ]]; then
  printf 'dry_run_command='
  printf '%q ' "${redacted_cmd[@]}"
  printf '\n'
  exit 0
fi

"${compose_cmd[@]}"
if [[ "$scenario" == "seed" || "$scenario" == "prepare-seed" ]]; then
  mkdir -p "$K6_STATE_DIR"
  printf '%s\n' "$RUN_ID" > "$K6_SEED_STATE_FILE"
  printf 'saved_seed_run_id=%s\n' "$K6_SEED_STATE_FILE"
fi
