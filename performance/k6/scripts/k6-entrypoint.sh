#!/bin/sh
set -eu

die() {
  echo "error: $*" >&2
  exit 2
}

is_yes() {
  case "$1" in
    1 | true | TRUE | yes | YES | y | Y | on | ON)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

matches() {
  value="$1"
  pattern="$2"

  [ -n "$pattern" ] || return 1
  printf '%s\n' "$value" | grep -Eq "$pattern"
}

normalize_base_url() {
  printf '%s' "${1:-}" | tr '[:upper:]' '[:lower:]'
}

is_local_base_url() {
  base_url="$(normalize_base_url "$1")"

  matches "$base_url" '^https?://(localhost|127\.0\.0\.1|host\.docker\.internal)(:[0-9]+)?(/|$)' && return 0
  matches "$base_url" '^https?://\[::1\](:[0-9]+)?(/|$)'
}

is_remote_http_url() {
  base_url="$(normalize_base_url "$1")"
  matches "$base_url" '^https?://'
}

requires_confirmation() {
  k6_env="${K6_ENV:-${ENV:-local}}"
  run_prefix="${RUN_PREFIX:-}"
  base_url="${BASE_URL:-}"
  normalized_base_url="$(normalize_base_url "$base_url")"

  if [ "$k6_env" = "prod" ] || [ "$run_prefix" = "prod" ] || is_yes "${K6_REQUIRE_CONFIRM:-}"; then
    return 0
  fi

  if is_remote_http_url "$base_url" && ! is_local_base_url "$base_url"; then
    return 0
  fi

  if matches "$base_url" "${K6_PROD_BASE_URL_PATTERN:-}"; then
    return 0
  fi

  matches "$normalized_base_url" '(^https?://api\.|[./-]prod[./:-]|production)'
}

if requires_confirmation && [ "${CONFIRM_PROD_LOADTEST:-}" != "yes" ]; then
  die "non-local or production-like load test requires CONFIRM_PROD_LOADTEST=yes"
fi

exec k6 "$@"
