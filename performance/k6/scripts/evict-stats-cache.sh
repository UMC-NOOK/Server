#!/usr/bin/env bash
set -euo pipefail

die() {
  printf 'error: %s\n' "$*" >&2
  exit 2
}

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
server_dir="$(cd "$script_dir/../../.." && pwd)"
cd "$server_dir"
source "$script_dir/target-policy.sh"

[[ "${K6_CACHE_TARGET:-}" == "monthly" || "${K6_CACHE_TARGET:-}" == "focus-monthly" ]] \
  || die "K6_CACHE_TARGET must be monthly or focus-monthly"
[[ "${K6_STATS_YEAR_MONTH:-}" =~ ^[0-9]{4}-(0[1-9]|1[0-2])$ ]] \
  || die "K6_STATS_YEAR_MONTH must use yyyy-MM"
[[ -n "${K6_USER_EMAIL:-}" && -n "${K6_USER_NICKNAME:-}" ]] \
  || die "K6_USER_EMAIL and K6_USER_NICKNAME must come from a reusable seed manifest"

k6_assert_http_url "BASE_URL" "${BASE_URL:-}"
k6_is_local_base_url "${BASE_URL:-}" \
  || die "automatic cache eviction is allowed only for a local BASE_URL"

host_base_url="${BASE_URL/host.docker.internal/localhost}"
host_base_url="${host_base_url%/}"
login_payload="$(jq -nc \
  --arg email "$K6_USER_EMAIL" \
  --arg nickName "$K6_USER_NICKNAME" \
  '{email: $email, nickName: $nickName}')"
login_response="$(curl --fail --silent --show-error \
  --connect-timeout 5 \
  --max-time 30 \
  -H 'Content-Type: application/json' \
  --data "$login_payload" \
  "$host_base_url/api/v1/auth/dev/login")" \
  || die "DEV login failed while resolving the cache user"

user_id="$(jq -er '.result.id | select(type == "number" and floor == . and . > 0)' <<<"$login_response")" \
  || die "DEV login response did not contain a positive integer user id"
response_email="$(jq -er '.result.email | select(type == "string")' <<<"$login_response")" \
  || die "DEV login response did not contain the user email"
[[ "$response_email" == "$K6_USER_EMAIL" ]] \
  || die "DEV login identity does not match the reusable seed manifest"

if [[ "$K6_CACHE_TARGET" == "monthly" ]]; then
  keys=(
    "stats:library:monthly:zset:${user_id}:${K6_STATS_YEAR_MONTH}"
    "stats:library:monthly:total:${user_id}:${K6_STATS_YEAR_MONTH}"
    "stats:library:monthly:exists:${user_id}:${K6_STATS_YEAR_MONTH}"
  )
else
  compact_year_month="${K6_STATS_YEAR_MONTH//-/}"
  keys=(
    "stats:focus:daily:${user_id}:${compact_year_month}"
    "stats:focus:daily:exists:${user_id}:${compact_year_month}"
  )
fi

existing_count="$(docker compose exec -T redis redis-cli --raw EXISTS "${keys[@]}")" \
  || die "Redis cache existence check failed"
[[ "$existing_count" =~ ^[0-9]+$ && "$existing_count" -le "${#keys[@]}" ]] \
  || die "Redis cache existence check returned an invalid count"

deleted_count="$(docker compose exec -T redis redis-cli --raw DEL "${keys[@]}")" \
  || die "Redis cache eviction failed"
[[ "$deleted_count" =~ ^[0-9]+$ && "$deleted_count" -le "${#keys[@]}" ]] \
  || die "Redis cache eviction returned an invalid delete count"
[[ "$deleted_count" == "$existing_count" ]] \
  || die "Redis cache eviction did not delete the expected existing keys"

remaining_count="$(docker compose exec -T redis redis-cli --raw EXISTS "${keys[@]}")" \
  || die "Redis cache post-eviction check failed"
[[ "$remaining_count" == "0" ]] \
  || die "Redis cache eviction left target keys behind"

printf 'cache_user_id=%s\ncache_keys_targeted=%s\ncache_keys_deleted=%s\ncache_keys_remaining=%s\n' \
  "$user_id" "${#keys[@]}" "$deleted_count" "$remaining_count"
