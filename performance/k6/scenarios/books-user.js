import { check } from "k6";

import { internalApiOptions } from "../config/profiles.js";
import { authenticateDevUser } from "../lib/auth.js";
import { requireApiResponse, requirePositiveNumber } from "../lib/checks.js";
import { scenarioUser, updatedUserBookPayload, userBookPayload } from "../lib/data.js";
import { authHeaders, get, patch, post } from "../lib/http.js";
import { createSummary } from "../lib/summary.js";

export const options = internalApiOptions;

export function setup() {
  return authenticateDevUser(scenarioUser("books"));
}

export default function (auth) {
  const params = {
    headers: authHeaders(auth.accessToken),
  };

  const createPayload = userBookPayload();
  const create = post("/api/v1/books/user", createPayload, {
    ...params,
    tags: { name: "books-user:create" },
  });
  requireApiResponse(create, {
    label: "books user create",
    statuses: [201, 200],
    requireResult: true,
  });

  const bookId = requirePositiveNumber(create.json("result.bookId"), "books user create bookId");
  const libraryId = requirePositiveNumber(create.json("result.libraryId"), "books user create libraryId");
  check(create, {
    "books user create: bookId exists": () => typeof bookId === "number" && bookId > 0,
    "books user create: libraryId exists": () => typeof libraryId === "number" && libraryId > 0,
    "books user create: sourceType USER": (res) => res.json("result.sourceType") === "USER",
  });

  const detail = get(`/api/v1/books/id/${bookId}`, {
    ...params,
    tags: { name: "books-user:detail" },
  });
  requireApiResponse(detail, {
    label: "books user detail",
    statuses: [200],
    requireResult: true,
  });
  check(detail, {
    "books user detail: same book": (res) => res.json("result.bookId") === bookId,
    "books user detail: same library": (res) => res.json("result.libraryId") === libraryId,
  });

  const updatePayload = updatedUserBookPayload(bookId);
  const update = patch(`/api/v1/books/user/${bookId}`, updatePayload, {
    ...params,
    tags: { name: "books-user:update" },
  });
  requireApiResponse(update, {
    label: "books user update",
    statuses: [200],
    requireResult: true,
  });
  check(update, {
    "books user update: title changed": (res) => res.json("result.title") === updatePayload.title,
    "books user update: same book": (res) => res.json("result.bookId") === bookId,
  });

  const updatedDetail = get(`/api/v1/books/id/${bookId}`, {
    ...params,
    tags: { name: "books-user:updated-detail" },
  });
  requireApiResponse(updatedDetail, {
    label: "books user updated detail",
    statuses: [200],
    requireResult: true,
  });
  check(updatedDetail, {
    "books user updated detail: title persisted": (res) => res.json("result.title") === updatePayload.title,
  });
}

export function handleSummary(data) {
  return createSummary(data);
}
