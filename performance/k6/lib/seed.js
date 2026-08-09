import { check } from "k6";

import { checkApiResponse, requirePositiveNumber } from "./checks.js";
import { createUserBook } from "./books.js";
import { intEnv, stringEnv } from "./env.js";
import { authHeaders, get, post } from "./http.js";
import { onboardingPayload } from "./data.js";

const DEFAULT_EMOTIONS = ["FUN", "EMPATHIZING", "USEFUL", "COMPLICATED", "SAD"];

function positiveIntEnv(name, fallback) {
  return Math.max(0, intEnv(name, fallback));
}

function seedContent(prefix, index) {
  return `${prefix} ${index}`;
}

function buildSeedConfig(overrides = {}) {
  return {
    bookCount: positiveIntEnv("SEED_BOOKS", 5),
    recordsPerBook: positiveIntEnv("SEED_RECORDS_PER_BOOK", 2),
    focusSessions: positiveIntEnv("SEED_FOCUS_SESSIONS", 2),
    bookTitlePrefix: stringEnv("SEED_BOOK_TITLE_PREFIX", "k6 seed book"),
    recordPrefix: stringEnv("SEED_RECORD_PREFIX", "k6 seed record"),
    ...overrides,
  };
}

export function completeOnboarding(params, overrides = {}) {
  const status = get("/api/v1/users/me/onboarding/status", {
    ...params,
    tags: { name: "seed:onboarding-status" },
  });

  const statusOk = checkApiResponse(status, {
    label: "seed onboarding status",
    statuses: [200],
    requireResult: true,
  });
  if (!statusOk) {
    throw new Error(`seed onboarding status failed. status=${status.status}`);
  }

  if (status.json("result.needsOnboarding") === false) {
    return {
      completed: true,
      skipped: true,
    };
  }

  const response = post("/api/v1/users/me/onboarding/complete", onboardingPayload(overrides), {
    ...params,
    tags: { name: "seed:onboarding-complete" },
  });

  const completeOk = checkApiResponse(response, {
    label: "seed onboarding complete",
    statuses: [200],
    requireResult: true,
  });
  if (!completeOk) {
    throw new Error(`seed onboarding complete failed. status=${response.status}`);
  }
  check(response, {
    "seed onboarding complete: completed": (res) => res.json("result.onboardingCompleted") === true,
  });

  return {
    completed: response.json("result.onboardingCompleted") === true,
  };
}

export function createSeedBooks(params, count, titlePrefix = "k6 seed book") {
  const books = [];

  for (let index = 0; index < count; index += 1) {
    books.push(
      createUserBook(
        params,
        {
          title: seedContent(titlePrefix, index),
          pages: 180 + index,
        },
        "seed:book-create"
      )
    );
  }

  return books;
}

export function createBookRecords(params, books, recordsPerBook, contentPrefix = "k6 seed record") {
  const records = [];

  books.forEach((book, bookIndex) => {
    for (let index = 0; index < recordsPerBook; index += 1) {
      const emotion = DEFAULT_EMOTIONS[(bookIndex + index) % DEFAULT_EMOTIONS.length];
      const response = post(
        `/api/v1/records/books/${book.bookId}`,
        {
          content: seedContent(`${contentPrefix} ${bookIndex}`, index),
          emotion,
          imageKeys: [],
        },
        {
          ...params,
          tags: { name: "seed:record-create" },
        }
      );

      const recordOk = checkApiResponse(response, {
        label: "seed record create",
        statuses: [201, 200],
      });
      if (!recordOk) {
        throw new Error(`seed record create failed. status=${response.status}`);
      }

      records.push({
        bookId: book.bookId,
        libraryId: book.libraryId,
        emotion,
      });
    }
  });

  return records;
}

export function createFocusSessions(params, books, count) {
  if (count === 0 || books.length === 0) {
    return [];
  }

  const themes = get("/api/v1/focuses/themes", {
    ...params,
    tags: { name: "seed:focus-themes" },
  });
  const themesOk = checkApiResponse(themes, {
    label: "seed focus themes",
    statuses: [200],
    requireResult: true,
  });
  if (!themesOk) {
    throw new Error(`seed focus themes failed. status=${themes.status}`);
  }

  const theme = (themes.json("result.themes") || [])[0];
  if (!theme?.themeId) {
    console.warn("No focus themes found. Skipping seed focus sessions.");
    return [];
  }

  const sessions = [];
  for (let index = 0; index < count; index += 1) {
    const book = books[index % books.length];
    const start = post(
      "/api/v1/focuses/start",
      {
        libraryId: book.libraryId,
        themeId: theme.themeId,
      },
      {
        ...params,
        tags: { name: "seed:focus-start" },
      }
    );
    const startOk = checkApiResponse(start, {
      label: "seed focus start",
      statuses: [201, 200],
      requireResult: true,
    });
    if (!startOk) {
      throw new Error(`seed focus start failed. status=${start.status}`);
    }

    const focusId = requirePositiveNumber(start.json("result.focusId"), "seed focus start focusId");

    const end = post(
      "/api/v1/focuses/end",
      {
        focusId,
        page: 20 + index,
        isFinished: false,
      },
      {
        ...params,
        tags: { name: "seed:focus-end" },
      }
    );
    const endOk = checkApiResponse(end, {
      label: "seed focus end",
      statuses: [200],
      requireResult: true,
    });
    if (!endOk) {
      throw new Error(`seed focus end failed. status=${end.status}`);
    }

    sessions.push({
      focusId,
      bookId: book.bookId,
      libraryId: book.libraryId,
    });
  }

  return sessions;
}

export function seedUserDataset(auth, overrides = {}) {
  const config = buildSeedConfig(overrides);
  const params = {
    headers: authHeaders(auth.accessToken),
  };

  const onboarding = completeOnboarding(params);
  const books = createSeedBooks(params, config.bookCount, config.bookTitlePrefix);
  const records = createBookRecords(params, books, config.recordsPerBook, config.recordPrefix);
  const focusSessions = createFocusSessions(params, books, config.focusSessions);

  return {
    user: auth.user,
    onboarding,
    books,
    records,
    focusSessions,
    config,
  };
}
