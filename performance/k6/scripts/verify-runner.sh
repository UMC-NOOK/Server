#!/usr/bin/env bash
set -euo pipefail

server_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
cd "$server_dir"

runner="performance/k6/scripts/run-k6.sh"
entrypoint="performance/k6/scripts/k6-entrypoint.sh"
focus="all"
if [[ "${1:-}" == "--focus" ]]; then
  focus="${2:-}"
fi
if [[ "$focus" != "all" && "$focus" != "routes" && "$focus" != "safety" ]]; then
  printf 'unknown focus: %s\n' "$focus" >&2
  exit 2
fi

test_dir="$(mktemp -d)"
trap 'rm -rf "$test_dir"' EXIT
fake_bin="$test_dir/bin"
state_dir="$test_dir/state"
mkdir -p "$fake_bin" "$state_dir"

printf '%s\n' '#!/bin/sh' 'printf "fake-docker:%s\n" "$*"' 'exit "${FAKE_DOCKER_EXIT:-0}"' > "$fake_bin/docker"
printf '%s\n' '#!/bin/sh' 'printf "fake-k6:%s\n" "$*"' 'exit "${FAKE_K6_EXIT:-0}"' > "$fake_bin/k6"
chmod +x "$fake_bin/docker" "$fake_bin/k6"

failures=0
checks=0
captured_output=""
captured_status=0

capture() {
  set +e
  captured_output="$("$@" 2>&1)"
  captured_status=$?
  set -e
}

record_failure() {
  printf 'FAIL: %s\n%s\n' "$1" "$captured_output" >&2
  failures=$((failures + 1))
}

expect_status() {
  local expected="$1"
  local label="$2"
  checks=$((checks + 1))
  if [[ "$captured_status" -ne "$expected" ]]; then
    record_failure "$label: expected exit $expected, got $captured_status"
  fi
}

expect_contains() {
  local expected="$1"
  local label="$2"
  checks=$((checks + 1))
  if ! grep -Fq -- "$expected" <<<"$captured_output"; then
    record_failure "$label: missing '$expected'"
  fi
}

expect_absent() {
  local forbidden="$1"
  local label="$2"
  checks=$((checks + 1))
  if grep -Fq -- "$forbidden" <<<"$captured_output"; then
    record_failure "$label: exposed '$forbidden'"
  fi
}

base_runner_env=(
  env
  PATH="$fake_bin:$PATH"
  K6_ENV_FILE=/dev/null
  ENV_FILE=/dev/null
  K6_STATE_DIR="$state_dir"
  K6_DRY_RUN=1
  RUN_ID=verify-runner
  SEED_RUN_ID=verify-seed
  BASE_URL=http://host.docker.internal:8080
  K6_ENV=local
)

verify_route() {
  local spec="$1"
  local scenario profile expected_script expected_report
  IFS='|' read -r scenario profile expected_script expected_report <<<"$spec"
  local args=("$scenario")
  if [[ -n "$profile" ]]; then
    args+=("$profile")
  fi

  capture "${base_runner_env[@]}" bash "$runner" "${args[@]}"
  expect_status 0 "route $scenario ${profile:-default}"
  expect_contains "scenario=$scenario" "route $scenario"
  expect_contains "script=$expected_script" "route $scenario"
  expect_contains "K6_REPORT_NAME=$expected_report" "route $scenario"
}

