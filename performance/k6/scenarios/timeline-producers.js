import { check } from "k6";

import { internalApiOptions } from "../config/profiles.js";
import { authenticateDevUser } from "../lib/auth.js";
import { createUserBook } from "../lib/books.js";
import { checkApiResponse, requireApiResponse, requirePositiveNumber } from "../lib/checks.js";
import { scenarioUser } from "../lib/data.js";
import { authHeaders, get, patch, post } from "../lib/http.js";
import { createSummary } from "../lib/summary.js";
import { findTimelineItemByType } from "../lib/timeline.js";

export const options = internalApiOptions;

function latestTimelinePreview(params, libraryId) {
  const response = get(`/api/v1/library/${libraryId}/timeline`, {
    ...params,
    tags: { name: "timeline-producers:preview" },
  });

  requireApiResponse(response, {
    label: "timeline producers preview",
    statuses: [200],
    requireResult: true,
  });

  return response.json("result");
}

function assertTimelineDetail(params, libraryId, item, type) {
  const itemExists = check(null, {
    [`timeline producers: ${type} item exists`]: () => Boolean(item?.timelineId),
  });

  if (!itemExists) {
    throw new Error(`timeline producers ${type} timelineId is required`);
  }
  const timelineId = requirePositiveNumber(item.timelineId, `timeline producers ${type} timelineId`);

  const response = get(`/api/v1/library/${libraryId}/timeline/${timelineId}`, {
    ...params,
    tags: { name: `timeline-producers:${type.toLowerCase()}-detail` },
  });

  checkApiResponse(response, {
    label: `timeline producers ${type} detail`,
    statuses: [200],
    requireResult: true,
  });
  check(response, {
    [`timeline producers ${type} detail: type matches`]: (res) => res.json("result.type") === type,
  });
}

function createStatusTimeline(params) {
  const book = createUserBook(params, { title: `k6 timeline status ${__ITER}` }, "timeline-producers:create-status-book");
  const response = patch(
    "/api/v1/library/status",
    {
      bookId: book.bookId,
      readingStatus: "READING",
    },
    {
      ...params,
      tags: { name: "timeline-producers:status-change" },
    }
  );

  requireApiResponse(response, {
    label: "timeline producers status change",
    statuses: [200],
    requireResult: true,
  });

  const preview = latestTimelinePreview(params, book.libraryId);
  assertTimelineDetail(params, book.libraryId, findTimelineItemByType(preview, "STATUS"), "STATUS");
}

function createRecordTimeline(params) {
  const book = createUserBook(params, { title: `k6 timeline record ${__ITER}` }, "timeline-producers:create-record-book");
  const response = post(
    `/api/v1/records/books/${book.bookId}`,
    {
      content: `k6 timeline record ${__ITER}`,
      emotion: "FUN",
      imageKeys: [],
    },
    {
      ...params,
      tags: { name: "timeline-producers:create-record" },
    }
  );

  const recordOk = checkApiResponse(response, {
    label: "timeline producers create record",
    statuses: [201, 200],
  });
  if (!recordOk) {
    throw new Error(`timeline producers create record failed. status=${response.status}`);
  }

  const preview = latestTimelinePreview(params, book.libraryId);
  assertTimelineDetail(params, book.libraryId, findTimelineItemByType(preview, "RECORD"), "RECORD");
}

function createFocusTimeline(params) {
  const themes = get("/api/v1/focuses/themes", {
    ...params,
    tags: { name: "timeline-producers:themes" },
  });
  const themesOk = checkApiResponse(themes, {
    label: "timeline producers themes",
    statuses: [200],
    requireResult: true,
  });
  if (!themesOk) {
    throw new Error(`timeline producers themes failed. status=${themes.status}`);
  }

  const theme = (themes.json("result.themes") || [])[0];
  if (!theme?.themeId) {
    console.warn("No focus themes found. Skipping FOCUS timeline producer.");
    return;
  }

  const book = createUserBook(params, { title: `k6 timeline focus ${__ITER}` }, "timeline-producers:create-focus-book");
  const start = post(
    "/api/v1/focuses/start",
    {
      libraryId: book.libraryId,
      themeId: theme.themeId,
    },
    {
      ...params,
      tags: { name: "timeline-producers:focus-start" },
    }
  );
  const startOk = checkApiResponse(start, {
    label: "timeline producers focus start",
    statuses: [201, 200],
    requireResult: true,
  });
  if (!startOk) {
    throw new Error(`timeline producers focus start failed. status=${start.status}`);
  }

  const focusId = requirePositiveNumber(start.json("result.focusId"), "timeline producers focus start focusId");

  const end = post(
    "/api/v1/focuses/end",
    {
      focusId,
      page: 42,
      isFinished: false,
    },
    {
      ...params,
      tags: { name: "timeline-producers:focus-end" },
    }
  );
  const endOk = checkApiResponse(end, {
    label: "timeline producers focus end",
    statuses: [200],
    requireResult: true,
  });
  if (!endOk) {
    throw new Error(`timeline producers focus end failed. status=${end.status}`);
  }

  const preview = latestTimelinePreview(params, book.libraryId);
  assertTimelineDetail(params, book.libraryId, findTimelineItemByType(preview, "FOCUS"), "FOCUS");
}

export function setup() {
  return authenticateDevUser(scenarioUser("tlprod"));
}

export default function (auth) {
  const params = {
    headers: authHeaders(auth.accessToken),
  };

  createStatusTimeline(params);
  createRecordTimeline(params);
  createFocusTimeline(params);
}

export function handleSummary(data) {
  return createSummary(data);
}
