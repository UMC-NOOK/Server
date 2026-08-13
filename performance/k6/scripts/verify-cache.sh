#!/usr/bin/env bash
set -euo pipefail

server_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
cd "$server_dir"

runner="performance/k6/scripts/run-k6.sh"
cache_script="performance/k6/scenarios/cache-stats.js"
eviction_script="performance/k6/scripts/evict-stats-cache.sh"
test_dir="$(mktemp -d)"
trap 'rm -rf "$test_dir"' EXIT
state_dir="$test_dir/state"
manifest_dir="$state_dir/seeds"
fake_bin="$test_dir/bin"
mkdir -p "$manifest_dir" "$fake_bin"

write_manifest() {
  local environment="$1"
  cat > "$manifest_dir/seed-${environment}-cache-light.env" <<MANIFEST
SEED_NAMESPACE=cache-light
SEED_PROFILE=light
SEED_RUN_ID=verify-cache-seed
K6_USER_EMAIL=seed-cache-light@test.com
K6_USER_NICKNAME=seedcachelight
SEED_BOOKS=5
SEED_RECORDS_PER_BOOK=2
SEED_FOCUS_SESSIONS=2
K6_GIT_COMMIT_SHA=0123456789abcdef
MANIFEST
}

write_manifest local
write_manifest staging
printf 'cache-light\n' > "$state_dir/last-seed-local"
cp "$manifest_dir/seed-local-cache-light.env" "$test_dir/manifest-before"
cp "$state_dir/last-seed-local" "$test_dir/latest-before"

assert_line() {
  local output="$1"
  local expected="$2"
  grep -Fxq -- "$expected" <<<"$output" || {
    printf 'missing cache runner output: %s\n%s\n' "$expected" "$output" >&2
    exit 1
  }
}

assert_contains() {
  local output="$1"
  local expected="$2"
  grep -Fq -- "$expected" <<<"$output" || {
    printf 'missing cache output fragment: %s\n%s\n' "$expected" "$output" >&2
    exit 1
  }
}

assert_command_env() {
  local output="$1"
  local expected="$2"
  sed -n 's/^dry_run_command=//p' <<<"$output" | grep -Fq -- "$expected" || {
    printf 'missing cache dry-run environment: %s\n%s\n' "$expected" "$output" >&2
    exit 1
  }
}

expect_failure() {
  local label="$1"
  local expected_message=""
  local output
  shift
  if [[ "${1:-}" == "--message" ]]; then
    expected_message="$2"
    shift 2
  fi
  if output="$("$@" 2>&1)"; then
    printf 'expected failure: %s\n' "$label" >&2
    exit 1
  fi
  if [[ -n "$expected_message" ]] && ! grep -Fq -- "$expected_message" <<<"$output"; then
    printf 'unexpected failure reason for %s; expected: %s\n%s\n' \
      "$label" "$expected_message" "$output" >&2
    exit 1
  fi
}

inspect_options() {
  local target="$1"
  local phase="$2"
  K6_DOCKER_USER="$(id -u):$(id -g)" \
    ENV_FILE="${ENV_FILE:-performance/k6/env/monitoring.env.example}" \
    docker compose -f docker-compose.monitoring.yml --profile loadtest run --rm --no-deps \
      --entrypoint k6 k6 inspect --execution-requirements \
      -e "K6_CACHE_TARGET=$target" \
      -e "K6_CACHE_PHASE=$phase" \
      -e TARGET_RPS=13 \
      -e DURATION=7s \
      -e PRE_ALLOCATED_VUS=17 \
      -e MAX_VUS=33 \
      "/workspace/$cache_script"
}

base_env=(
  env
  -u TOKEN
  -u K6_ACCESS_TOKEN
  -u K6_REFRESH_TOKEN
  -u K6_USER_EMAIL
  -u K6_USER_NICKNAME
  -u RUN_ID
  K6_ENV=local
  K6_ENV_FILE=/dev/null
  K6_STATE_DIR="$state_dir"
  ENV_FILE=/dev/null
  BASE_URL=http://host.docker.internal:8080
  MANAGEMENT_BASE_URL=http://host.docker.internal:8080
  K6_DRY_RUN=1
  RUN_PREFIX=verify
  SEED_NAMESPACE=cache-light
  K6_STATS_YEAR_MONTH=2026-08
)

scenarios=(
  cache-monthly-cold
  cache-monthly-warm
  cache-focus-monthly-cold
  cache-focus-monthly-warm
)

for scenario in "${scenarios[@]}"; do
  output="$("${base_env[@]}" TARGET_RPS=13 DURATION=7s "$runner" "$scenario")"
  target="monthly"
  [[ "$scenario" == cache-focus-* ]] && target="focus-monthly"
  phase="warm"
  [[ "$scenario" == *-cold ]] && phase="cold"

  assert_line "$output" "scenario=$scenario"
  assert_line "$output" "script=$cache_script"
  assert_line "$output" "cache_target=$target"
  assert_line "$output" "cache_phase=$phase"
  assert_line "$output" "stats_year_month=2026-08"
  assert_line "$output" "request_name=cache:$target:$phase"
  assert_contains "$output" "run_id=verify-$scenario-"
  assert_command_env "$output" "K6_CACHE_TARGET=$target"
  assert_command_env "$output" "K6_CACHE_PHASE=$phase"
  assert_command_env "$output" "K6_USER_EMAIL=seed-cache-light@test.com"

  if [[ "$phase" == "cold" ]]; then
    assert_line "$output" "cache_evict=planned"
  else
    assert_line "$output" "target_rps=13"
  fi
