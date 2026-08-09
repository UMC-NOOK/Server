#!/usr/bin/env bash
set -euo pipefail

server_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
cd "$server_dir"

runner="performance/k6/scripts/run-k6.sh"
test_dir="$(mktemp -d)"
trap 'rm -rf "$test_dir"' EXIT
fake_bin="$test_dir/bin"
state_dir="$test_dir/state"
mkdir -p "$fake_bin" "$state_dir"

printf '%s\n' '#!/bin/sh' 'printf "fake-docker:%s\n" "$*"' 'exit "${FAKE_DOCKER_EXIT:-0}"' > "$fake_bin/docker"
chmod +x "$fake_bin/docker"

base_env=(
  env
  PATH="$fake_bin:$PATH"
  K6_ENV=local
  K6_ENV_FILE=/dev/null
  K6_STATE_DIR="$state_dir"
  ENV_FILE=/dev/null
  BASE_URL=http://host.docker.internal:8080
  MANAGEMENT_BASE_URL=http://host.docker.internal:8080
  K6_GIT_COMMIT_SHA=0123456789abcdef
)

assert_line() {
  local output="$1"
  local expected="$2"
  grep -Fxq -- "$expected" <<<"$output" || {
    printf 'missing seed runner output: %s\n%s\n' "$expected" "$output" >&2
    exit 1
  }
}

assert_command_env() {
  local output="$1"
  local expected="$2"
  sed -n 's/^dry_run_command=//p' <<<"$output" | grep -Fq -- "$expected" || {
    printf 'missing seed dry-run environment: %s\n%s\n' "$expected" "$output" >&2
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

dry_seed() {
  local profile="$1"
  local namespace="$2"
  "${base_env[@]}" K6_DRY_RUN=1 RUN_ID="verify-$profile" SEED_NAMESPACE="$namespace" \
    "$runner" seed "$profile"
}

verify_profile() {
  local spec="$1"
  local profile books records focuses output
  IFS='|' read -r profile books records focuses <<<"$spec"
  output="$(dry_seed "$profile" "verify-$profile")"
  assert_line "$output" "seed_mode=create"
  assert_line "$output" "seed_profile=$profile"
  assert_line "$output" "seed_namespace=verify-$profile"
  assert_line "$output" "script=performance/k6/scenarios/prepare-seed.js"
  assert_command_env "$output" "SEED_PROFILE=$profile"
  assert_command_env "$output" "SEED_NAMESPACE=verify-$profile"
  assert_command_env "$output" "SEED_BOOKS=$books"
  assert_command_env "$output" "SEED_RECORDS_PER_BOOK=$records"
  assert_command_env "$output" "SEED_FOCUS_SESSIONS=$focuses"
  assert_command_env "$output" "K6_USER_EMAIL=seed-verify-$profile@test.com"
}

for profile_spec in "light|5|2|2" "normal|30|3|10" "heavy|300|10|100"; do
  verify_profile "$profile_spec"
done

profile_env="$test_dir/profile.env"
printf '%s\n' \
  'SEED_PROFILE=light' \
  'SEED_NAMESPACE=env-light' \
  'SEED_BOOKS=999' \
  'SEED_RECORDS_PER_BOOK=999' \
  'SEED_FOCUS_SESSIONS=999' > "$profile_env"
profile_output="$(
  "${base_env[@]}" K6_ENV_FILE="$profile_env" K6_DRY_RUN=1 RUN_ID=verify-env "$runner" seed
)"
assert_line "$profile_output" "seed_profile=light"
assert_line "$profile_output" "seed_namespace=env-light"
assert_command_env "$profile_output" "SEED_BOOKS=5"
assert_command_env "$profile_output" "SEED_RECORDS_PER_BOOK=2"
assert_command_env "$profile_output" "SEED_FOCUS_SESSIONS=2"

expect_failure "unknown seed profile" "${base_env[@]}" K6_DRY_RUN=1 "$runner" seed unknown
expect_failure "invalid seed namespace" "${base_env[@]}" K6_DRY_RUN=1 SEED_NAMESPACE='invalid namespace' "$runner" seed light
expect_failure "seed token override" "${base_env[@]}" K6_DRY_RUN=1 K6_ACCESS_TOKEN=test-token "$runner" seed light

namespace="lifecycle-light"
create_output="$(
  "${base_env[@]}" RUN_ID=verify-create SEED_NAMESPACE="$namespace" "$runner" seed light
)"
manifest="$state_dir/seeds/seed-local-$namespace.env"
pointer="$state_dir/last-seed-local"
test -f "$manifest"
test "$(stat -c '%a' "$manifest")" = "600"
grep -Fxq 'SEED_NAMESPACE=lifecycle-light' "$manifest"
grep -Fxq 'SEED_PROFILE=light' "$manifest"
grep -Fxq 'SEED_RUN_ID=verify-create' "$manifest"
grep -Fxq 'SEED_BOOKS=5' "$manifest"
grep -Fxq 'SEED_RECORDS_PER_BOOK=2' "$manifest"
grep -Fxq 'SEED_FOCUS_SESSIONS=2' "$manifest"
grep -Fxq 'K6_GIT_COMMIT_SHA=0123456789abcdef' "$manifest"
test "$(<"$pointer")" = "$namespace"
assert_line "$create_output" "saved_seed_manifest=$manifest"

