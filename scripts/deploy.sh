#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "Usage: $0 <dev|prod> <image>" >&2
  exit 1
fi

deploy_env="$1"
image="$2"

case "$deploy_env" in
  dev|prod) ;;
  *)
    echo "Unsupported environment: $deploy_env" >&2
    exit 1
    ;;
esac

compose_file="docker-compose.${deploy_env}.yml"
secret_dir="${SECRET_DIR:-/secrets}"
env_file="${secret_dir}/.env.${deploy_env}"

if [[ ! -f "$compose_file" ]]; then
  echo "Missing compose file: $compose_file" >&2
  exit 1
fi

if [[ ! -f "$env_file" ]]; then
  echo "Missing server secret file: $env_file" >&2
  exit 1
fi

server_name="$(awk 'index($0, "SERVER_NAME=") == 1 { print substr($0, 13); exit }' "$env_file" | tr -d '\r')"

if [[ ! "$server_name" =~ ^[A-Za-z0-9.-]+\.[A-Za-z]{2,}$ ]]; then
  echo "SERVER_NAME must be a valid hostname in $env_file" >&2
  exit 1
fi

certificate_dir="$(pwd)/data/certbot/conf/live/${server_name}"

if [[ ! -f "${certificate_dir}/fullchain.pem" || ! -f "${certificate_dir}/privkey.pem" ]]; then
  echo "Missing TLS certificate for ${server_name}. Run ./scripts/init-certificate.sh ${deploy_env} first." >&2
  exit 1
fi

mkdir -p data/certbot/www

export IMAGE="$image"
export ENV_FILE="$env_file"

docker compose --env-file "$env_file" -f "$compose_file" config --quiet
docker compose --env-file "$env_file" -f "$compose_file" pull
docker compose --env-file "$env_file" -f "$compose_file" up -d --remove-orphans

app_container_id="$(docker compose --env-file "$env_file" -f "$compose_file" ps -q app)"

for attempt in $(seq 1 20); do
  health="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$app_container_id")"
  if [[ "$health" == "healthy" ]]; then
    docker image prune -f
    echo "${deploy_env} deployment succeeded: ${image}"
    exit 0
  fi

  if [[ "$health" == "unhealthy" || "$health" == "exited" ]]; then
    break
  fi

  sleep 6
done

docker compose --env-file "$env_file" -f "$compose_file" ps
docker compose --env-file "$env_file" -f "$compose_file" logs --tail=200 app
echo "${deploy_env} deployment failed health verification" >&2
exit 1
