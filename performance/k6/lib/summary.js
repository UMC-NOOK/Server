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
  const name = reportName(stringEnv("K6_REPORT_NAME", "summary"));
  const id = runId();
  const path = `performance/k6/reports/${name}-${id}.json`;
  const metadata = {
    run_id: id,
    test_name: name,
    k6_env: stringEnv("K6_ENV", "unknown"),
    base_url: stringEnv("BASE_URL", "unknown"),
    git_commit_sha: stringEnv("K6_GIT_COMMIT_SHA", "unknown"),
    seed_git_commit_sha: stringEnv("SEED_GIT_COMMIT_SHA"),
    seed_profile: stringEnv("SEED_PROFILE"),
    seed_namespace: stringEnv("SEED_NAMESPACE"),
  };
  const sanitizedData = sanitize({
    ...data,
    metadata,
  });

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
