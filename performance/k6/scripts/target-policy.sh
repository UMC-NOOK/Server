#!/bin/sh

k6_target_die() {
  echo "error: $*" >&2
  exit 2
}

k6_matches() {
  k6_match_value="$1"
  k6_match_pattern="$2"
  [ -n "$k6_match_pattern" ] || return 1
  printf '%s\n' "$k6_match_value" | grep -Eq -- "$k6_match_pattern"
}

k6_normalize_base_url() {
  printf '%s' "${1:-}" | tr '[:upper:]' '[:lower:]'
}

k6_assert_http_url() {
  name="$1"
  url="$2"
  normalized_url="$(k6_normalize_base_url "$url")"
  case "$normalized_url" in
    http://* | https://*) ;;
    *) k6_target_die "$name must use http or https" ;;
  esac

  authority="${normalized_url#*://}"
  authority="${authority%%[/?#]*}"
  [ -n "$authority" ] || k6_target_die "$name must include a host"
  case "$authority" in
    *@*) k6_target_die "$name must not include userinfo" ;;
  esac
}

k6_is_local_base_url() {
  normalized_base_url="$(k6_normalize_base_url "$1")"
  k6_matches "$normalized_base_url" '^https?://(localhost|127\.0\.0\.1|host\.docker\.internal)(:[0-9]+)?(/|$)' && return 0
  k6_matches "$normalized_base_url" '^https?://\[::1\](:[0-9]+)?(/|$)'
}

k6_is_seed_script() {
  case "$1" in
    */prepare-seed.js | */verify-seed.js | */cleanup-seed.js) return 0 ;;
    *) return 1 ;;
  esac
}

k6_is_cache_script() {
  case "$1" in
    */cache-stats.js | performance/k6/scenarios/cache-stats.js) return 0 ;;
    *) return 1 ;;
  esac
}

k6_is_smoke_script() {
  case "$1" in
    */smoke.js | performance/k6/scenarios/smoke.js) return 0 ;;
    *) return 1 ;;
  esac
}

k6_is_production_environment() {
  k6_env="${K6_ENV:-${ENV:-local}}"
  if [ "$k6_env" = "prod" ]; then
    return 0
  fi
  return 1
}

k6_assert_url_allowed_for_environment() {
  name="$1"
  url="$2"
  pattern="$3"
  k6_env="${K6_ENV:-${ENV:-local}}"

  if k6_is_local_base_url "$url"; then
    [ "$k6_env" = "local" ] || k6_target_die "$name must match the $k6_env environment allowlist"
    return 0
  fi

  [ "$k6_env" != "local" ] || k6_target_die "$name must use a local host when K6_ENV=local"
  [ -n "$pattern" ] || k6_target_die "$name allowlist is required for K6_ENV=$k6_env"
  k6_matches "$url" "$pattern" || k6_target_die "$name must match the $k6_env environment allowlist"
}

k6_assert_target_allowed() {
  script="$1"
  k6_env="${K6_ENV:-${ENV:-local}}"
  base_url="${BASE_URL:-}"
  management_base_url="${MANAGEMENT_BASE_URL:-$base_url}"

  k6_assert_http_url "BASE_URL" "$base_url"
  k6_assert_http_url "MANAGEMENT_BASE_URL" "$management_base_url"
  k6_assert_url_allowed_for_environment "BASE_URL" "$base_url" "${K6_BASE_URL_PATTERN:-}"
  k6_assert_url_allowed_for_environment "MANAGEMENT_BASE_URL" "$management_base_url" "${K6_MANAGEMENT_BASE_URL_PATTERN:-${K6_BASE_URL_PATTERN:-}}"
  if k6_is_seed_script "$script" && { [ "$k6_env" != "local" ] || ! k6_is_local_base_url "$base_url"; }; then
    k6_target_die "seed is allowed only against the local k6 environment"
  fi
  if k6_is_cache_script "$script" && { [ "$k6_env" != "local" ] || ! k6_is_local_base_url "$base_url"; }; then
    k6_target_die "cache scenarios are allowed only against the local k6 environment"
  fi
  if k6_is_production_environment && ! k6_is_smoke_script "$script"; then
    k6_target_die "production targets allow only the smoke scenario"
  fi
  if [ "$k6_env" != "local" ] && [ "${CONFIRM_PROD_LOADTEST:-}" != "yes" ]; then
    k6_target_die "non-local load test requires CONFIRM_PROD_LOADTEST=yes"
  fi
}
