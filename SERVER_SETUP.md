# Server and monitoring setup

## Compose files

- `docker-compose.yml`: local PostgreSQL and Redis
- `docker-compose.dev.yml`: dev application, Redis, nginx, and Alloy
- `docker-compose.prod.yml`: prod application, Redis, nginx, and Alloy
- `docker-compose.monitoring.yml`: Prometheus, Grafana, and Loki

Alloy runs on every application instance because Docker logs are local to that
host. It discovers local containers through the Docker socket and pushes their
logs to the central Loki instance.

## Spring profiles

- `application.yml`: configuration shared by every environment
- `application-local.yml`: reads the repository-root `.env` as a properties file
- `application-dev.yml`: receives variables from `/secrets/.env.dev` through Compose
- `application-prod.yml`: receives variables from `/secrets/.env.prod` through Compose
- `application-test.yml`: isolated test configuration

For local execution, copy `.env.example` to `.env`. Spring Boot reads this
extensionless file using the `[.properties]` extension hint, so no dotenv
library is required. Dev and prod do not read the host file directly; Docker
Compose injects its values as container environment variables.

## Application servers

Copy `.env.dev.example` to `/secrets/.env.dev` on the dev server and
`.env.prod.example` to `/secrets/.env.prod` on the prod server. Replace every placeholder.
The Loki URL must use the monitoring server's private IP or private DNS name.

```text
LOKI_URL=http://10.0.2.10:3100/loki/api/v1/push
```

Set the public hostname and the Let's Encrypt notification email in each server file.

```text
# /secrets/.env.dev
SERVER_NAME=dev.example.com
CERTBOT_EMAIL=admin@example.com

# /secrets/.env.prod
SERVER_NAME=api.example.com
CERTBOT_EMAIL=admin@example.com
```

Replace `example.com` with the actual domain. Create the DNS records before requesting
certificates.

| DNS record | Target |
|---|---|
| `dev.<domain>` A | dev EC2 Elastic IP |
| `api.<domain>` A | prod EC2 Elastic IP |

Only plain nginx is used; Nginx Proxy Manager is not part of the stack. HTTP port 80
serves the ACME challenge and redirects other requests to HTTPS. HTTPS port 443 proxies
to `app:8080` inside the Docker network.

After DNS propagation, issue the initial certificate once on each application server.
Port 80 must be free while this command runs.

```bash
chmod +x scripts/init-certificate.sh scripts/deploy.sh
./scripts/init-certificate.sh dev
```

For prod, run `./scripts/init-certificate.sh prod` only after the prod server and
`api.<domain>` DNS record exist. The Certbot container checks renewal every 12 hours,
and nginx reloads certificates periodically without recreating the application container.

You can verify renewal after nginx is running.

```bash
docker compose --env-file /secrets/.env.dev -f docker-compose.dev.yml \
  run --rm --entrypoint certbot certbot renew --dry-run \
  --webroot --webroot-path /var/www/certbot
```

Deploy an image without bringing the whole stack down first:

```bash
./scripts/deploy.sh dev <ECR_IMAGE_URI>
./scripts/deploy.sh prod <ECR_IMAGE_URI>
```

Prometheus scrapes the management-only port `9091`. The application port `8080`
is only exposed inside the Docker network. nginx is the public entry point and only
ports 80 and 443 are internet-facing.

## Monitoring server

1. Copy `.env.monitoring.example` to `/secrets/.env.monitoring` and set a strong Grafana password.
2. Replace `dev-private-ip` and `prod-private-ip` in `monitoring/prometheus/targets`.
3. Start the monitoring stack with `./scripts/deploy-monitoring.sh`.
4. Open Grafana on port `3000` from an administrator CIDR.

Prometheus is bound to loopback only. Loki port `3100` is reachable only from
application CIDRs through the AWS security group. Grafana provisions both data
sources automatically.

## Production hold

Production Terraform and CD are intentionally separated. Do not apply
`infra/envs/prod` until the production launch is approved. The prod workflow is
manual-only and should use a protected GitHub `prod` environment.

Both CD workflows use GitHub OIDC. Configure `AWS_ROLE_ARN` in the protected
GitHub environment and add the server host, username, private key, and deploy
path secrets prefixed with `DEV_` or `PROD_`. The EC2 instance role can pull
from ECR without storing AWS access keys on the server.

Create the secret directory with access restricted to the deployment user:

```bash
sudo install -d -m 700 -o ubuntu -g ubuntu /secrets
sudo chmod 600 /secrets/.env.dev
sudo chmod 600 /secrets/.env.prod
sudo chmod 600 /secrets/.env.monitoring
```

Each server only needs its own environment file. The Terraform bootstrap creates
`/secrets` automatically; the secret files themselves are never written by
Terraform or committed to Git.