done

cmp -s "$test_dir/manifest-before" "$manifest_dir/seed-local-cache-light.env" \
  || { printf 'cache dry-run changed the seed manifest\n' >&2; exit 1; }
cmp -s "$test_dir/latest-before" "$state_dir/last-seed-local" \
  || { printf 'cache dry-run changed the latest seed pointer\n' >&2; exit 1; }

for target in monthly focus-monthly; do
  cold_options="$(inspect_options "$target" cold)"
  cold_request="cache:$target:cold"
  jq -e --arg duration_key "http_req_duration{name:$cold_request}" \
    --arg failure_key "http_req_failed{name:$cold_request}" '
      .scenarios.cache_cold.executor == "shared-iterations" and
      .scenarios.cache_cold.vus == 1 and
      .scenarios.cache_cold.iterations == 1 and
      .thresholds.dropped_iterations == ["count<=0"] and
      .thresholds[$duration_key] == ["p(95)<1000"] and
      .thresholds[$failure_key] == ["rate<0.01"] and
      .thresholds.http_req_duration == null and
      .thresholds.http_req_failed == null
    ' <<<"$cold_options" >/dev/null || {
    printf 'invalid cold cache options for %s\n' "$target" >&2
    exit 1
  }

  warm_options="$(inspect_options "$target" warm)"
  warm_request="cache:$target:warm"
  jq -e --arg duration_key "http_req_duration{name:$warm_request}" \
    --arg failure_key "http_req_failed{name:$warm_request}" '
      .scenarios.cache_warm.executor == "constant-arrival-rate" and
      .scenarios.cache_warm.rate == 13 and
      .scenarios.cache_warm.timeUnit == "1s" and
      .scenarios.cache_warm.duration == "7s" and
      .scenarios.cache_warm.preAllocatedVUs == 17 and
      .scenarios.cache_warm.maxVUs == 33 and
      .thresholds.dropped_iterations == ["count<=0"] and
      .thresholds[$duration_key] == ["p(95)<1000"] and
      .thresholds[$failure_key] == ["rate<0.01"] and
      .thresholds.http_req_duration == null and
      .thresholds.http_req_failed == null
    ' <<<"$warm_options" >/dev/null || {
    printf 'invalid warm cache options for %s\n' "$target" >&2
    exit 1
  }
done

expect_failure "cold VUS override" "${base_env[@]}" VUS=2 "$runner" cache-monthly-cold
expect_failure "cold iterations override" "${base_env[@]}" ITERATIONS=2 "$runner" cache-monthly-cold
expect_failure "invalid year-month" "${base_env[@]}" K6_STATS_YEAR_MONTH=2026-8 "$runner" cache-monthly-cold
expect_failure "missing seed manifest" "${base_env[@]}" SEED_NAMESPACE=missing "$runner" cache-monthly-cold
expect_failure "configured access token" \
  --message "cache scenarios require an unmodified reusable seed manifest" \
  "${base_env[@]}" K6_ACCESS_TOKEN=forbidden "$runner" cache-monthly-cold
expect_failure "configured user identity" \
  --message "cache scenarios require an unmodified reusable seed manifest" \
  "${base_env[@]}" K6_USER_EMAIL=other@test.com "$runner" cache-monthly-cold

expect_failure "staging cache scenario" \
  --message "cache scenarios are allowed only against the local k6 environment" \
  env \
  -u TOKEN \
  -u K6_ACCESS_TOKEN \
  -u K6_REFRESH_TOKEN \
  -u K6_USER_EMAIL \
  -u K6_USER_NICKNAME \
  -u RUN_ID \
  K6_ENV=staging \
  K6_ENV_FILE=/dev/null \
  K6_STATE_DIR="$state_dir" \
  ENV_FILE=/dev/null \
  BASE_URL=https://staging-api.example.com \
  MANAGEMENT_BASE_URL=http://staging-api.example.com:9091 \
  K6_BASE_URL_PATTERN='^https://staging-api[.]example[.]com(/|$)' \
  K6_MANAGEMENT_BASE_URL_PATTERN='^http://staging-api[.]example[.]com:9091(/|$)' \
  CONFIRM_PROD_LOADTEST=yes \
  K6_DRY_RUN=1 \
  SEED_NAMESPACE=cache-light \
  "$runner" cache-monthly-warm

cat > "$fake_bin/curl" <<'FAKE_CURL'
#!/bin/sh
printf '%s\n' "$*" >> "$FAKE_CURL_LOG"
printf '{"result":{"id":42,"email":"%s","nickName":"%s"}}\n' \
  "${FAKE_LOGIN_EMAIL:-$K6_USER_EMAIL}" "${FAKE_LOGIN_NICKNAME:-$K6_USER_NICKNAME}"
