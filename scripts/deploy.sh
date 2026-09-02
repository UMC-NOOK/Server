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

if [[ "$deploy_env" == "prod" ]]; then
  server_name="$(awk 'index($0, "SERVER_NAME=") == 1 { print substr($0, 13); exit }' "$env_file" | tr -d '\r')"

  if [[ ! "$server_name" =~ ^[A-Za-z0-9.-]+\.[A-Za-z]{2,}$ ]]; then
    echo "SERVER_NAME must be a valid hostname in $env_file" >&2
    exit 1
  fi

  certificate_dir="$(pwd)/data/certbot/conf/live/${server_name}"

  if ! sudo test -f "${certificate_dir}/fullchain.pem" || ! sudo test -f "${certificate_dir}/privkey.pem"; then
    echo "Missing TLS certificate for ${server_name}. Run ./scripts/init-certificate.sh prod first." >&2
    exit 1
  fi

  mkdir -p data/certbot/www
fi

export IMAGE="$image"
export ENV_FILE="$env_file"

docker compose --env-file "$env_file" -f "$compose_file" config --quiet
docker compose --env-file "$env_file" -f "$compose_file" pull

if [[ "$deploy_env" != "prod" ]]; then
  docker compose --env-file "$env_file" -f "$compose_file" up -d --remove-orphans
  app_service="app"
else
  active_conf="nginx/conf.d/active.conf"
  if grep -q 'server app-green:8080;' "$active_conf"; then
    current="green"
    target="blue"
  elif grep -q 'server app-blue:8080;' "$active_conf"; then
    current="blue"
    target="green"
  else
    echo "Cannot determine active production upstream from $active_conf" >&2
    exit 1
  fi

  # 첫 배포는 기본 upstream(blue)을 그대로 사용한다.
  if ! docker compose --env-file "$env_file" -f "$compose_file" ps -q "app-$current" | grep -q .; then
    target="$current"
    current=""
  fi

  echo "Production deployment: ${current:-none} -> $target"
  docker compose --env-file "$env_file" -f "$compose_file" up -d redis certbot
  docker compose --env-file "$env_file" -f "$compose_file" up -d --no-deps "app-$target"
  app_service="app-$target"
fi

app_container_id="$(docker compose --env-file "$env_file" -f "$compose_file" ps -q "$app_service")"

for attempt in $(seq 1 20); do
  health="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$app_container_id")"
  if [[ "$health" == "healthy" ]]; then
    break
  fi

  if [[ "$health" == "unhealthy" || "$health" == "exited" ]]; then
    break
  fi

  sleep 6
done

if [[ "${health:-}" != "healthy" ]]; then
  docker compose --env-file "$env_file" -f "$compose_file" ps
  docker compose --env-file "$env_file" -f "$compose_file" logs --tail=200 "$app_service"
  echo "${deploy_env} deployment failed health verification" >&2
  exit 1
fi

if [[ "$deploy_env" == "prod" ]]; then
  active_conf="nginx/conf.d/active.conf"
  temporary_conf="$(mktemp)"
  trap 'rm -f "$temporary_conf"' EXIT
  sed -E "s/server app-(blue|green):(8080|9091);/server app-$target:\2;/" "$active_conf" > "$temporary_conf"
  cat "$temporary_conf" > "$active_conf"

  # 기존 단일 app 컨테이너는 최초 blue/green 전환 때만 정리한다.
  if [[ -z "$current" ]]; then
    legacy_app_ids="$(docker ps -aq --filter 'label=com.docker.compose.service=app')"
    if [[ -n "$legacy_app_ids" ]]; then
      docker stop $legacy_app_ids
      docker rm $legacy_app_ids
    fi
  fi

  nginx_container_id="$(docker compose --env-file "$env_file" -f "$compose_file" ps -q nginx)"
  if [[ -z "$nginx_container_id" ]]; then
    docker compose --env-file "$env_file" -f "$compose_file" up -d --no-deps nginx
  else
    docker compose --env-file "$env_file" -f "$compose_file" exec -T nginx /bin/sh -c \
      "sed 's|\${SERVER_NAME}|${server_name}|g' /etc/nginx/templates/default.conf.template > /etc/nginx/conf.d/default.conf"
    docker compose --env-file "$env_file" -f "$compose_file" exec -T nginx nginx -t
    docker compose --env-file "$env_file" -f "$compose_file" exec -T nginx nginx -s reload
  fi

  docker compose --env-file "$env_file" -f "$compose_file" up -d --no-deps redis-exporter alloy
  if [[ -n "$current" ]]; then
    sleep 5
    docker compose --env-file "$env_file" -f "$compose_file" stop "app-$current"
  fi
fi

docker image prune -f
echo "${deploy_env} deployment succeeded: ${image}"
