#!/usr/bin/env bash
set -euo pipefail

secret_dir="${SECRET_DIR:-/secrets}"
env_file="${secret_dir}/.env.monitoring"
compose_file="docker-compose.monitoring.yml"

if [[ ! -f "$env_file" ]]; then
  echo "Missing server secret file: $env_file" >&2
  exit 1
fi

export ENV_FILE="$env_file"

docker compose --env-file "$env_file" -f "$compose_file" pull
docker compose --env-file "$env_file" -f "$compose_file" up -d --remove-orphans
docker compose --env-file "$env_file" -f "$compose_file" ps
