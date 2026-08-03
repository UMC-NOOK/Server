import { check } from "k6";

import { internalApiOptions } from "../config/profiles.js";
import { authenticateDevUser } from "../lib/auth.js";
import { requireApiResponse, requirePositiveNumber } from "../lib/checks.js";
import { scenarioUser, searchableUserBookPayload } from "../lib/data.js";
import { authHeaders, del, get, post, withQuery } from "../lib/http.js";
import { createSummary } from "../lib/summary.js";

export const options = internalApiOptions;

function createSearchableBooks(params, keyword, count) {
  const books = [];

  for (let index = 0; index < count; index += 1) {
    const response = post("/api/v1/books/user", searchableUserBookPayload(index, keyword), {
      ...params,
      tags: { name: "books-search:seed-book" },
    });

    requireApiResponse(response, {
      label: `books search seed ${index}`,
      statuses: [201, 200],
      requireResult: true,
    });

    books.push({
      bookId: requirePositiveNumber(response.json("result.bookId"), `books search seed ${index} bookId`),
      libraryId: requirePositiveNumber(response.json("result.libraryId"), `books search seed ${index} libraryId`),
      title: response.json("result.title"),
    });
  }

  return books;
}

export function setup() {
  return authenticateDevUser(scenarioUser("search"));
}

export default function (auth) {
  const params = {
    headers: authHeaders(auth.accessToken),
  };
  const keyword = `k6-search-${auth.user.email.split("@")[0]}-${__ITER}`;

  const books = createSearchableBooks(params, keyword, 12);

  const firstSearch = get(withQuery("/api/v1/books/search/LIBRARY", { keyword }), {
    ...params,
    tags: { name: "books-search:library-first" },
  });
  requireApiResponse(firstSearch, {
    label: "books search library first",
    statuses: [200],
    requireResult: true,
  });

  const firstBooks = firstSearch.json("result.books") || [];
  const nextCursor = requirePositiveNumber(firstSearch.json("result.nextCursor"), "books search library nextCursor");
  check(firstSearch, {
    "books search library first: returns books": () => firstBooks.length > 0,
    "books search library first: has next cursor": () => typeof nextCursor === "number",
    "books search library first: seeded book included": () =>
      firstBooks.some((book) => books.some((seed) => seed.bookId === book.bookId)),
  });

  const nextSearch = get(withQuery("/api/v1/books/search/LIBRARY", { keyword, cursor: nextCursor }), {
    ...params,
    tags: { name: "books-search:library-next" },
  });
  requireApiResponse(nextSearch, {
    label: "books search library next",
    statuses: [200],
    requireResult: true,
  });
  check(nextSearch, {
    "books search library next: returns remaining books": (res) => {
      const result = res.json("result.books") || [];
      return result.length > 0;
    },
  });

  const histories = get("/api/v1/books/search/LIBRARY/histories", {
    ...params,
    tags: { name: "books-search:histories" },
  });
  requireApiResponse(histories, {
    label: "books search histories",
    statuses: [200],
    requireResult: true,
  });
  check(histories, {
    "books search histories: contains keyword": (res) => {
      const result = res.json("result") || [];
      return result.includes(keyword);
    },
  });

  const deleteHistory = del(withQuery("/api/v1/books/search/LIBRARY/histories", { keyword }), {
    ...params,
    tags: { name: "books-search:delete-history" },
  });
  requireApiResponse(deleteHistory, {
    label: "books search delete history",
    statuses: [200],
  });

  const historiesAfterDelete = get("/api/v1/books/search/LIBRARY/histories", {
    ...params,
    tags: { name: "books-search:histories-after-delete" },
  });
  requireApiResponse(historiesAfterDelete, {
    label: "books search histories after delete",
    statuses: [200],
    requireResult: true,
  });
  check(historiesAfterDelete, {
    "books search histories after delete: keyword removed": (res) => {
      const result = res.json("result") || [];
      return !result.includes(keyword);
    },
  });

  const repeatSearch = get(withQuery("/api/v1/books/search/LIBRARY", { keyword }), {
    ...params,
    tags: { name: "books-search:library-repeat" },
  });
  requireApiResponse(repeatSearch, {
    label: "books search library repeat",
    statuses: [200],
    requireResult: true,
  });

  const deleteAllHistories = del("/api/v1/books/search/LIBRARY/histories/all", {
    ...params,
    tags: { name: "books-search:delete-all-histories" },
  });
  requireApiResponse(deleteAllHistories, {
    label: "books search delete all histories",
    statuses: [200],
  });

  const historiesAfterDeleteAll = get("/api/v1/books/search/LIBRARY/histories", {
    ...params,
    tags: { name: "books-search:histories-after-delete-all" },
  });
  requireApiResponse(historiesAfterDeleteAll, {
    label: "books search histories after delete all",
    statuses: [200],
    requireResult: true,
  });
  check(historiesAfterDeleteAll, {
    "books search histories after delete all: empty": (res) => {
      const result = res.json("result") || [];
      return result.length === 0;
    },
  });

  const libraryHome = get("/api/v1/books/search/library/home", {
    ...params,
    tags: { name: "books-search:library-home" },
  });
  requireApiResponse(libraryHome, {
    label: "books search library home",
    statuses: [200],
    requireResult: true,
  });
  check(libraryHome, {
    "books search library home: has sections": (res) => {
      const sections = res.json("result.sections") || [];
      return sections.length > 0;
    },
    "books search library home: before reading available": (res) => {
      const sections = res.json("result.sections") || [];
      return sections.some((section) => section.type === "BEFORE_READING");
    },
  });
}

export function handleSummary(data) {
  return createSummary(data);
}
