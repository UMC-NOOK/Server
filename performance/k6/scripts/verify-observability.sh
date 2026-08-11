#!/usr/bin/env bash
set -euo pipefail

server_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
cd "$server_dir"

dashboard="grafana/dashboards/nook-api-observability.json"

fail() {
  printf 'observability verification failed: %s\n' "$*" >&2
  exit 1
}

monitoring_config="$({
  ENV_FILE="${ENV_FILE:-performance/k6/env/monitoring.env.example}" \
    K6_DOCKER_USER="$(id -u):$(id -g)" \
    docker compose -f docker-compose.monitoring.yml --profile loadtest config --format json
})"

jq -e '
  .services["redis-exporter"].image == "oliver006/redis_exporter:v1.84.0" and
  .services["redis-exporter"].environment.REDIS_EXPORTER_REDACT_CONFIG_METRICS == "true" and
  .services["postgres-exporter"].image == "prometheuscommunity/postgres-exporter:v0.19.1" and
  (.services["postgres-exporter"].command | index("--collector.long_running_transactions")) and
  (.services["postgres-exporter"].command | index("--collector.stat_statements")) and
  (.services["postgres-exporter"].command | index("--collector.stat_statements.limit=50"))
' <<<"$monitoring_config" >/dev/null || fail "exporter services or collectors are missing"

application_config="$(docker compose -f docker-compose.yml config --format json)"
jq -e '
  (.services.postgres.command | join(" ") | contains("shared_preload_libraries=pg_stat_statements")) and
  (.services.postgres.command | join(" ") | contains("compute_query_id=on")) and
  (.services.postgres.command | join(" ") | contains("pg_stat_statements.track=all")) and
  (.services.postgres.volumes | any(.target == "/docker-entrypoint-initdb.d/10-observability.sql"))
' <<<"$application_config" >/dev/null || fail "PostgreSQL observability bootstrap is missing"

grep -Fxq 'CREATE EXTENSION IF NOT EXISTS pg_stat_statements;' monitoring/postgres/init-observability.sql \
  || fail "pg_stat_statements extension bootstrap is missing"
rg -Uq 'server:\n  tomcat:\n    mbeanregistry:\n      enabled: true' src/main/resources/application.yml \
  || fail "Tomcat MBean metrics are not enabled"

docker run --rm \
  --user "$(id -u):$(id -g)" \
  -v "$server_dir/monitoring/prometheus:/etc/prometheus:ro" \
  --entrypoint /bin/promtool \
  prom/prometheus:v3.13.0 check config /etc/prometheus/prometheus.yml >/dev/null

jq -e '
  def panels: [.. | objects | select(has("id") and has("title") and has("type"))];
  def titles: panels | map(.title);
  .uid == "nook-api-observability" and
  (.description | contains("Run Summary and Subsystem Health")) and
  ([.templating.list[].name] | index("run_id")) and
  ([.templating.list[].name] | index("database")) and
  ((panels | map(.id) | length) == (panels | map(.id) | unique | length)) and
  ((panels | map(select(.type == "timeseries")) | length) == 18) and
  ((panels | map(select(.type == "stat")) | length) == 8) and
  ((panels | map(select(.type == "row")) | length) == 5) and
  (titles | index("Run Summary")) and
  (titles | index("k6 Latency")) and
  (titles | index("k6 Reliability")) and
  (titles | index("k6 Load Delivery")) and
  (titles | index("App Saturation")) and
  (titles | index("Hikari Pool")) and
  (titles | index("Redis Health")) and
  (titles | index("Postgres")) and
  (titles | index("Top API Offenders by p95")) and
  (titles | index("JVM GC Pause")) and
  (titles | index("Tomcat Threads")) and
  (titles | index("Redis Keyspace Hits, Misses, and Ratio")) and
  (titles | index("Redis Command Mean Latency")) and
  (titles | index("Redis Commands and Connections")) and
  (titles | index("Redis Errors")) and
  (titles | index("PostgreSQL Connections")) and
  (titles | index("PostgreSQL Locks and Transactions")) and
  (titles | index("PostgreSQL Statement Mean Time")) and
  ([.panels[] | select(.type == "row") | .title] == [
    "k6 Request Results",
    "Spring / JVM / Tomcat",
    "Hikari Connection Pool",
    "Redis",
    "PostgreSQL"
  ]) and
  (all(.panels[] | select(.type == "row"); .collapsed == true and (.panels | length) > 0))
' "$dashboard" >/dev/null || fail "dashboard summary, diagnostic rows, panels, or variables are incomplete"

dashboard_expressions="$(jq -r '[.. | objects | .targets[]? | .expr? // empty] | join("\n")' "$dashboard")"
for metric in \
  jvm_gc_pause_seconds_sum \
  tomcat_threads_busy_threads \
  redis_keyspace_hits_total \
  redis_keyspace_misses_total \
  redis_commands_duration_seconds_total \
  redis_commands_total \
  redis_connected_clients \
  redis_commands_failed_calls_total \
  redis_rejected_connections_total \
  pg_stat_database_numbackends \
  pg_locks_count \
  pg_stat_database_deadlocks \
  pg_stat_statements_seconds_total \
  pg_stat_statements_calls_total; do
  grep -Fq "$metric" <<<"$dashboard_expressions" \
    || fail "dashboard does not query $metric"
