#!/bin/sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
. "$script_dir/target-policy.sh"

actual_script="${K6_SCRIPT:-performance/k6/scenarios/smoke.js}"
for argument in "$@"; do
  case "$argument" in
    */scenarios/*.js) actual_script="$argument" ;;
  esac
done

k6_assert_target_allowed "$actual_script"
exec k6 "$@"
