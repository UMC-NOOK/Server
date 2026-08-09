import { check } from "k6";

import { internalApiOptions } from "../config/profiles.js";
import { authenticateDevUser } from "../lib/auth.js";
import { checkApiResponse } from "../lib/checks.js";
import { goalUpdatePayload, onboardingPayload, scenarioUser } from "../lib/data.js";
import { authHeaders, get, patch, post } from "../lib/http.js";
import { createSummary } from "../lib/summary.js";

export const options = internalApiOptions;

export function setup() {
  return authenticateDevUser(scenarioUser("onboarding"));
}

export default function (auth) {
  const params = {
    headers: authHeaders(auth.accessToken),
  };

  const complete = post("/api/v1/users/me/onboarding/complete", onboardingPayload(), {
    ...params,
    tags: { name: "onboarding:complete" },
  });
  checkApiResponse(complete, {
    label: "onboarding complete",
    statuses: [200],
    requireResult: true,
  });
  check(complete, {
    "onboarding complete: completed": (res) => res.json("result.onboardingCompleted") === true,
  });

  const status = get("/api/v1/users/me/onboarding/status", {
    ...params,
    tags: { name: "onboarding:status" },
  });
  checkApiResponse(status, {
    label: "onboarding status",
    statuses: [200],
    requireResult: true,
  });

  const goal = get("/api/v1/users/me/onboarding/goal", {
    ...params,
    tags: { name: "onboarding:goal" },
  });
  checkApiResponse(goal, {
    label: "onboarding goal",
    statuses: [200],
    requireResult: true,
  });

  const updatedGoal = 120 + (__ITER % 30);
  const updateGoal = patch("/api/v1/users/me/onboarding/goal", goalUpdatePayload(updatedGoal), {
    ...params,
    tags: { name: "onboarding:update-goal" },
  });
  checkApiResponse(updateGoal, {
    label: "onboarding update goal",
    statuses: [200],
    requireResult: true,
  });
  check(updateGoal, {
    "onboarding update goal: goal changed": (res) => res.json("result.goal") === updatedGoal,
  });
}

export function handleSummary(data) {
  return createSummary(data);
}
