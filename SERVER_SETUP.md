# Server and monitoring setup

## Compose files

- `docker-compose.yml`: local PostgreSQL and Redis
- `docker-compose.dev.yml`: dev application, Redis, and optional Alloy; host nginx handles HTTPS
- `docker-compose.prod.yml`: prod application, Redis, nginx, and Alloy
- `docker-compose.monitoring.yml`: Prometheus, Grafana, and Loki

Alloy runs on every application instance after the monitoring profile is enabled because
Docker logs are local to that host. It discovers local containers through the Docker
socket and pushes their logs to the central Loki instance.

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
Dev does not need domain, certificate, or monitoring variables in this file initially.

### Dev host nginx

Dev uses the nginx already installed on the EC2 host. Docker publishes the application
only on `127.0.0.1:8080`, so port 8080 is not reachable directly from the internet.
Create or update the host nginx site with the actual dev domain.

```nginx
server {
    listen 80;
    server_name dev.example.com;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Validate and reload the existing host nginx, then issue the certificate on the host.

```bash
sudo nginx -t
sudo systemctl reload nginx
sudo certbot --nginx -d dev.example.com
sudo certbot renew --dry-run
```

Replace `dev.example.com` only in the server-side nginx file. The dev Compose and GitHub
CD workflow do not copy or overwrite the host nginx configuration.

### Prod Docker nginx

Prod keeps nginx and Certbot in Docker. Set the public hostname and Let's Encrypt
notification email only in the prod server file.

```text
# /secrets/.env.prod
SERVER_NAME=api.example.com
CERTBOT_EMAIL=admin@example.com
```

Create the DNS records before requesting certificates.

| DNS record | Target |
|---|---|
| `dev.<domain>` A | dev EC2 Elastic IP |
| `api.<domain>` A | prod EC2 Elastic IP |

For prod, run the certificate script after the prod server and DNS record exist.

```bash
./scripts/init-certificate.sh prod
```

The prod Certbot container checks renewal every 12 hours, and nginx reloads certificates
periodically without recreating the application container.

### Optional dev log agent

When the monitoring server is ready, add its private Loki URL to `/secrets/.env.dev`.

```text
LOKI_URL=http://10.0.2.10:3100/loki/api/v1/push
```

Enable the optional Alloy profile during a later deployment with
`COMPOSE_PROFILES=monitoring`. Without this profile, dev starts only the app and Redis.

```bash
COMPOSE_PROFILES=monitoring ./scripts/deploy.sh dev <ECR_IMAGE_URI>
```

Deploy an image without bringing the whole stack down first:

```bash
./scripts/deploy.sh dev <ECR_IMAGE_URI>
./scripts/deploy.sh prod <ECR_IMAGE_URI>
```

Prometheus scrapes the management-only port `9091`. Until monitoring is connected, dev
binds both `8080` and `9091` only to host loopback. When monitoring is ready, set
`MANAGEMENT_BIND_ADDRESS` to the dev private IP and allow 9091 only from the monitoring
security group. The host nginx is the public entry point. On prod, the application port
is exposed only inside the Docker network and Docker nginx publishes ports 80 and 443.

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

Dev CD keeps the existing GitHub secrets: `AWS_ACCESS_KEY_ID`,
`AWS_SECRET_ACCESS_KEY`, `AWS_ACCOUNT_ID`, `DEV_HOST`, `DEV_USERNAME`, and
`DEV_PRIVATE_KEY`. Deployment files are copied to the SSH user's home directory. The
workflow never creates or overwrites `/secrets/.env.dev`; the server-managed file is
preserved across deployments.
Prod CD remains OIDC-based and is not enabled until production infrastructure is ready.
The EC2 server must be able to run `aws ecr get-login-password` to pull from ECR.

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
