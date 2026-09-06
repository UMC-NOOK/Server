import { group, check } from "k6";

import { mixedReadJourneyOptions } from "../config/profiles.js";
import { authenticateDevUser } from "../lib/auth.js";
import { checkApiResponse } from "../lib/checks.js";
import { scenarioUser } from "../lib/data.js";
import { stringEnv } from "../lib/env.js";
import { discoverReadTargets } from "../lib/readTargets.js";
import { authHeaders, get, withQuery } from "../lib/http.js";
import { createSummary } from "../lib/summary.js";
import { timelineItems } from "../lib/timeline.js";

export const options = mixedReadJourneyOptions;

function checkItems(response, path, label) {
  check(response, {
    [`${label}: returns items`]: (res) => {
      const items = res.json(path);
      return Array.isArray(items) && items.length > 0;
    },
  });
}

function readBookAndSearch(params, targets) {
  const detail = get(`/api/v1/books/id/${targets.bookId}`, {
    ...params,
    tags: { name: "read:book-detail" },
  });
  checkApiResponse(detail, {
    label: "read book detail",
    statuses: [200],
    requireResult: true,
  });

  const librarySearch = get(
    withQuery("/api/v1/books/search/LIBRARY", {
      keyword: targets.searchKeyword,
    }),
    {
      ...params,
      tags: { name: "read:books-search-library" },
    }
  );
  checkApiResponse(librarySearch, {
    label: "read books search library",
    statuses: [200],
    requireResult: true,
  });

  const libraryHome = get("/api/v1/books/search/library/home", {
    ...params,
    tags: { name: "read:books-search-library-home" },
  });
  checkApiResponse(libraryHome, {
    label: "read books search library home",
    statuses: [200],
    requireResult: true,
  });
}

function readLibrary(params) {
  const books = get(withQuery("/api/v1/library/books", { sort: "RECENT_FOCUSED", size: 20 }), {
    ...params,
    tags: { name: "read:library-books" },
  });
  checkApiResponse(books, {
    label: "read library books",
    statuses: [200],
    requireResult: true,
  });
  checkItems(books, "result.items", "read library books");

  const statusBooks = get(withQuery("/api/v1/library/status", { status: "BEFORE", size: 20 }), {
    ...params,
    tags: { name: "read:library-status-before" },
  });
  checkApiResponse(statusBooks, {
    label: "read library status before",
    statuses: [200],
    requireResult: true,
  });

  const count = get("/api/v1/library/count", {
    ...params,
    tags: { name: "read:library-count" },
  });
  checkApiResponse(count, {
    label: "read library count",
    statuses: [200],
    requireResult: true,
  });

  const beforeReading = get("/api/v1/library/before-reading", {
    ...params,
    tags: { name: "read:library-before-reading" },
  });
  checkApiResponse(beforeReading, {
    label: "read library before reading",
    statuses: [200],
    requireResult: true,
  });

  const years = get("/api/v1/library/years", {
    ...params,
    tags: { name: "read:library-years" },
  });
  checkApiResponse(years, {
    label: "read library years",
    statuses: [200],
    requireResult: true,
  });

  const recentFocus = get("/api/v1/library/recent-focus", {
    ...params,
    tags: { name: "read:library-recent-focus" },
  });
  checkApiResponse(recentFocus, {
    label: "read library recent focus",
    statuses: [200],
  });
}

function readRecords(params, targets) {
  const records = get(withQuery("/api/v1/records", { size: 20, order: "RECENT_RECORDED" }), {
    ...params,
    tags: { name: "read:records-list" },
  });
  checkApiResponse(records, {
    label: "read records list",
    statuses: [200],
    requireResult: true,
  });

  const bookRecords = get(withQuery(`/api/v1/records/books/${targets.bookId}`, { size: 20 }), {
    ...params,
    tags: { name: "read:records-book-list" },
  });
  checkApiResponse(bookRecords, {
    label: "read records book list",
    statuses: [200],
    requireResult: true,
  });

  const emotionCounts = get(`/api/v1/records/emotions/${targets.bookId}`, {
    ...params,
    tags: { name: "read:records-emotions" },
  });
  checkApiResponse(emotionCounts, {
    label: "read records emotions",
    statuses: [200],
    requireResult: true,
  });

  if (!targets.recordId) {
    return;
  }

  const detail = get(`/api/v1/records/${targets.recordId}`, {
    ...params,
    tags: { name: "read:records-detail" },
  });
  checkApiResponse(detail, {
    label: "read records detail",
    statuses: [200],
    requireResult: true,
  });
}

function readFocus(params) {
  const recent = get(withQuery("/api/v1/focuses/recent", { size: 10 }), {
    ...params,
    tags: { name: "read:focus-recent" },
  });
  checkApiResponse(recent, {
    label: "read focus recent",
    statuses: [200],
    requireResult: true,
  });
}

function readTimeline(params, targets) {
  const summary = get(`/api/v1/library/${targets.libraryId}/timeline/summary`, {
    ...params,
    tags: { name: "read:timeline-summary" },
  });
  checkApiResponse(summary, {
    label: "read timeline summary",
    statuses: [200],
    requireResult: true,
  });

  const preview = get(`/api/v1/library/${targets.libraryId}/timeline`, {
    ...params,
    tags: { name: "read:timeline-preview" },
  });
  checkApiResponse(preview, {
    label: "read timeline preview",
    statuses: [200],
    requireResult: true,
  });
  check(preview, {
    "read timeline preview: has items": (res) => timelineItems(res.json("result")).length > 0,
  });

  if (!targets.timelineId) {
    return;
  }

  const detail = get(`/api/v1/library/${targets.libraryId}/timeline/${targets.timelineId}`, {
    ...params,
    tags: { name: "read:timeline-detail" },
  });
  checkApiResponse(detail, {
    label: "read timeline detail",
    statuses: [200],
    requireResult: true,
  });
}

export function setup() {
  const configuredEmail = stringEnv("K6_USER_EMAIL");
  const user = configuredEmail
    ? {
        email: configuredEmail,
        nickName: stringEnv("K6_USER_NICKNAME", "k6read"),
      }
    : scenarioUser("read");

  const auth = authenticateDevUser(user);
  return discoverReadTargets(auth);
}

export default function (targets) {
  const params = {
    headers: authHeaders(targets.auth.accessToken),
  };

  group("book and search", () => readBookAndSearch(params, targets));
  group("library", () => readLibrary(params));
  group("records", () => readRecords(params, targets));
  group("focus", () => readFocus(params));
  group("timeline", () => readTimeline(params, targets));
}

export function handleSummary(data) {
  return createSummary(data);
}