done

jq -e '
  def panel($title): [.. | objects | select(.title? == $title)][0];
  (panel("Redis Keyspace Hits, Misses, and Ratio") |
    (.targets[] | select(.refId == "C").expr | contains("clamp_min") | not) and
    (.fieldConfig.overrides[] | select(.matcher.options == "C") |
      any(.properties[]; .id == "unit" and .value == "percentunit"))) and
  (panel("Redis Commands and Connections") |
    (.fieldConfig.overrides[] | select(.matcher.options == "B") |
      any(.properties[]; .id == "unit" and .value == "short"))) and
  (panel("Redis Errors") |
    (.fieldConfig.overrides[] | select(.matcher.options == "C") |
      any(.properties[]; .id == "unit" and .value == "bool"))) and
  (panel("PostgreSQL Connections") |
    any(.targets[]; .expr | contains(">= 0")))
' "$dashboard" >/dev/null || fail "dashboard units or idle/unlimited semantics are invalid"

jq -e '
  def panel($title): [.. | objects | select(.title? == $title)][0];
  (panel("Run Summary") |
    .type == "text" and
    (.options.content | contains("${run_id:text}")) and
    (.options.content | contains("Red/orange = investigate")) and
    (.options.content | contains("blue/purple = neutral"))) and
  (panel("k6 Latency") |
    .type == "stat" and
    all(.targets[]; .instant == true and (.expr | contains("$__range"))) and
    all(.targets[]; (.expr | contains("by (run_id)") | not))) and
  (panel("k6 Reliability") |
    .type == "stat" and
    .options.colorMode == "background" and
    any(.targets[]; .expr | contains("max_over_time(k6_http_req_failed_rate")) and
    any(.targets[]; .expr | contains("min_over_time(k6_checks_rate"))) and
  (panel("k6 Load Delivery") |
    .type == "stat" and
    any(.targets[]; .expr | contains("k6_dropped_iterations_total")) and
    any(.targets[]; .expr | contains("or on() (0 * sum")) and
    .options.textMode == "value_and_name" and
    all(.targets[]; (.expr | contains("by (run_id)") | not))) and
  (panel("App Saturation") |
    any(.targets[]; .expr | contains("process_cpu_usage")) and
    any(.targets[]; .expr | contains("jvm_memory_used_bytes")) and
    any(.targets[]; .expr | contains("tomcat_threads_busy_threads"))) and
  (panel("Hikari Pool") |
    .options.colorMode == "background" and
    any(.targets[]; .expr | contains("hikaricp_connections_pending")) and
    any(.targets[]; .expr | contains("hikaricp_connections_timeout_total"))) and
  (panel("Redis Health") |
    any(.targets[]; .legendFormat == "hit ratio" and
      (.expr | contains("clamp_min") | not)) and
    any(.targets[]; .expr | contains("redis_commands_failed_calls_total")) and
    any(.targets[]; .expr | contains("redis_rejected_connections_total")) and
    any(.targets[]; .expr | contains("redis_up"))) and
  (panel("Postgres") |
    any(.targets[]; .expr | contains("pg_stat_database_numbackends")) and
    any(.targets[]; .expr | contains("pg_long_running_transactions")) and
    any(.targets[]; .expr | contains("pg_stat_database_deadlocks")) and
    any(.targets[];
      (.expr | contains("pg_stat_statements_seconds_total")) and
      .legendFormat == "slowest mean")) and
  (panel("Top API Offenders by p95") |
    .type == "table" and
    any(.targets[]; .format == "table" and .instant == true) and
    any(.transformations[];
      .id == "organize" and
      .options.excludeByName.error_code == true and
      .options.renameByName.status == "Status") and
    any(.fieldConfig.overrides[];
      .matcher.options == "Run ID" and
      any(.properties[]; .id == "custom.width" and .value >= 300)) and
    any(.fieldConfig.overrides[];
      .matcher.options == "Status" and
      any(.properties[]; .id == "unit" and .value == "none"))) and
  (panel("k6 Failure Rate and Checks") |
    (.gridPos.h >= 10) and
    (.targets[] | select(.legendFormat | startswith("checks")) |
      (.expr | contains("avg by (run_id, group)")) and
      (.legendFormat | contains("{{run_id}}")))) and
  (panel("k6 API Latency by Name") | .gridPos.h >= 10) and
  (panel("Redis Command Mean Latency") | .gridPos.h >= 16) and
  (panel("PostgreSQL Statement Mean Time") | .gridPos.h >= 16)
' "$dashboard" >/dev/null || fail "dashboard summary semantics, cold/warm identity, or diagnostic layout are invalid"

printf 'verified local exporters, Prometheus jobs, application metrics, dashboard summary, and diagnostic rows\n'
