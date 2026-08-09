#!/usr/bin/env bash
set -euo pipefail

server_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
cd "$server_dir"

report="performance/k6/reports/summary-contract-verify-summary.json"
trap 'rm -f "$report"' EXIT

K6_DOCKER_USER="$(id -u):$(id -g)" \
  ENV_FILE="${ENV_FILE:-performance/k6/env/monitoring.env.example}" \
  docker compose -f docker-compose.monitoring.yml --profile loadtest run --rm --no-deps \
    --entrypoint k6 k6 run --quiet \
    -e K6_REPORT_NAME=summary-contract \
    -e K6_SCENARIO_NAME=summary-contract \
    -e RUN_ID=verify-summary \
    -e K6_ENV=contract-env \
    -e BASE_URL=https://contract.example.test \
    -e K6_GIT_COMMIT_SHA=0123456789abcdef \
    -e SEED_GIT_COMMIT_SHA=fedcba9876543210 \
    -e SEED_PROFILE=heavy \
    -e SEED_NAMESPACE=contract-heavy \
    /workspace/performance/k6/tests/summary-metadata.js >/dev/null

jq -e '
  .metadata == {
    "run_id": "verify-summary",
    "test_name": "summary-contract",
    "k6_env": "contract-env",
    "base_url": "https://contract.example.test",
    "git_commit_sha": "0123456789abcdef",
    "seed_git_commit_sha": "fedcba9876543210",
    "seed_profile": "heavy",
    "seed_namespace": "contract-heavy"
  }
' "$report" >/dev/null || {
  printf 'summary report is missing reproducibility metadata\n' >&2
  exit 1
}

printf 'verified summary reproducibility metadata\n'
