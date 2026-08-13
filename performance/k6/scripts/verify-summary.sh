#!/usr/bin/env bash
set -euo pipefail

server_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
cd "$server_dir"

test_suffix="$$"
report_name="summary-contract-$test_suffix"
report_run_id="verify-summary-$test_suffix"
fallback_report_name="summary-fallback-$test_suffix"
report="performance/k6/reports/${report_name}-${report_run_id}.json"
fallback_report="performance/k6/reports/${fallback_report_name}-1001.json"
rm -f "$report" "$fallback_report"
trap 'rm -f "$report" "$fallback_report"' EXIT

K6_DOCKER_USER="$(id -u):$(id -g)" \
  ENV_FILE="${ENV_FILE:-performance/k6/env/monitoring.env.example}" \
  docker compose -f docker-compose.monitoring.yml --profile loadtest run --rm --no-deps \
    --entrypoint k6 k6 run --quiet \
    -e K6_REPORT_NAME="$report_name" \
    -e K6_SCENARIO_NAME="$report_name" \
    -e RUN_ID="$report_run_id" \
    -e K6_ENV=contract-env \
    -e BASE_URL=https://contract.example.test \
    -e K6_GIT_COMMIT_SHA=0123456789abcdef \
    -e SEED_GIT_COMMIT_SHA=fedcba9876543210 \
    -e SEED_PROFILE=heavy \
    -e SEED_NAMESPACE=contract-heavy \
    -e K6_CACHE_TARGET=focus-monthly \
    -e K6_CACHE_PHASE=warm \
    -e K6_STATS_YEAR_MONTH=2026-08 \
    /workspace/performance/k6/tests/summary-metadata.js >/dev/null

jq -e --arg run_id "$report_run_id" --arg test_name "$report_name" '
  .metadata == {
    "run_id": $run_id,
    "test_name": $test_name,
    "k6_env": "contract-env",
    "base_url": "https://contract.example.test",
    "git_commit_sha": "0123456789abcdef",
    "seed_git_commit_sha": "fedcba9876543210",
    "seed_profile": "heavy",
    "seed_namespace": "contract-heavy",
    "cache_target": "focus-monthly",
    "cache_phase": "warm",
    "stats_year_month": "2026-08"
  }
' "$report" >/dev/null || {
  printf 'summary report is missing reproducibility metadata\n' >&2
  exit 1
}

K6_DOCKER_USER="$(id -u):$(id -g)" \
  ENV_FILE="${ENV_FILE:-performance/k6/env/monitoring.env.example}" \
  docker compose -f docker-compose.monitoring.yml --profile loadtest run --rm --no-deps \
    --entrypoint k6 k6 run --quiet \
    -e K6_REPORT_NAME="$fallback_report_name" \
    -e RUN_ID= \
    -e K6_TEST_INCREMENTING_CLOCK=yes \
    /workspace/performance/k6/tests/summary-metadata.js >/dev/null

jq -e '.metadata.run_id == "1001"' "$fallback_report" >/dev/null || {
  printf 'summary fallback run ID does not match its report filename\n' >&2
  exit 1
}

printf 'verified summary reproducibility metadata\n'
