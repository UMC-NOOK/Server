import {
  singleApiArrivalRateOptions,
  singleApiRampingArrivalRateOptions,
} from "../config/profiles.js";
import { authenticateExistingUser } from "../lib/auth.js";
import { requireApiResponse } from "../lib/checks.js";
import { seedUser } from "../lib/data.js";
import { stringEnv } from "../lib/env.js";
import { authHeaders, get, withQuery } from "../lib/http.js";
import { discoverReadTargets } from "../lib/readTargets.js";
import { createSummary } from "../lib/summary.js";

function libraryBooks(sort) {
  return () => withQuery("/api/v1/library/books", {
    sort,
    size: 20,
  });
}

const READ_DEFINITIONS = {
  "timeline-list": {
    path: (targets) => `/api/v1/library/${targets.libraryId}/timeline`,
  },
  "timeline-summary": {
    path: (targets) => `/api/v1/library/${targets.libraryId}/timeline/summary`,
  },
  "timeline-detail": {
    path: (targets) => `/api/v1/library/${targets.libraryId}/timeline/${targets.timelineId}`,
    requiredTarget: "timelineId",
  },
  "library-books-recent-focused": {
    path: libraryBooks("RECENT_FOCUSED"),
  },
  "library-books-record-count-desc": {
    path: libraryBooks("RECORD_COUNT_DESC"),
  },
  "library-books-record-count-asc": {
    path: libraryBooks("RECORD_COUNT_ASC"),
  },
  "library-books-alphabetical": {
    path: libraryBooks("ALPHABETICAL"),
  },
  "records-list": {
    path: () => withQuery("/api/v1/records", {
      order: "RECENT_RECORDED",
      size: 20,
    }),
  },
  "records-book-list": {
    path: (targets) => withQuery(`/api/v1/records/books/${targets.bookId}`, {
      size: 20,
    }),
  },
  "records-emotions": {
    path: (targets) => `/api/v1/records/emotions/${targets.bookId}`,
  },
  "records-detail": {
    path: (targets) => `/api/v1/records/${targets.recordId}`,
    requiredTarget: "recordId",
  },
};

function resolveReadDefinition(readTarget) {
  const definition = READ_DEFINITIONS[readTarget];
  if (!definition) {
    throw new Error(`Unknown K6_READ_TARGET: ${readTarget}`);
  }
  return definition;
}

function resolveOptions(profile, requestName) {
  if (profile === "arrival") {
    return singleApiArrivalRateOptions(requestName);
  }
  if (profile === "ramping") {
    return singleApiRampingArrivalRateOptions(requestName);
  }
  throw new Error(`Unknown K6_SINGLE_API_PROFILE: ${profile}`);
}

const readTarget = stringEnv("K6_READ_TARGET");
const requestName = `read:${readTarget}`;
const definition = resolveReadDefinition(readTarget);
const profile = stringEnv("K6_SINGLE_API_PROFILE", "arrival");

export const options = resolveOptions(profile, requestName);

export function setup() {
  const targets = discoverReadTargets(authenticateExistingUser(seedUser()));
  if (definition.requiredTarget && !Number.isFinite(targets[definition.requiredTarget])) {
    throw new Error(`${definition.requiredTarget} is required for ${readTarget}`);
  }
  return targets;
}

export default function (targets) {
  const response = get(definition.path(targets), {
    headers: authHeaders(targets.auth.accessToken),
    tags: { name: requestName },
  });
  requireApiResponse(response, {
    label: readTarget,
    statuses: [200],
    requireResult: true,
  });
}

export function handleSummary(data) {
  return createSummary(data);
}
