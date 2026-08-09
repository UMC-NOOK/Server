#!/bin/sh

k6_target_die() {
  echo "error: $*" >&2
  exit 2
}

k6_matches() {
  value="$1"
  pattern="$2"
  [ -n "$pattern" ] || return 1
  printf '%s\n' "$value" | grep -Eq "$pattern"
}

k6_normalize_base_url() {
  printf '%s' "${1:-}" | tr '[:upper:]' '[:lower:]'
}

k6_assert_http_target() {
  normalized_base_url="$(k6_normalize_base_url "${BASE_URL:-}")"
  case "$normalized_base_url" in
    http://* | https://*) ;;
    *) k6_target_die "BASE_URL must use http or https" ;;
  esac

  authority="${normalized_base_url#*://}"
  authority="${authority%%[/?#]*}"
  [ -n "$authority" ] || k6_target_die "BASE_URL must include a host"
  case "$authority" in
    *@*) k6_target_die "BASE_URL must not include userinfo" ;;
  esac
}

k6_is_local_base_url() {
  normalized_base_url="$(k6_normalize_base_url "$1")"
  k6_matches "$normalized_base_url" '^https?://(localhost|127\.0\.0\.1|host\.docker\.internal)(:[0-9]+)?(/|$)' && return 0
  k6_matches "$normalized_base_url" '^https?://\[::1\](:[0-9]+)?(/|$)'
}

k6_is_remote_http_url() {
  normalized_base_url="$(k6_normalize_base_url "$1")"
  k6_matches "$normalized_base_url" '^https?://'
}

k6_is_seed_script() {
  case "$1" in
    */prepare-seed.js | performance/k6/scenarios/prepare-seed.js) return 0 ;;
    *) return 1 ;;
  esac
}

k6_is_smoke_script() {
  case "$1" in
    */smoke.js | performance/k6/scenarios/smoke.js) return 0 ;;
    *) return 1 ;;
  esac
}

k6_is_production_target() {
  k6_env="${K6_ENV:-${ENV:-local}}"
  run_prefix="${RUN_PREFIX:-}"
  base_url="${BASE_URL:-}"
  normalized_base_url="$(k6_normalize_base_url "$base_url")"

  if [ "$k6_env" = "prod" ] || [ "$run_prefix" = "prod" ]; then
    return 0
  fi
  k6_matches "$base_url" "${K6_PROD_BASE_URL_PATTERN:-}" && return 0
  k6_matches "$normalized_base_url" '(^https?://api\.|[./-]prod[./:-]|production)'
}

k6_requires_confirmation() {
  k6_env="${K6_ENV:-${ENV:-local}}"
  run_prefix="${RUN_PREFIX:-}"
  base_url="${BASE_URL:-}"

  if [ "$k6_env" = "prod" ] || [ "$run_prefix" = "prod" ]; then
    return 0
  fi
  case "${K6_REQUIRE_CONFIRM:-}" in
    1 | true | TRUE | yes | YES | y | Y | on | ON) return 0 ;;
  esac
  if k6_is_remote_http_url "$base_url" && ! k6_is_local_base_url "$base_url"; then
    return 0
  fi
  k6_matches "$base_url" "${K6_PROD_BASE_URL_PATTERN:-}"
}

k6_assert_target_allowed() {
  script="$1"
  k6_env="${K6_ENV:-${ENV:-local}}"
  base_url="${BASE_URL:-}"

  k6_assert_http_target
  if k6_is_seed_script "$script" && { [ "$k6_env" != "local" ] || ! k6_is_local_base_url "$base_url"; }; then
    k6_target_die "seed is allowed only against the local k6 environment"
  fi
  if k6_is_production_target && ! k6_is_smoke_script "$script"; then
    k6_target_die "production targets allow only the smoke scenario"
  fi
  if k6_requires_confirmation && [ "${CONFIRM_PROD_LOADTEST:-}" != "yes" ]; then
    k6_target_die "non-local or production-like load test requires CONFIRM_PROD_LOADTEST=yes"
  fi
}
