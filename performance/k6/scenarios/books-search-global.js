import { check } from "k6";

import { externalApiOptions } from "../config/profiles.js";
import { authenticateDevUser } from "../lib/auth.js";
import { checkApiResponse } from "../lib/checks.js";
import { runId } from "../lib/data.js";
import { boolEnv, intEnv, stringEnv } from "../lib/env.js";
import { authHeaders, get, withQuery } from "../lib/http.js";
import { createSummary } from "../lib/summary.js";

export const options = externalApiOptions;

const GLOBAL_SEARCH_CHECK_TAGS = { scope: "global-search" };

function searchKeywords() {
  const rawKeywords = stringEnv("K6_GLOBAL_SEARCH_KEYWORDS", stringEnv("K6_SEARCH_KEYWORD", "자바"));
  const keywords = rawKeywords
    .split(",")
    .map((keyword) => keyword.trim())
    .filter(Boolean);

  return keywords.length > 0 ? keywords : ["자바"];
}

function globalSearchUser(index) {
  return {
    email: `global-${runId()}-${index}@test.com`,
    nickName: `global${runId()}${index}`.replace(/[^a-zA-Z0-9가-힣]/g, "").slice(0, 20),
  };
}

function assertPoolCredentialPolicy(userPoolSize) {
  const hasScalarToken = Boolean(stringEnv("TOKEN") || stringEnv("K6_ACCESS_TOKEN"));

  if (userPoolSize > 1 && hasScalarToken) {
    throw new Error("GLOBAL/Aladin user pool cannot use a single TOKEN or K6_ACCESS_TOKEN when K6_GLOBAL_USER_POOL_SIZE > 1");
  }
}

export function setup() {
  if (!boolEnv("K6_ENABLE_EXTERNAL_API")) {
    throw new Error("GLOBAL/Aladin scenario requires K6_ENABLE_EXTERNAL_API=yes");
  }

  const userPoolSize = Math.max(1, intEnv("K6_GLOBAL_USER_POOL_SIZE", intEnv("MAX_VUS", 20)));
  assertPoolCredentialPolicy(userPoolSize);

  const authPool = [];
  for (let index = 0; index < userPoolSize; index += 1) {
    authPool.push(authenticateDevUser(globalSearchUser(index), { useConfiguredUser: false }));
  }

  return {
    authPool,
    keywords: searchKeywords(),
  };
}

export default function ({ authPool, keywords }) {
  const auth = authPool[(__VU - 1) % authPool.length];
  const params = {
    headers: authHeaders(auth.accessToken),
  };
  const keyword = keywords[__ITER % keywords.length];

  const firstSearch = get(withQuery("/api/v1/books/search/GLOBAL", { keyword }), {
    ...params,
    tags: { name: "books-search:global-first" },
  });
  const firstOk = checkApiResponse(firstSearch, {
    label: "books search global first",
    statuses: [200],
    requireResult: true,
  }, GLOBAL_SEARCH_CHECK_TAGS);
  if (!firstOk) {
    throw new Error(`books search global first failed. status=${firstSearch.status}`);
  }

  const books = firstSearch.json("result.books") || [];
  const hasNext = firstSearch.json("result.hasNext") === true;
  const nextCursor = firstSearch.json("result.nextCursor");
  check(firstSearch, {
    "books search global first: returns books": () => books.length > 0,
    "books search global first: books is array": () => Array.isArray(books),
  }, GLOBAL_SEARCH_CHECK_TAGS);

  if (!hasNext || typeof nextCursor !== "number") {
    return;
  }

  const nextSearch = get(withQuery("/api/v1/books/search/GLOBAL", { keyword, cursor: nextCursor }), {
    ...params,
    tags: { name: "books-search:global-next" },
  });
  const nextOk = checkApiResponse(nextSearch, {
    label: "books search global next",
    statuses: [200],
    requireResult: true,
  }, GLOBAL_SEARCH_CHECK_TAGS);
  if (!nextOk) {
    throw new Error(`books search global next failed. status=${nextSearch.status}`);
  }
  check(nextSearch, {
    "books search global next: books is array": (res) => Array.isArray(res.json("result.books") || []),
  }, GLOBAL_SEARCH_CHECK_TAGS);
}

export function handleSummary(data) {
  return createSummary(data);
}
