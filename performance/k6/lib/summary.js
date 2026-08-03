import { reportName, runId, stringEnv } from "./env.js";

const SENSITIVE_KEY_PATTERN = /(authorization|accessToken|refreshToken|token|secret|password)/i;

function sanitize(value) {
  if (Array.isArray(value)) {
    return value.map(sanitize);
  }

  if (value && typeof value === "object") {
    return Object.entries(value).reduce((acc, [key, item]) => {
      acc[key] = SENSITIVE_KEY_PATTERN.test(key) ? "[REDACTED]" : sanitize(item);
      return acc;
    }, {});
  }

  return value;
}

export function createSummary(data) {
  const name = reportName(stringEnv("K6_SCENARIO_NAME", "summary"));
  const path = `performance/k6/reports/${name}-${runId()}.json`;
  const sanitizedData = sanitize(data);

  return {
    [path]: JSON.stringify(sanitizedData, null, 2),
    stdout: JSON.stringify(
      {
        checks: data.metrics.checks,
        dropped_iterations: data.metrics.dropped_iterations,
        http_req_failed: data.metrics.http_req_failed,
        http_req_duration: data.metrics.http_req_duration,
      },
      null,
      2
    ),
  };
}
