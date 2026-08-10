#!/usr/bin/env bash
set -euo pipefail

server_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
cd "$server_dir"

runner="performance/k6/scripts/run-k6.sh"
single_api_script="performance/k6/scenarios/single-api-read.js"
test_dir="$(mktemp -d)"
trap 'rm -rf "$test_dir"' EXIT
state_dir="$test_dir/state"
manifest_dir="$state_dir/seeds"
mkdir -p "$manifest_dir"

cat > "$manifest_dir/seed-local-api-light.env" <<'MANIFEST'
SEED_NAMESPACE=api-light
SEED_PROFILE=light
SEED_RUN_ID=verify-seed
K6_USER_EMAIL=seed-api-light@test.com
K6_USER_NICKNAME=seedapilight
SEED_BOOKS=5
SEED_RECORDS_PER_BOOK=2
SEED_FOCUS_SESSIONS=2
K6_GIT_COMMIT_SHA=0123456789abcdef
MANIFEST

assert_line() {
  local output="$1"
  local expected="$2"
  grep -Fxq -- "$expected" <<<"$output" || {
    printf 'missing single-API runner output: %s\n%s\n' "$expected" "$output" >&2
    exit 1
  }
}

assert_command_env() {
  local output="$1"
  local expected="$2"
  sed -n 's/^dry_run_command=//p' <<<"$output" | grep -Fq -- "$expected" || {
    printf 'missing single-API dry-run environment: %s\n%s\n' "$expected" "$output" >&2
    exit 1
  }
}

expect_failure() {
  local label="$1"
  shift
  if "$@" >/dev/null 2>&1; then
    printf 'expected failure: %s\n' "$label" >&2
    exit 1
  fi
}

inspect_options() {
  local read_target="$1"
  local profile="$2"
  K6_DOCKER_USER="$(id -u):$(id -g)" \
    ENV_FILE="${ENV_FILE:-performance/k6/env/monitoring.env.example}" \
    docker compose -f docker-compose.monitoring.yml --profile loadtest run --rm --no-deps \
      --entrypoint k6 k6 inspect --execution-requirements \
      -e "K6_READ_TARGET=$read_target" \
      -e "K6_SINGLE_API_PROFILE=$profile" \
      -e TARGET_RPS=13 \
      -e START_RPS=2 \
      -e RPS_STAGES=4:10s,8:10s,0:5s \
      -e TIME_UNIT=1m \
      -e DURATION=7s \
      "/workspace/$single_api_script"
}

base_env=(
  env
  K6_ENV=local
  K6_ENV_FILE=/dev/null
  K6_STATE_DIR="$state_dir"
  ENV_FILE=/dev/null
  BASE_URL=http://host.docker.internal:8080
  MANAGEMENT_BASE_URL=http://host.docker.internal:8080
  K6_DRY_RUN=1
  SEED_NAMESPACE=api-light
)

scenarios=(
  api-timeline-list
  api-timeline-summary
  api-timeline-detail
  api-library-books-recent-focused
  api-library-books-record-count-desc
  api-library-books-record-count-asc
  api-library-books-alphabetical
  api-records-list
  api-records-book-list
  api-records-emotions
  api-records-detail
)

for scenario in "${scenarios[@]}"; do
  read_target="${scenario#api-}"
  request_name="read:$read_target"
  output="$(
    "${base_env[@]}" RUN_ID="verify-$scenario" TARGET_RPS=13 DURATION=7s \
      "$runner" "$scenario" arrival
  )"

  assert_line "$output" "scenario=$scenario"
  assert_line "$output" "script=$single_api_script"
  assert_line "$output" "single_api_profile=arrival"
  assert_line "$output" "target_rps=13"
  assert_command_env "$output" "K6_READ_TARGET=$read_target"
  assert_command_env "$output" "K6_SINGLE_API_PROFILE=arrival"
  assert_command_env "$output" "K6_USER_EMAIL=seed-api-light@test.com"

  options_json="$(inspect_options "$read_target" arrival)"
  duration_key="http_req_duration{name:$request_name}"
  failure_key="http_req_failed{name:$request_name}"
  jq -e --arg duration_key "$duration_key" --arg failure_key "$failure_key" '
    .scenarios.steady_state.executor == "constant-arrival-rate" and
    .scenarios.steady_state.rate == 13 and
    .scenarios.steady_state.timeUnit == "1s" and
    .scenarios.steady_state.duration == "7s" and
    .thresholds.dropped_iterations == ["count<=0"] and
    .thresholds[$duration_key] == ["p(95)<1000"] and
    .thresholds[$failure_key] == ["rate<0.01"] and
    .thresholds.http_req_duration == null and
    .thresholds.http_req_failed == null and
    .thresholds.checks == ["rate>0.99"]
  ' <<<"$options_json" >/dev/null || {
    printf 'invalid arrival options for %s\n' "$scenario" >&2
    exit 1
  }
done

ramping_output="$(
  "${base_env[@]}" RUN_ID=verify-api-ramping START_RPS=2 RPS_STAGES=4:10s,8:10s,0:5s \
    "$runner" api-timeline-list ramping
)"
assert_line "$ramping_output" "single_api_profile=ramping"
assert_line "$ramping_output" "start_rps=2"
assert_line "$ramping_output" "rps_stages=4:10s,8:10s,0:5s"
assert_command_env "$ramping_output" "K6_SINGLE_API_PROFILE=ramping"
assert_command_env "$ramping_output" "START_RPS=2"

ramping_options_json="$(inspect_options timeline-list ramping)"
jq -e '
  .scenarios.api_bottleneck.executor == "ramping-arrival-rate" and
  .scenarios.api_bottleneck.startRate == 2 and
  .scenarios.api_bottleneck.timeUnit == "1s" and
  .scenarios.api_bottleneck.stages == [
    {"duration": "10s", "target": 4},
    {"duration": "10s", "target": 8},
    {"duration": "5s", "target": 0}
  ]
' \
  <<<"$ramping_options_json" >/dev/null || {
  printf 'invalid ramping single-API options\n' >&2
  exit 1
}

expect_failure "TARGET_RPS=0" "${base_env[@]}" TARGET_RPS=0 "$runner" api-timeline-list arrival
expect_failure "unknown API target" "${base_env[@]}" "$runner" api-unknown arrival
expect_failure "unknown API profile" "${base_env[@]}" "$runner" api-timeline-list unknown
expect_failure "missing seed manifest" env K6_ENV=local K6_ENV_FILE=/dev/null K6_STATE_DIR="$state_dir" \
  ENV_FILE=/dev/null BASE_URL=http://host.docker.internal:8080 MANAGEMENT_BASE_URL=http://host.docker.internal:8080 \
  K6_DRY_RUN=1 SEED_NAMESPACE=missing "$runner" api-timeline-list arrival

printf 'verified %s single-API routes with arrival and ramping options\n' "${#scenarios[@]}"
