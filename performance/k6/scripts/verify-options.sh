#!/usr/bin/env bash
set -euo pipefail

server_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
cd "$server_dir"

expected_scenarios=(
  books-search-global.js
  books-search-library.js
  books-user.js
  cache-stats.js
  cleanup-seed.js
  mixed-read-journey.js
  onboarding.js
  prepare-seed.js
  single-api-read.js
  smoke.js
  timeline-core.js
  timeline-producers.js
  verify-seed.js
)

test_dir="$(mktemp -d)"
trap 'rm -rf "$test_dir"' EXIT
printf '%s\n' "${expected_scenarios[@]}" | sort > "$test_dir/expected-scenarios"
for scenario_path in performance/k6/scenarios/*.js; do
  basename "$scenario_path"
done | sort > "$test_dir/actual-scenarios"
if ! diff -u "$test_dir/expected-scenarios" "$test_dir/actual-scenarios"; then
  printf 'supported scenario list does not match the files on disk\n' >&2
  exit 1
fi

for scenario_file in "${expected_scenarios[@]}"; do
  output_file="$test_dir/${scenario_file%.js}.json"
  scenario_env=()
  if [[ "$scenario_file" == "single-api-read.js" ]]; then
    scenario_env=(
      -e K6_READ_TARGET=timeline-list
      -e K6_SINGLE_API_PROFILE=arrival
    )
  elif [[ "$scenario_file" == "cache-stats.js" ]]; then
    scenario_env=(
      -e K6_CACHE_TARGET=monthly
      -e K6_CACHE_PHASE=cold
    )
  fi
  K6_DOCKER_USER="$(id -u):$(id -g)" \
    ENV_FILE="${ENV_FILE:-performance/k6/env/monitoring.env.example}" \
    docker compose -f docker-compose.monitoring.yml --profile loadtest run --rm --no-deps \
      --entrypoint k6 k6 inspect --execution-requirements \
      "${scenario_env[@]}" \
      "/workspace/performance/k6/scenarios/$scenario_file" > "$output_file"
  jq -e '.thresholds | type == "object"' "$output_file" >/dev/null
  printf 'inspected %s\n' "$scenario_file"
done

options_json="$(<"$test_dir/mixed-read-journey.json")"

if ! jq -e '.thresholds.dropped_iterations == ["count<=0"]' <<<"$options_json" >/dev/null; then
  printf 'missing default dropped_iterations threshold\n' >&2
  exit 1
fi

request_names=()
while IFS= read -r request_name; do
  request_names+=("$request_name")
done < <(
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
