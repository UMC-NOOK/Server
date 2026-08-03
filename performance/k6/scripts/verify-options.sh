#!/usr/bin/env bash
set -euo pipefail

server_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
cd "$server_dir"

options_json="$({
  K6_DOCKER_USER="$(id -u):$(id -g)" \
    ENV_FILE="${ENV_FILE:-performance/k6/env/monitoring.env}" \
    docker compose -f docker-compose.monitoring.yml --profile loadtest run --rm --no-deps \
      --entrypoint k6 k6 inspect --execution-requirements \
      /workspace/performance/k6/scenarios/mixed-read-journey.js
})"

if ! jq -e '.thresholds.dropped_iterations == ["count<=0"]' <<<"$options_json" >/dev/null; then
  printf 'missing default dropped_iterations threshold\n' >&2
  exit 1
fi

mapfile -t request_names < <(
  sed -nE 's/.*tags: \{ name: "(read:[^"]+)" \}.*/\1/p' \
    performance/k6/scenarios/mixed-read-journey.js | sort -u
)

if [[ "${#request_names[@]}" -ne 18 ]]; then
  printf 'expected 18 mixed-read request tags, found %s\n' "${#request_names[@]}" >&2
  exit 1
fi

for request_name in "${request_names[@]}"; do
  duration_key="http_req_duration{name:${request_name}}"
  failure_key="http_req_failed{name:${request_name}}"

  if ! jq -e --arg key "$duration_key" '.thresholds[$key] == ["p(95)<1000"]' <<<"$options_json" >/dev/null; then
    printf 'missing default p95 threshold for %s\n' "$request_name" >&2
    exit 1
  fi

  if ! jq -e --arg key "$failure_key" '.thresholds[$key] == ["rate<0.01"]' <<<"$options_json" >/dev/null; then
    printf 'missing default failure-rate threshold for %s\n' "$request_name" >&2
    exit 1
  fi
done

printf 'verified mixed-read thresholds for %s request tags\n' "${#request_names[@]}"
