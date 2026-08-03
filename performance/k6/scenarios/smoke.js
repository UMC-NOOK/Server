import { smokeOptions } from "../config/profiles.js";
import { authenticateDevUser } from "../lib/auth.js";
import { checkApiResponse, checkStatus } from "../lib/checks.js";
import { stringEnv } from "../lib/env.js";
import { authHeaders, get } from "../lib/http.js";
import { createSummary } from "../lib/summary.js";

export const options = smokeOptions;

export function setup() {
  return authenticateDevUser();
}

export default function (auth) {
  const managementBaseUrl = stringEnv("MANAGEMENT_BASE_URL").replace(/\/$/, "");
  const health = get(`${managementBaseUrl}/actuator/health`, {
    tags: { name: "actuator:health" },
  });
  checkStatus(health, [200], "actuator health");

  const params = {
    headers: authHeaders(auth.accessToken),
  };

  const me = get("/api/v1/auth/me", {
    ...params,
    tags: { name: "auth:me" },
  });
  checkApiResponse(me, {
    label: "auth me",
    statuses: [200],
    requireResult: true,
  });

  const onboardingStatus = get("/api/v1/users/me/onboarding/status", {
    ...params,
    tags: { name: "onboarding:status" },
  });
  checkApiResponse(onboardingStatus, {
    label: "onboarding status",
    statuses: [200],
    requireResult: true,
  });
}

export function handleSummary(data) {
  return createSummary(data);
}