if [[ "$focus" != "safety" ]]; then
  route_specs=(
    "smoke||performance/k6/scenarios/smoke.js|smoke"
    "seed||performance/k6/scenarios/prepare-seed.js|seed"
    "prepare-seed||performance/k6/scenarios/prepare-seed.js|seed"
    "mixed-read|jps1|performance/k6/scenarios/mixed-read-journey.js|mixed-read-journey"
    "mixed-read|jps5|performance/k6/scenarios/mixed-read-journey.js|mixed-read-journey"
    "mixed-read|jps10|performance/k6/scenarios/mixed-read-journey.js|mixed-read-journey"
    "books-user||performance/k6/scenarios/books-user.js|books-user"
    "books-search-library||performance/k6/scenarios/books-search-library.js|books-search-library"
    "search-library||performance/k6/scenarios/books-search-library.js|books-search-library"
    "books-search-global||performance/k6/scenarios/books-search-global.js|books-search-global"
    "global-search||performance/k6/scenarios/books-search-global.js|books-search-global"
    "aladin||performance/k6/scenarios/books-search-global.js|books-search-global"
    "onboarding||performance/k6/scenarios/onboarding.js|onboarding"
    "timeline-core||performance/k6/scenarios/timeline-core.js|timeline-core"
    "timeline-producers||performance/k6/scenarios/timeline-producers.js|timeline-producers"
  )
  for route_spec in "${route_specs[@]}"; do
    verify_route "$route_spec"
  done

  capture "${base_runner_env[@]}" bash "$runner" mixed-read jps1
  expect_contains "DURATION=1m" "mixed-read jps1 duration"
  expect_contains "PRE_ALLOCATED_VUS=5" "mixed-read jps1 VUs"
  expect_contains "MAX_VUS=20" "mixed-read jps1 max VUs"
  expect_contains "max_requests_per_journey=18" "mixed-read request count"

  capture "${base_runner_env[@]}" bash "$runner" mixed-read jps5
  expect_contains "DURATION=5m" "mixed-read jps5 duration"
  expect_contains "PRE_ALLOCATED_VUS=20" "mixed-read jps5 VUs"
  expect_contains "MAX_VUS=80" "mixed-read jps5 max VUs"

  capture "${base_runner_env[@]}" bash "$runner" mixed-read jps10
  expect_contains "DURATION=10m" "mixed-read jps10 duration"
  expect_contains "PRE_ALLOCATED_VUS=40" "mixed-read jps10 VUs"
  expect_contains "MAX_VUS=150" "mixed-read jps10 max VUs"

  capture "${base_runner_env[@]}" bash "$runner" books-user
  expect_contains "VUS=1" "internal journey VUs"
  expect_contains "ITERATIONS=1" "internal journey iterations"
  expect_contains "MAX_DURATION=2m" "internal journey max duration"

  capture "${base_runner_env[@]}" bash "$runner" books-search-global
  expect_contains "K6_ENABLE_EXTERNAL_API=yes" "global external flag"
  expect_contains "TARGET_RPS=1" "global target rate"

  capture "${base_runner_env[@]}" bash "$runner" mixed-read jps0
  expect_status 2 "invalid mixed-read profile"
  capture "${base_runner_env[@]}" bash "$runner" unknown-scenario
  expect_status 2 "unknown scenario"

  capture env PATH="$fake_bin:$PATH" K6_ENV_FILE=/dev/null ENV_FILE=/dev/null K6_STATE_DIR="$state_dir" \
    K6_ENV=local BASE_URL=http://host.docker.internal:8080 FAKE_DOCKER_EXIT=7 bash "$runner" smoke
  expect_status 7 "docker exit propagation"

  capture "$runner" --help
  expect_status 0 "direct executable runner"
fi

