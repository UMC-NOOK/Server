#!/usr/bin/env bash

seed_manifest_path() {
  printf '%s/seeds/seed-%s-%s.env' "$K6_STATE_DIR" "$K6_ENV" "$SEED_NAMESPACE"
}

seed_manifest_value() {
  local key="$1"
  sed -n "s/^${key}=//p" "$K6_SEED_MANIFEST_FILE" | tail -n 1
}

apply_seed_profile_defaults() {
  SEED_PROFILE="$1"
  case "$SEED_PROFILE" in
    light)
      SEED_BOOKS=5
      SEED_RECORDS_PER_BOOK=2
      SEED_FOCUS_SESSIONS=2
      ;;
    normal)
      SEED_BOOKS=30
      SEED_RECORDS_PER_BOOK=3
      SEED_FOCUS_SESSIONS=10
      ;;
    heavy)
      SEED_BOOKS=300
      SEED_RECORDS_PER_BOOK=10
      SEED_FOCUS_SESSIONS=100
      ;;
    *) die "unknown seed profile '$SEED_PROFILE' (expected light, normal, or heavy)" ;;
  esac
  SEED_NAMESPACE="${SEED_NAMESPACE:-${K6_ENV}-${SEED_PROFILE}}"
}

validate_seed_namespace() {
  [[ "$SEED_NAMESPACE" =~ ^[A-Za-z0-9][A-Za-z0-9_-]*$ ]] \
    || die "SEED_NAMESPACE must start with a letter or number and contain only letters, numbers, underscores, or hyphens"
  (( ${#SEED_NAMESPACE} <= 48 )) || die "SEED_NAMESPACE must be at most 48 characters"
}

validate_seed_counts() {
  [[ "$SEED_BOOKS" =~ ^[1-9][0-9]*$ ]] || die "SEED_BOOKS must be a positive integer"
  [[ "$SEED_RECORDS_PER_BOOK" =~ ^[1-9][0-9]*$ ]] || die "SEED_RECORDS_PER_BOOK must be a positive integer"
  [[ "$SEED_FOCUS_SESSIONS" =~ ^[1-9][0-9]*$ ]] || die "SEED_FOCUS_SESSIONS must be a positive integer"
}

set_seed_identity() {
  local nickname
  nickname="$(printf 'seed%s' "$SEED_NAMESPACE" | tr -cd '[:alnum:]가-힣' | cut -c1-20)"
  K6_USER_EMAIL="seed-${SEED_NAMESPACE}@test.com"
  K6_USER_NICKNAME="$nickname"
}

validate_seed_identity() {
  [[ "$K6_USER_EMAIL" == "seed-${SEED_NAMESPACE}@test.com" ]] \
    || die "seed lifecycle only accepts the namespace-generated test user"
}

load_seed_manifest() {
  local manifest_namespace manifest_profile
  manifest_namespace="$(seed_manifest_value SEED_NAMESPACE)"
  manifest_profile="$(seed_manifest_value SEED_PROFILE)"

  [[ "$manifest_namespace" == "$SEED_NAMESPACE" ]] || die "seed manifest namespace does not match SEED_NAMESPACE"
  [[ -n "$manifest_profile" ]] || die "seed manifest is missing SEED_PROFILE"

  SEED_PROFILE="$manifest_profile"
  SEED_RUN_ID="$(seed_manifest_value SEED_RUN_ID)"
  K6_USER_EMAIL="$(seed_manifest_value K6_USER_EMAIL)"
  K6_USER_NICKNAME="$(seed_manifest_value K6_USER_NICKNAME)"
  SEED_BOOKS="$(seed_manifest_value SEED_BOOKS)"
  SEED_RECORDS_PER_BOOK="$(seed_manifest_value SEED_RECORDS_PER_BOOK)"
  SEED_FOCUS_SESSIONS="$(seed_manifest_value SEED_FOCUS_SESSIONS)"
  SEED_GIT_COMMIT_SHA="$(seed_manifest_value K6_GIT_COMMIT_SHA)"

  [[ -n "$SEED_RUN_ID" ]] || die "seed manifest is missing SEED_RUN_ID"
  [[ -n "$K6_USER_NICKNAME" ]] || die "seed manifest is missing K6_USER_NICKNAME"
  [[ -n "$SEED_GIT_COMMIT_SHA" ]] || die "seed manifest is missing K6_GIT_COMMIT_SHA"
  validate_seed_identity
  validate_seed_counts
}

prepare_seed_create_or_reuse() {
  local requested_profile="$1"
  local requested_books="$SEED_BOOKS"
  local requested_records_per_book="$SEED_RECORDS_PER_BOOK"
  local requested_focus_sessions="$SEED_FOCUS_SESSIONS"

  [[ -z "${TOKEN:-}" && -z "${K6_ACCESS_TOKEN:-}" ]] || die "seed does not accept TOKEN or K6_ACCESS_TOKEN"
  validate_seed_namespace
  validate_seed_counts
  K6_SEED_MANIFEST_FILE="$(seed_manifest_path)"

  if [[ -f "$K6_SEED_MANIFEST_FILE" ]]; then
    load_seed_manifest
    [[ "$SEED_PROFILE" == "$requested_profile" ]] || die "seed namespace '$SEED_NAMESPACE' already uses profile '$SEED_PROFILE'"
    [[ "$SEED_BOOKS" == "$requested_books" ]] || die "seed namespace '$SEED_NAMESPACE' already uses SEED_BOOKS=$SEED_BOOKS"
    [[ "$SEED_RECORDS_PER_BOOK" == "$requested_records_per_book" ]] \
      || die "seed namespace '$SEED_NAMESPACE' already uses SEED_RECORDS_PER_BOOK=$SEED_RECORDS_PER_BOOK"
    [[ "$SEED_FOCUS_SESSIONS" == "$requested_focus_sessions" ]] \
      || die "seed namespace '$SEED_NAMESPACE' already uses SEED_FOCUS_SESSIONS=$SEED_FOCUS_SESSIONS"
    SEED_MODE=reuse
    return
  fi

  SEED_MODE=create
  SEED_GIT_COMMIT_SHA="$K6_GIT_COMMIT_SHA"
  set_seed_identity
}

prepare_seed_cleanup() {
  [[ -z "${TOKEN:-}" && -z "${K6_ACCESS_TOKEN:-}" ]] || die "cleanup-seed does not accept TOKEN or K6_ACCESS_TOKEN"
  SEED_NAMESPACE="${SEED_NAMESPACE:-${K6_ENV}-${SEED_PROFILE:-normal}}"
  validate_seed_namespace
  K6_SEED_MANIFEST_FILE="$(seed_manifest_path)"

  if [[ -f "$K6_SEED_MANIFEST_FILE" ]]; then
    load_seed_manifest
  else
    SEED_PROFILE="${SEED_PROFILE:-unknown}"
    SEED_GIT_COMMIT_SHA="${SEED_GIT_COMMIT_SHA:-}"
    set_seed_identity
  fi
  validate_seed_identity
  SEED_MODE=cleanup
}

set_legacy_seed_identity() {
  K6_USER_EMAIL="seed-${SEED_RUN_ID}-0-0@test.com"
  K6_USER_NICKNAME="${K6_USER_NICKNAME:-k6read}"
}

prepare_mixed_seed_identity() {
  [[ -z "${K6_ACCESS_TOKEN:-}" && -z "${TOKEN:-}" && -z "${K6_USER_EMAIL:-}" ]] || return 0

  if [[ -n "${SEED_NAMESPACE:-}" ]]; then
    validate_seed_namespace
    K6_SEED_MANIFEST_FILE="$(seed_manifest_path)"
    [[ -f "$K6_SEED_MANIFEST_FILE" ]] || die "seed manifest not found: $K6_SEED_MANIFEST_FILE"
    load_seed_manifest
    return
  fi

  if [[ -n "${SEED_RUN_ID:-}" ]]; then
    set_legacy_seed_identity
    return
  fi

  if [[ -f "$K6_SEED_STATE_FILE" ]]; then
    local seed_reference candidate_manifest
    seed_reference="$(<"$K6_SEED_STATE_FILE")"
    candidate_manifest="${K6_STATE_DIR}/seeds/seed-${K6_ENV}-${seed_reference}.env"
    if [[ -f "$candidate_manifest" ]]; then
      SEED_NAMESPACE="$seed_reference"
      K6_SEED_MANIFEST_FILE="$candidate_manifest"
      load_seed_manifest
      return
    fi

    SEED_RUN_ID="$seed_reference"
    set_legacy_seed_identity
  fi
}

prepare_cache_seed_identity() {
  [[ -z "${TOKEN:-}" && -z "${K6_ACCESS_TOKEN:-}" && -z "${K6_USER_EMAIL:-}" && -z "${K6_USER_NICKNAME:-}" ]] \
    || die "cache scenarios require an unmodified reusable seed manifest and do not accept configured credentials"

  prepare_mixed_seed_identity

  [[ -n "${SEED_NAMESPACE:-}" && -n "${K6_SEED_MANIFEST_FILE:-}" && -f "$K6_SEED_MANIFEST_FILE" ]] \
    || die "cache scenarios require a reusable namespace seed manifest; run seed first"
  validate_seed_identity
}

mark_seed_manifest_latest() {
  mkdir -p "$K6_STATE_DIR"
  printf '%s\n' "$SEED_NAMESPACE" > "$K6_SEED_STATE_FILE"
}

save_seed_manifest() {
  local manifest_dir temporary_manifest
  manifest_dir="$(dirname "$K6_SEED_MANIFEST_FILE")"
  temporary_manifest="${K6_SEED_MANIFEST_FILE}.tmp.$$"
  mkdir -p "$manifest_dir"

  umask 077
  printf '%s\n' \
    "SEED_NAMESPACE=$SEED_NAMESPACE" \
    "SEED_PROFILE=$SEED_PROFILE" \
    "SEED_RUN_ID=$RUN_ID" \
    "K6_USER_EMAIL=$K6_USER_EMAIL" \
    "K6_USER_NICKNAME=$K6_USER_NICKNAME" \
    "SEED_BOOKS=$SEED_BOOKS" \
    "SEED_RECORDS_PER_BOOK=$SEED_RECORDS_PER_BOOK" \
    "SEED_FOCUS_SESSIONS=$SEED_FOCUS_SESSIONS" \
    "K6_GIT_COMMIT_SHA=$K6_GIT_COMMIT_SHA" > "$temporary_manifest"
  mv "$temporary_manifest" "$K6_SEED_MANIFEST_FILE"
  mark_seed_manifest_latest
  printf 'saved_seed_manifest=%s\n' "$K6_SEED_MANIFEST_FILE"
}

remove_seed_manifest() {
  rm -f "$K6_SEED_MANIFEST_FILE"
  if [[ -f "$K6_SEED_STATE_FILE" && "$(<"$K6_SEED_STATE_FILE")" == "$SEED_NAMESPACE" ]]; then
    rm -f "$K6_SEED_STATE_FILE"
  fi
  printf 'removed_seed_manifest=%s\n' "$K6_SEED_MANIFEST_FILE"
}
