#!/usr/bin/env bash
set -euo pipefail

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
migration_relative_path="src/main/resources/db/migration"
migration_directory="${repository_root}/${migration_relative_path}"
base_ref="${1:-}"

legacy_migrations=(
  "V1__init_schema.sql"
  "V2__add_unique_index_for_aladin_book_isbn.sql"
  "V3__cleanup_legacy_hibernate_schema.sql"
  "V4__create_book_view_history.sql"
  "V5__add_on_delete_cascade.sql"
  "V6__add_users_status_deleted_at_index.sql"
)

fail() {
  echo "Flyway migration validation failed: $1" >&2
  exit 1
}

is_legacy_migration() {
  local filename="$1"
  local legacy_migration

  for legacy_migration in "${legacy_migrations[@]}"; do
    if [[ "$filename" == "$legacy_migration" ]]; then
      return 0
    fi
  done

  return 1
}

is_valid_timestamp() {
  local date_part="$1"
  local time_part="$2"
  local year=$((10#${date_part:0:4}))
  local month=$((10#${date_part:4:2}))
  local day=$((10#${date_part:6:2}))
  local hour=$((10#${time_part:0:2}))
  local minute=$((10#${time_part:2:2}))
  local second=$((10#${time_part:4:2}))
  local max_day

  if (( year < 1970 || month < 1 || month > 12 || hour > 23 || minute > 59 || second > 59 )); then
    return 1
  fi

  case "$month" in
    1|3|5|7|8|10|12) max_day=31 ;;
    4|6|9|11) max_day=30 ;;
    2)
      max_day=28
      if (( year % 400 == 0 || (year % 4 == 0 && year % 100 != 0) )); then
        max_day=29
      fi
      ;;
  esac

  (( day >= 1 && day <= max_day ))
}

timestamp_version() {
  local filename="$1"

  if [[ "$filename" =~ ^V([0-9]{8})_([0-9]{6})__([a-z0-9]+(_[a-z0-9]+)*)\.sql$ ]]; then
    printf '%s%s\n' "${BASH_REMATCH[1]}" "${BASH_REMATCH[2]}"
    return 0
  fi

  return 1
}

validate_filename() {
  local filename="$1"
  local date_part
  local time_part

  if is_legacy_migration "$filename"; then
    return 0
  fi

  if [[ ! "$filename" =~ ^V([0-9]{8})_([0-9]{6})__([a-z0-9]+(_[a-z0-9]+)*)\.sql$ ]]; then
    fail "${filename} must match VyyyyMMdd_HHmmss__snake_case_description.sql"
  fi

  date_part="${BASH_REMATCH[1]}"
  time_part="${BASH_REMATCH[2]}"

  if ! is_valid_timestamp "$date_part" "$time_part"; then
    fail "${filename} contains an invalid UTC date or time"
  fi
}

validate_repository_files() {
  local migration_file
  local legacy_migration
  local filename
  local version
  local seen_versions=" "
  local migration_count=0

  [[ -d "$migration_directory" ]] || fail "missing migration directory: ${migration_relative_path}"

  for legacy_migration in "${legacy_migrations[@]}"; do
    [[ -f "${migration_directory}/${legacy_migration}" ]] \
      || fail "missing legacy migration: ${legacy_migration}"
  done

  shopt -s nullglob
  for migration_file in "$migration_directory"/*; do
    [[ -f "$migration_file" ]] || continue
    filename="$(basename "$migration_file")"
    validate_filename "$filename"

    if is_legacy_migration "$filename"; then
      version="${filename%%__*}"
      version="${version#V}"
    else
      version="$(timestamp_version "$filename")"
    fi

    if [[ "$seen_versions" == *" ${version} "* ]]; then
      fail "duplicate migration version: ${version}"
    fi

    seen_versions+="${version} "
    ((migration_count += 1))
  done
  shopt -u nullglob

  (( migration_count > 0 )) || fail "no migration files found"
  printf '%s\n' "$migration_count"
}

base_max_version() {
  local ref="$1"
  local path
  local filename
  local version
  local max_version=0

  while IFS= read -r path; do
    [[ -n "$path" ]] || continue
    filename="$(basename "$path")"

    if is_legacy_migration "$filename"; then
      version="${filename%%__*}"
      version="${version#V}"
    elif version="$(timestamp_version "$filename")"; then
      :
    else
      fail "base ref ${ref} contains an unsupported migration filename: ${filename}"
    fi

    if (( 10#$version > 10#$max_version )); then
      max_version="$version"
    fi
  done < <(git -C "$repository_root" ls-tree -r --name-only "$ref" -- "$migration_relative_path")

  printf '%s\n' "$max_version"
}

validate_changes_from_base() {
  local ref="$1"
  local path
  local filename
  local version
  local max_version
  local added_count=0

  git -C "$repository_root" rev-parse --verify --quiet "${ref}^{commit}" >/dev/null \
    || fail "base ref does not resolve to a commit: ${ref}"

  max_version="$(base_max_version "$ref")"

  while IFS= read -r path; do
    [[ -n "$path" ]] || continue
    filename="$(basename "$path")"
    [[ -f "${repository_root}/${path}" ]] \
      || fail "existing migration files are immutable (deleted: ${path})"

    if ! git -C "$repository_root" show "${ref}:${path}" | cmp -s - "${repository_root}/${path}"; then
      fail "existing migration files are immutable (modified: ${path})"
    fi
  done < <(git -C "$repository_root" ls-tree -r --name-only "$ref" -- "$migration_relative_path")

  shopt -s nullglob
  for path in "$migration_directory"/*; do
    [[ -f "$path" ]] || continue
    filename="$(basename "$path")"

    if git -C "$repository_root" cat-file -e "${ref}:${migration_relative_path}/${filename}" 2>/dev/null; then
      continue
    fi

    if is_legacy_migration "$filename"; then
      fail "legacy migration cannot be added again: ${filename}"
    fi

    version="$(timestamp_version "$filename")"
    if (( 10#$version <= 10#$max_version )); then
      fail "${filename} must have a version greater than the base maximum ${max_version}"
    fi

    ((added_count += 1))
  done
  shopt -u nullglob

  printf '%s\n' "$added_count"
}

migration_count="$(validate_repository_files)"

if [[ -n "$base_ref" ]]; then
  added_count="$(validate_changes_from_base "$base_ref")"
  echo "Validated ${migration_count} Flyway migrations and ${added_count} migration changes against ${base_ref}."
else
  echo "Validated ${migration_count} Flyway migrations. Pass a base ref to validate changed migrations."
fi
