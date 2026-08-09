import { check } from "k6";

import { requireApiResponse, requirePositiveNumber } from "./checks.js";
import { post } from "./http.js";
import { userBookPayload } from "./data.js";

export function createUserBook(params, overrides = {}, tagName = "books:create-helper") {
  const response = post("/api/v1/books/user", userBookPayload(overrides), {
    ...params,
    tags: { name: tagName },
  });

  requireApiResponse(response, {
    label: tagName,
    statuses: [201, 200],
    requireResult: true,
  });

  const book = {
    bookId: requirePositiveNumber(response.json("result.bookId"), `${tagName} bookId`),
    libraryId: requirePositiveNumber(response.json("result.libraryId"), `${tagName} libraryId`),
    title: response.json("result.title"),
  };

  check(response, {
    [`${tagName}: bookId exists`]: () => typeof book.bookId === "number" && book.bookId > 0,
    [`${tagName}: libraryId exists`]: () => typeof book.libraryId === "number" && book.libraryId > 0,
  });

  return book;
}