reuse_output="$(dry_seed light "$namespace")"
assert_line "$reuse_output" "seed_mode=reuse"
assert_line "$reuse_output" "script=performance/k6/scenarios/verify-seed.js"
assert_command_env "$reuse_output" "SEED_RUN_ID=verify-create"
assert_command_env "$reuse_output" "K6_USER_EMAIL=seed-lifecycle-light@test.com"
expect_failure "namespace profile mismatch" dry_seed heavy "$namespace"

mixed_output="$(
  "${base_env[@]}" K6_DRY_RUN=1 RUN_ID=verify-mixed SEED_NAMESPACE="$namespace" "$runner" mixed-read jps1
)"
assert_command_env "$mixed_output" "SEED_PROFILE=light"
assert_command_env "$mixed_output" "SEED_NAMESPACE=$namespace"
assert_command_env "$mixed_output" "K6_USER_EMAIL=seed-lifecycle-light@test.com"

cleanup_dry_output="$(
  "${base_env[@]}" K6_DRY_RUN=1 RUN_ID=verify-cleanup SEED_NAMESPACE="$namespace" "$runner" cleanup-seed
)"
assert_line "$cleanup_dry_output" "seed_mode=cleanup"
assert_line "$cleanup_dry_output" "script=performance/k6/scenarios/cleanup-seed.js"
assert_command_env "$cleanup_dry_output" "K6_USER_EMAIL=seed-lifecycle-light@test.com"

"${base_env[@]}" RUN_ID=verify-cleanup SEED_NAMESPACE="$namespace" "$runner" cleanup-seed >/dev/null
test ! -e "$manifest"
test ! -e "$pointer"

"${base_env[@]}" RUN_ID=verify-recovery SEED_NAMESPACE=recovery "$runner" cleanup-seed >/dev/null
expect_failure "remote cleanup" "${base_env[@]}" K6_ENV=staging BASE_URL=https://staging.example.test MANAGEMENT_BASE_URL=https://staging.example.test CONFIRM_PROD_LOADTEST=yes SEED_NAMESPACE=recovery "$runner" cleanup-seed
expect_failure "cleanup token override" "${base_env[@]}" K6_ACCESS_TOKEN=test-token SEED_NAMESPACE=recovery "$runner" cleanup-seed

expect_failure "failed seed command" "${base_env[@]}" FAKE_DOCKER_EXIT=7 RUN_ID=verify-failed SEED_NAMESPACE=failed "$runner" seed light
test ! -e "$state_dir/seeds/seed-local-failed.env"

printf 'verified seed profiles, manifests, reuse, mixed-read identity, cleanup, and failure recovery\n'
