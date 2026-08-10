import { check } from "k6";

import { requireApiResponse } from "./checks.js";
import { intEnv } from "./env.js";
import { authHeaders, get, withQuery } from "./http.js";
import { timelineItems } from "./timeline.js";

function nonNegativeInt(value, label) {
  if (!Number.isInteger(value) || value < 0) {
    throw new Error(`${label} must be a non-negative integer`);
  }
  return value;
}

function positiveInt(value, label) {
  if (!Number.isInteger(value) || value <= 0) {
    throw new Error(`${label} must be a positive integer`);
  }
  return value;
}

export function expectedSeedCounts() {
  const books = nonNegativeInt(intEnv("SEED_BOOKS", -1), "SEED_BOOKS");
  const recordsPerBook = nonNegativeInt(intEnv("SEED_RECORDS_PER_BOOK", -1), "SEED_RECORDS_PER_BOOK");
  const records = books * recordsPerBook;
  const focuses = nonNegativeInt(intEnv("SEED_FOCUS_SESSIONS", -1), "SEED_FOCUS_SESSIONS");

  return {
    books,
    records,
    focuses,
    timelines: books + records + focuses,
  };
}

export function countTimelineItems(timelinePreview) {
  return timelineItems(timelinePreview).length;
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

function liveLibraryBookIds(params) {
  const bookIds = [];
  let cursor;
  let page = 0;

  while (true) {
    const response = get(withQuery("/api/v1/library/books", {
      sort: "ALPHABETICAL",
      cursor,
      size: 100,
    }), {
      ...params,
      tags: { name: "seed-verify:library-books" },
    });
    requireApiResponse(response, {
      label: "seed verify library books",
      statuses: [200],
      requireResult: true,
    });

    const items = response.json("result.items");
    if (!Array.isArray(items)) {
      throw new Error("seed verify library books items must be an array");
    }
    items.forEach((item) => {
      bookIds.push(positiveInt(item?.bookId, "seed verify library bookId"));
    });

    if (response.json("result.hasNext") !== true) {
      return bookIds;
    }

    const nextCursor = response.json("result.nextCursor");
    if (typeof nextCursor !== "string" || nextCursor.length === 0 || nextCursor === cursor) {
      throw new Error("seed verify library books returned an invalid nextCursor");
    }
    cursor = nextCursor;
    page += 1;
    if (page > 10000) {
      throw new Error("seed verify library books exceeded the pagination safety limit");
    }
  }
}

function liveTimelineCount(params) {
  return liveLibraryBookIds(params).reduce((total, bookId) => {
    const book = get(`/api/v1/books/id/${bookId}`, {
      ...params,
      tags: { name: "seed-verify:book-detail" },
    });
    requireApiResponse(book, {
      label: "seed verify book detail",
      statuses: [200],
      requireResult: true,
    });
    const libraryId = positiveInt(book.json("result.libraryId"), "seed verify libraryId");

    const timeline = get(`/api/v1/library/${libraryId}/timeline`, {
      ...params,
      tags: { name: "seed-verify:timeline-count" },
    });
    requireApiResponse(timeline, {
      label: "seed verify timeline count",
      statuses: [200],
      requireResult: true,
    });

    return total + countTimelineItems(timeline.json("result"));
  }, 0);
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
      timelines: liveTimelineCount(params),
    },
    expected: expectedSeedCounts(),
  };
}

export function checkExactSeedCounts(seedCounts, label) {
  return check(seedCounts, {
    [`${label}: book count is exact`]: (value) => value.actual.books === value.expected.books,
    [`${label}: record count is exact`]: (value) => value.actual.records === value.expected.records,
    [`${label}: focus count is exact`]: (value) => value.actual.focuses === value.expected.focuses,
    [`${label}: timeline count is exact`]: (value) => value.actual.timelines === value.expected.timelines,
  });
}
