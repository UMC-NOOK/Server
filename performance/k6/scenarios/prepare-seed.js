import { seedOptions } from "../config/profiles.js";
import { authenticateDevUser } from "../lib/auth.js";
import { scenarioUser } from "../lib/data.js";
import { seedUserDataset } from "../lib/seed.js";
import { createSummary } from "../lib/summary.js";

export const options = seedOptions;

export function setup() {
  return authenticateDevUser(scenarioUser("seed"));
}

export default function (auth) {
  seedUserDataset(auth);
}

export function handleSummary(data) {
  return createSummary(data);
}
