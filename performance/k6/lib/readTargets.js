import { check } from "k6";

import { checkApiResponse } from "./checks.js";
import { intEnv, stringEnv } from "./env.js";
import { authHeaders, get, withQuery } from "./http.js";
import { timelineItems } from "./timeline.js";

function safeJson(response, path, fallback = null) {
  try {
    const value = response.json(path);
    return value === undefined ? fallback : value;
  } catch (_) {
    return fallback;
  }
}

function positiveIntEnv(name) {
  const value = intEnv(name, 0);
  return value > 0 ? value : null;
}

function firstArrayItem(response, path) {
  const items = safeJson(response, path, []);
  return Array.isArray(items) && items.length > 0 ? items[0] : null;
}

function discoverBookId(params) {
  const configuredBookId = positiveIntEnv("K6_BOOK_ID");
  if (configuredBookId) {
    return configuredBookId;
  }

  const response = get(withQuery("/api/v1/library/books", { sort: "RECENT_FOCUSED", size: 1 }), {
    ...params,
    tags: { name: "read-targets:library-books" },
  });
  checkApiResponse(response, {
    label: "read targets library books",
    statuses: [200],
  });

  return firstArrayItem(response, "result.items")?.bookId || null;
}

function discoverLibraryId(params, bookId) {
  const configuredLibraryId = positiveIntEnv("K6_LIBRARY_ID");
  if (configuredLibraryId) {
    return configuredLibraryId;
  }

  const response = get(`/api/v1/books/id/${bookId}`, {
    ...params,
    tags: { name: "read-targets:book-detail" },
  });
  checkApiResponse(response, {
    label: "read targets book detail",
    statuses: [200],
    requireResult: true,
  });

  return safeJson(response, "result.libraryId");
}

function discoverRecordId(params, bookId) {
  const configuredRecordId = positiveIntEnv("K6_RECORD_ID");
  if (configuredRecordId) {
    return configuredRecordId;
  }

  const response = get(withQuery(`/api/v1/records/books/${bookId}`, { size: 1 }), {
    ...params,
    tags: { name: "read-targets:book-records" },
  });
  checkApiResponse(response, {
    label: "read targets book records",
    statuses: [200],
    requireResult: true,
  });

  return firstArrayItem(response, "result.items")?.recordId || null;
}

function discoverTimelineId(params, libraryId) {
  const configuredTimelineId = positiveIntEnv("K6_TIMELINE_ID");
  if (configuredTimelineId) {
    return configuredTimelineId;
  }

  const response = get(`/api/v1/library/${libraryId}/timeline`, {
    ...params,
    tags: { name: "read-targets:timeline-preview" },
  });
  checkApiResponse(response, {
    label: "read targets timeline preview",
    statuses: [200],
    requireResult: true,
  });

  return timelineItems(safeJson(response, "result", {}))[0]?.timelineId || null;
}

export function discoverReadTargets(auth) {
  const params = {
    headers: authHeaders(auth.accessToken),
  };

  const bookId = discoverBookId(params);
  if (!bookId) {
    throw new Error("No readable book found. Run prepare-seed first or provide K6_BOOK_ID.");
  }

  const libraryId = discoverLibraryId(params, bookId);
  if (!libraryId) {
    throw new Error("No readable library found. Run prepare-seed first or provide K6_LIBRARY_ID.");
  }

  const recordId = discoverRecordId(params, bookId);
  const timelineId = discoverTimelineId(params, libraryId);

  check(null, {
    "read targets: bookId resolved": () => Number.isFinite(bookId),
    "read targets: libraryId resolved": () => Number.isFinite(libraryId),
  });

  return {
    auth,
    bookId,
    libraryId,
    recordId,
    timelineId,
    searchKeyword: stringEnv("K6_SEARCH_KEYWORD", "k6"),
  };
}
