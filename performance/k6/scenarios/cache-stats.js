import { cacheColdOptions, cacheWarmOptions } from "../config/profiles.js";
import { authenticateExistingUser } from "../lib/auth.js";
import { requireApiResponse } from "../lib/checks.js";
import { seedUser } from "../lib/data.js";
import { stringEnv } from "../lib/env.js";
import { authHeaders, get, withQuery } from "../lib/http.js";
import { createSummary } from "../lib/summary.js";

const TARGET_PATHS = {
  monthly: "/api/v1/library/stats/monthly",
  "focus-monthly": "/api/v1/library/stats/focus-monthly",
};

function currentYearMonth() {
  return new Date().toISOString().slice(0, 7);
}

function resolveTarget(target) {
  const path = TARGET_PATHS[target];
  if (!path) {
    throw new Error(`Unknown K6_CACHE_TARGET: ${target}`);
  }
  return path;
}

function resolveOptions(phase, requestName) {
  if (phase === "cold") {
    return cacheColdOptions(requestName);
  }
  if (phase === "warm") {
    return cacheWarmOptions(requestName);
  }
  throw new Error(`Unknown K6_CACHE_PHASE: ${phase}`);
}

const target = stringEnv("K6_CACHE_TARGET");
const phase = stringEnv("K6_CACHE_PHASE");
const yearMonth = stringEnv("K6_STATS_YEAR_MONTH", currentYearMonth());
const requestName = `cache:${target}:${phase}`;
const path = withQuery(resolveTarget(target), { yearMonth });

export const options = resolveOptions(phase, requestName);

export function setup() {
  const auth = authenticateExistingUser(seedUser(), { allowConfiguredToken: false });
  if (phase === "warm") {
    const primeResponse = get(path, {
      headers: authHeaders(auth.accessToken),
      tags: { name: `cache-prime:${target}` },
    });
    requireApiResponse(primeResponse, {
      label: `cache prime ${target}`,
      statuses: [200],
      requireResult: true,
    });
  }

  return { auth, path };
}

export default function ({ auth, path: targetPath }) {
  const response = get(targetPath, {
    headers: authHeaders(auth.accessToken),
    tags: { name: requestName },
  });
  requireApiResponse(response, {
    label: requestName,
    statuses: [200],
    requireResult: true,
  });
}

export function handleSummary(data) {
  return createSummary(data);
}