FAKE_CURL
cat > "$fake_bin/docker" <<'FAKE_DOCKER'
#!/bin/sh
printf '%s\n' "$*" >> "$FAKE_DOCKER_LOG"
case "$*" in
  *" redis-cli --raw EXISTS "*)
    count=0
    [ ! -f "$FAKE_EXISTS_COUNTER" ] || count="$(cat "$FAKE_EXISTS_COUNTER")"
    count=$((count + 1))
    printf '%s\n' "$count" > "$FAKE_EXISTS_COUNTER"
    if [ "$count" -eq 1 ]; then
      printf '%s\n' "${FAKE_REDIS_EXISTING:-0}"
    else
      printf '%s\n' "${FAKE_REDIS_REMAINING:-0}"
    fi
    ;;
  *" redis-cli --raw DEL "*) printf '%s\n' "${FAKE_REDIS_DELETED:-0}" ;;
  *) exit 9 ;;
esac
FAKE_DOCKER
chmod +x "$fake_bin/curl" "$fake_bin/docker"

run_eviction() {
  local target="$1"
  local suffix="$2"
  shift 2
  : > "$test_dir/docker-$suffix.log"
  : > "$test_dir/curl-$suffix.log"
  rm -f "$test_dir/exists-$suffix"
  env \
    PATH="$fake_bin:$PATH" \
    FAKE_CURL_LOG="$test_dir/curl-$suffix.log" \
    FAKE_DOCKER_LOG="$test_dir/docker-$suffix.log" \
    FAKE_EXISTS_COUNTER="$test_dir/exists-$suffix" \
    BASE_URL=http://host.docker.internal:8080 \
    K6_CACHE_TARGET="$target" \
    K6_STATS_YEAR_MONTH=2026-08 \
    K6_USER_EMAIL=seed-cache-light@test.com \
    K6_USER_NICKNAME=seedcachelight \
    "$@" bash "$eviction_script"
}

monthly_output="$(run_eviction monthly monthly-ok \
  FAKE_REDIS_EXISTING=2 FAKE_REDIS_DELETED=2 FAKE_REDIS_REMAINING=0)"
assert_line "$monthly_output" "cache_user_id=42"
assert_line "$monthly_output" "cache_keys_targeted=3"
assert_line "$monthly_output" "cache_keys_deleted=2"
assert_line "$monthly_output" "cache_keys_remaining=0"
assert_contains "$(<"$test_dir/curl-monthly-ok.log")" "--connect-timeout 5"
assert_contains "$(<"$test_dir/curl-monthly-ok.log")" "--max-time 30"
assert_contains "$(<"$test_dir/docker-monthly-ok.log")" \
  "stats:library:monthly:zset:42:2026-08 stats:library:monthly:total:42:2026-08 stats:library:monthly:exists:42:2026-08"

focus_output="$(run_eviction focus-monthly focus-ok \
  FAKE_REDIS_EXISTING=0 FAKE_REDIS_DELETED=0 FAKE_REDIS_REMAINING=0)"
assert_line "$focus_output" "cache_keys_targeted=2"
assert_contains "$(<"$test_dir/docker-focus-ok.log")" \
  "stats:focus:daily:42:202608 stats:focus:daily:exists:42:202608"

renamed_output="$(run_eviction monthly renamed-user \
  FAKE_LOGIN_NICKNAME=onboardednickname \
  FAKE_REDIS_EXISTING=0 FAKE_REDIS_DELETED=0 FAKE_REDIS_REMAINING=0)"
assert_line "$renamed_output" "cache_user_id=42"

expect_failure "manifest login email mismatch" \
  --message "DEV login identity does not match the reusable seed manifest" \
  run_eviction monthly identity-mismatch \
  FAKE_LOGIN_EMAIL=other@test.com
expect_failure "delete count mismatch" \
  --message "Redis cache eviction did not delete the expected existing keys" \
  run_eviction monthly delete-mismatch \
  FAKE_REDIS_EXISTING=2 FAKE_REDIS_DELETED=1 FAKE_REDIS_REMAINING=0
expect_failure "keys remain after eviction" \
  --message "Redis cache eviction left target keys behind" \
  run_eviction monthly keys-remain \
  FAKE_REDIS_EXISTING=2 FAKE_REDIS_DELETED=2 FAKE_REDIS_REMAINING=1
expect_failure "non-local eviction" \
  --message "automatic cache eviction is allowed only for a local BASE_URL" \
  env \
  PATH="$fake_bin:$PATH" \
  BASE_URL=https://staging-api.example.com \
  K6_CACHE_TARGET=monthly \
  K6_STATS_YEAR_MONTH=2026-08 \
  K6_USER_EMAIL=seed-cache-light@test.com \
  K6_USER_NICKNAME=seedcachelight \
  bash "$eviction_script"

printf 'verified 4 local cache routes, cold/warm options, manifest identity, and exact eviction\n'