if [[ "$focus" != "routes" ]]; then
  staging_policy_env=(
    K6_ENV=staging
    BASE_URL=https://staging-api.example.com
    MANAGEMENT_BASE_URL=http://staging-api.example.com:9091
    K6_BASE_URL_PATTERN='^https://staging-api[.]example[.]com(:[0-9]+)?(/|$)'
    K6_MANAGEMENT_BASE_URL_PATTERN='^http://staging-api[.]example[.]com:9091(/|$)'
  )
  production_policy_env=(
    K6_ENV=prod
    BASE_URL=https://api.example.com
    MANAGEMENT_BASE_URL=http://management.example.internal:9091
    K6_BASE_URL_PATTERN='^https://api[.]example[.]com(:[0-9]+)?(/|$)'
    K6_MANAGEMENT_BASE_URL_PATTERN='^http://management[.]example[.]internal:9091(/|$)'
  )

  capture "${base_runner_env[@]}" bash "$runner" smoke
  expect_status 0 "local smoke"
  capture "${base_runner_env[@]}" bash "$runner" seed
  expect_status 0 "local seed"

  capture "${base_runner_env[@]}" "${staging_policy_env[@]}" bash "$runner" smoke
  expect_status 2 "remote smoke without confirmation"
  expect_contains "CONFIRM_PROD_LOADTEST=yes" "remote confirmation error"
  capture "${base_runner_env[@]}" "${staging_policy_env[@]}" CONFIRM_PROD_LOADTEST=yes bash "$runner" smoke
  expect_status 0 "confirmed remote smoke"
  capture "${base_runner_env[@]}" "${staging_policy_env[@]}" CONFIRM_PROD_LOADTEST=yes bash "$runner" seed
  expect_status 2 "remote seed remains denied"
  capture "${base_runner_env[@]}" K6_ENV=staging BASE_URL=https://service.example.internal MANAGEMENT_BASE_URL=https://service.example.internal K6_BASE_URL_PATTERN='^https://staging-api[.]example[.]com(:[0-9]+)?(/|$)' CONFIRM_PROD_LOADTEST=yes bash "$runner" mixed-read jps1
  expect_status 2 "unlisted remote target remains denied"
  capture "${base_runner_env[@]}" "${staging_policy_env[@]}" MANAGEMENT_BASE_URL=https://management.example.internal CONFIRM_PROD_LOADTEST=yes bash "$runner" smoke
  expect_status 2 "unlisted management target remains denied"

  capture "${base_runner_env[@]}" "${production_policy_env[@]}" bash "$runner" smoke
  expect_status 2 "production smoke without confirmation"
  capture "${base_runner_env[@]}" "${production_policy_env[@]}" CONFIRM_PROD_LOADTEST=yes bash "$runner" smoke
  expect_status 0 "confirmed production smoke"
  for production_scenario in mixed-read books-search-global seed; do
    capture "${base_runner_env[@]}" "${production_policy_env[@]}" CONFIRM_PROD_LOADTEST=yes \
      bash "$runner" "$production_scenario" jps1
    expect_status 2 "production $production_scenario remains denied"
  done

  capture "${base_runner_env[@]}" TOKEN=token-sentinel K6_ACCESS_TOKEN=access-sentinel \
    K6_REFRESH_TOKEN=refresh-sentinel bash "$runner" smoke
  expect_contains "REDACTED" "dry-run credential redaction"
  expect_absent "token-sentinel" "TOKEN redaction"
  expect_absent "access-sentinel" "K6_ACCESS_TOKEN redaction"
  expect_absent "refresh-sentinel" "K6_REFRESH_TOKEN redaction"

  entrypoint_env=(
    env
    PATH="$fake_bin:$PATH"
    K6_ENV=local
    BASE_URL=http://host.docker.internal:8080
    MANAGEMENT_BASE_URL=http://host.docker.internal:8080
    K6_SCRIPT=performance/k6/scenarios/smoke.js
  )
  capture "${entrypoint_env[@]}" sh "$entrypoint" run performance/k6/scenarios/smoke.js
  expect_status 0 "entrypoint local smoke"
  capture "${entrypoint_env[@]}" K6_ENV=staging BASE_URL=https://staging-api.example.com MANAGEMENT_BASE_URL=http://staging-api.example.com:9091 K6_BASE_URL_PATTERN='^https://staging-api[.]example[.]com(:[0-9]+)?(/|$)' K6_MANAGEMENT_BASE_URL_PATTERN='^http://staging-api[.]example[.]com:9091(/|$)' sh "$entrypoint" run performance/k6/scenarios/smoke.js
  expect_status 2 "entrypoint remote confirmation"
  capture "${entrypoint_env[@]}" K6_ENV=staging BASE_URL=https://staging-api.example.com MANAGEMENT_BASE_URL=http://staging-api.example.com:9091 K6_BASE_URL_PATTERN='^https://staging-api[.]example[.]com(:[0-9]+)?(/|$)' K6_MANAGEMENT_BASE_URL_PATTERN='^http://staging-api[.]example[.]com:9091(/|$)' CONFIRM_PROD_LOADTEST=yes \
    K6_SCRIPT=performance/k6/scenarios/prepare-seed.js sh "$entrypoint" run performance/k6/scenarios/prepare-seed.js
  expect_status 2 "entrypoint remote seed"
  capture "${entrypoint_env[@]}" K6_ENV=prod BASE_URL=https://api.example.com MANAGEMENT_BASE_URL=http://management.example.internal:9091 K6_BASE_URL_PATTERN='^https://api[.]example[.]com(:[0-9]+)?(/|$)' K6_MANAGEMENT_BASE_URL_PATTERN='^http://management[.]example[.]internal:9091(/|$)' CONFIRM_PROD_LOADTEST=yes \
    K6_SCRIPT=performance/k6/scenarios/mixed-read-journey.js sh "$entrypoint" run performance/k6/scenarios/mixed-read-journey.js
  expect_status 2 "entrypoint production mixed-read"
  capture "${entrypoint_env[@]}" K6_ENV=prod BASE_URL=https://api.example.com MANAGEMENT_BASE_URL=http://management.example.internal:9091 K6_BASE_URL_PATTERN='^https://api[.]example[.]com(:[0-9]+)?(/|$)' K6_MANAGEMENT_BASE_URL_PATTERN='^http://management[.]example[.]internal:9091(/|$)' CONFIRM_PROD_LOADTEST=yes \
    K6_SCRIPT=performance/k6/scenarios/smoke.js sh "$entrypoint" run performance/k6/scenarios/smoke.js
  expect_status 0 "entrypoint production smoke"
fi

if (( failures > 0 )); then
  printf 'runner verification failed: %s/%s checks\n' "$failures" "$checks" >&2
  exit 1
fi

printf 'verified k6 runner routes, safety policy, and redaction (%s checks)\n' "$checks"
