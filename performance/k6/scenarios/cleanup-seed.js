import { check } from "k6";

import { seedOptions } from "../config/profiles.js";
import { authenticateExistingUser } from "../lib/auth.js";
import { requireApiResponse } from "../lib/checks.js";
import { seedUser } from "../lib/data.js";
import { authHeaders, del } from "../lib/http.js";
import { createSummary } from "../lib/summary.js";

export const options = seedOptions;

export function setup() {
  return authenticateExistingUser(seedUser(), {
    allowNotFound: true,
    allowConfiguredToken: false,
  });
}

export default function (auth) {
  if (!auth) {
    check(null, {
      "seed cleanup: user already absent": () => true,
    });
    return;
  }

  const response = del("/api/v1/auth/dev/withdraw", {
    headers: authHeaders(auth.accessToken),
    tags: { name: "seed:cleanup-user" },
  });
  requireApiResponse(response, {
    label: "seed cleanup user",
    statuses: [200],
  });
}

export function handleSummary(data) {
  return createSummary(data);
}
