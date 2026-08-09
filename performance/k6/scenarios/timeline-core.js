import { check } from "k6";

import { internalApiOptions } from "../config/profiles.js";
import { authenticateDevUser } from "../lib/auth.js";
import { createUserBook } from "../lib/books.js";
import { checkApiResponse, requirePositiveNumber } from "../lib/checks.js";
import { scenarioUser } from "../lib/data.js";
import { authHeaders, get } from "../lib/http.js";
import { createSummary } from "../lib/summary.js";
import { findTimelineItemByType } from "../lib/timeline.js";

export const options = internalApiOptions;

export function setup() {
  return authenticateDevUser(scenarioUser("timeline"));
}

export default function (auth) {
  const params = {
    headers: authHeaders(auth.accessToken),
  };

  const book = createUserBook(params, { title: `k6 timeline core ${__ITER}` }, "timeline-core:create-book");

  const summary = get(`/api/v1/library/${book.libraryId}/timeline/summary`, {
    ...params,
    tags: { name: "timeline-core:summary" },
  });
  checkApiResponse(summary, {
    label: "timeline core summary",
    statuses: [200],
    requireResult: true,
  });
  check(summary, {
    "timeline core summary: same library": (res) => res.json("result.libraryId") === book.libraryId,
    "timeline core summary: has preview": (res) => {
      const groups = res.json("result.timelinePreview.dateGroups") || [];
      return groups.length > 0;
    },
  });

  const preview = get(`/api/v1/library/${book.libraryId}/timeline`, {
    ...params,
    tags: { name: "timeline-core:preview" },
  });
  checkApiResponse(preview, {
    label: "timeline core preview",
    statuses: [200],
    requireResult: true,
  });

  const registerItem = findTimelineItemByType(preview.json("result"), "REGISTER");
  const registerOk = check(preview, {
    "timeline core preview: has REGISTER": () => Boolean(registerItem?.timelineId),
  });
  if (!registerOk) {
    throw new Error("timeline core preview did not contain REGISTER item");
  }
  const registerTimelineId = requirePositiveNumber(registerItem.timelineId, "timeline core REGISTER timelineId");

  const detail = get(`/api/v1/library/${book.libraryId}/timeline/${registerTimelineId}`, {
    ...params,
    tags: { name: "timeline-core:register-detail" },
  });
  checkApiResponse(detail, {
    label: "timeline core register detail",
    statuses: [200],
    requireResult: true,
  });
  check(detail, {
    "timeline core register detail: type REGISTER": (res) => res.json("result.type") === "REGISTER",
  });
}

export function handleSummary(data) {
  return createSummary(data);
}
