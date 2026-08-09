import { check } from "k6";

import { requireApiResponse } from "./checks.js";
import { intEnv } from "./env.js";
import { authHeaders, get, withQuery } from "./http.js";

function nonNegativeInt(value, label) {
  if (!Number.isInteger(value) || value < 0) {
    throw new Error(`${label} must be a non-negative integer`);
  }
  return value;
}

function expectedSeedCounts() {
  const books = nonNegativeInt(intEnv("SEED_BOOKS", -1), "SEED_BOOKS");
  const recordsPerBook = nonNegativeInt(intEnv("SEED_RECORDS_PER_BOOK", -1), "SEED_RECORDS_PER_BOOK");

  return {
    books,
    records: books * recordsPerBook,
    focuses: nonNegativeInt(intEnv("SEED_FOCUS_SESSIONS", -1), "SEED_FOCUS_SESSIONS"),
  };
}

function liveCount(params, path, resultPath, label, requestName) {
  const response = get(path, {
    ...params,
    tags: { name: requestName },
  });
  requireApiResponse(response, {
    label,
    statuses: [200],
    requireResult: true,
  });
  return nonNegativeInt(response.json(resultPath), `${label} result`);
}

function liveFocusCount(params) {
  let count = 0;
  let cursor;
  let page = 0;

  while (true) {
    const response = get(withQuery("/api/v1/focuses/recent", { cursor, size: 50 }), {
      ...params,
      tags: { name: "seed-verify:focus-count" },
    });
    requireApiResponse(response, {
      label: "seed verify focus count",
      statuses: [200],
      requireResult: true,
    });

    const items = response.json("result.items");
    if (!Array.isArray(items)) {
      throw new Error("seed verify focus count items must be an array");
    }
    count += items.length;

    if (response.json("result.hasNext") !== true) {
      return count;
    }

    const nextCursor = response.json("result.nextCursor");
    if (!Number.isInteger(nextCursor) || nextCursor <= 0 || nextCursor === cursor) {
      throw new Error("seed verify focus count returned an invalid nextCursor");
    }
    cursor = nextCursor;
    page += 1;
    if (page > 10000) {
      throw new Error("seed verify focus count exceeded the pagination safety limit");
    }
  }
}

export function readSeedCounts(accessToken) {
  const params = {
    headers: authHeaders(accessToken),
  };

  return {
    actual: {
      books: liveCount(
        params,
        "/api/v1/library/count",
        "result.totalBookNum",
        "seed verify book count",
        "seed-verify:book-count"
      ),
      records: liveCount(
        params,
        "/api/v1/records/count",
        "result.count",
        "seed verify record count",
        "seed-verify:record-count"
      ),
      focuses: liveFocusCount(params),
    },
    expected: expectedSeedCounts(),
  };
}

export function checkExactSeedCounts(seedCounts, label) {
  return check(seedCounts, {
    [`${label}: book count is exact`]: (value) => value.actual.books === value.expected.books,
    [`${label}: record count is exact`]: (value) => value.actual.records === value.expected.records,
    [`${label}: focus count is exact`]: (value) => value.actual.focuses === value.expected.focuses,
  });
}
