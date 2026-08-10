import { seedOptions } from "../config/profiles.js";
import { authenticateExistingUser } from "../lib/auth.js";
import { seedUser } from "../lib/data.js";
import { checkExactSeedCounts, readSeedCounts } from "../lib/seedCardinality.js";
import { createSummary } from "../lib/summary.js";

export const options = seedOptions;

export function setup() {
  return authenticateExistingUser(seedUser(), { allowConfiguredToken: false });
}

export default function (auth) {
  const countsOk = checkExactSeedCounts(readSeedCounts(auth.accessToken), "seed reuse");
  if (!countsOk) {
    throw new Error("seed reuse cardinality verification failed");
  }
}

export function handleSummary(data) {
  return createSummary(data);
}
