#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <dev|prod>" >&2
  exit 1
fi

deploy_env="$1"

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

read_env_value() {
  local key="$1"
  awk -v key="$key" 'index($0, key "=") == 1 { print substr($0, length(key) + 2); exit }' "$env_file" | tr -d '\r'
}

server_name="$(read_env_value SERVER_NAME)"
certbot_email="$(read_env_value CERTBOT_EMAIL)"

if [[ ! "$server_name" =~ ^[A-Za-z0-9.-]+\.[A-Za-z]{2,}$ ]]; then
  echo "SERVER_NAME must be a valid hostname in $env_file" >&2
  exit 1
fi

if [[ ! "$certbot_email" =~ ^[^[:space:]@]+@[^[:space:]@]+\.[^[:space:]@]+$ ]]; then
  echo "CERTBOT_EMAIL must be a valid email address in $env_file" >&2
  exit 1
fi

certificate_dir="$(pwd)/data/certbot/conf/live/${server_name}"

if [[ -f "${certificate_dir}/fullchain.pem" && -f "${certificate_dir}/privkey.pem" ]]; then
  echo "Certificate already exists for ${server_name}"
  exit 0
fi

mkdir -p data/certbot/conf data/certbot/www

export IMAGE="${IMAGE:-certificate-bootstrap}"
export ENV_FILE="$env_file"

docker compose --env-file "$env_file" -f "$compose_file" stop nginx >/dev/null 2>&1 || true

docker run --rm \
  --name "nook-certbot-${deploy_env}" \
  -p 80:80 \
  -v "$(pwd)/data/certbot/conf:/etc/letsencrypt" \
  -v "$(pwd)/data/certbot/www:/var/www/certbot" \
  certbot/certbot:v5.7.0 certonly \
  --standalone \
  --non-interactive \
  --agree-tos \
  --no-eff-email \
  --email "$certbot_email" \
  --domain "$server_name"

echo "Certificate issued for ${server_name}. Run scripts/deploy.sh to start HTTPS."
