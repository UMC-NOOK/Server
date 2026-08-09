import { runId as envRunId, stringEnv } from "./env.js";

const RUN_ID = envRunId();

export function runId() {
  return RUN_ID;
}

export function uniqueEmail(prefix = "k6") {
  const vu = typeof __VU === "number" ? __VU : 0;
  const iter = typeof __ITER === "number" ? __ITER : 0;

  return `${prefix}-${RUN_ID}-${vu}-${iter}@test.com`;
}

export function uniqueNickname(prefix = "k6") {
  const vu = typeof __VU === "number" ? __VU : 0;
  const iter = typeof __ITER === "number" ? __ITER : 0;
  const nickname = `${prefix}${RUN_ID}${vu}${iter}`.replace(/[^a-zA-Z0-9가-힣]/g, "");

  return nickname.slice(0, 20);
}

export function smokeUser() {
  return {
    email: __ENV.K6_USER_EMAIL || uniqueEmail("smoke"),
    nickName: __ENV.K6_USER_NICKNAME || uniqueNickname("smoke"),
  };
}

export function scenarioUser(prefix) {
  return {
    email: uniqueEmail(prefix),
    nickName: uniqueNickname(prefix),
  };
}

export function seedUser() {
  const namespace = stringEnv("SEED_NAMESPACE", RUN_ID).replace(/[^a-zA-Z0-9_-]/g, "");
  if (!namespace) {
    throw new Error("SEED_NAMESPACE must contain letters, numbers, underscores, or hyphens");
  }

  return {
    email: stringEnv("K6_USER_EMAIL", `seed-${namespace}@test.com`),
    nickName: stringEnv("K6_USER_NICKNAME", `seed${namespace}`.replace(/[^a-zA-Z0-9가-힣]/g, "").slice(0, 20)),
  };
}

export function onboardingPayload(overrides = {}) {
  const iter = typeof __ITER === "number" ? __ITER : 0;

  return {
    goal: 100 + (iter % 50),
    nickname: uniqueNickname("onb").slice(0, 10),
    categories: ["소설/시/희곡"],
    ...overrides,
  };
}

export function goalUpdatePayload(goal = 120) {
  return { goal };
}

export function userBookPayload(overrides = {}) {
  const iter = typeof __ITER === "number" ? __ITER : 0;
  const suffix = `${RUN_ID}-${iter}`;

  return {
    title: `k6 user book ${suffix}`,
    author: "k6 author",
    categoryName: "소설/시/희곡",
    description: "k6 performance test book",
    pages: 240 + (iter % 60),
    publisher: "k6 publisher",
    publicationDate: "2026-07-07",
    isbn13: null,
    ...overrides,
  };
}

export function searchableUserBookPayload(index, keyword, overrides = {}) {
  const iter = typeof __ITER === "number" ? __ITER : 0;

  return userBookPayload({
    title: `${keyword} ${RUN_ID} ${iter} ${index}`,
    author: `k6 search author ${index}`,
    description: `k6 searchable book ${keyword}`,
    pages: 200 + index,
    ...overrides,
  });
}

export function updatedUserBookPayload(bookId, overrides = {}) {
  return userBookPayload({
    title: `k6 updated book ${bookId}`,
    description: "k6 performance test book updated",
    pages: 300,
    publicationDate: "2026-07-08",
    ...overrides,
  });
}
