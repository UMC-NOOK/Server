import { check } from "k6";

import { setup as cleanupSeed } from "../scenarios/cleanup-seed.js";
import { setup as verifySeed } from "../scenarios/verify-seed.js";

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    checks: ["rate==1"],
  },
};

function rejectsConfiguredToken(setupSeed) {
  try {
    setupSeed();
  } catch (error) {
    return String(error).includes("does not accept a configured token");
  }
  return false;
}

export default function () {
  check(null, {
    "verify-seed rejects a configured token": () => rejectsConfiguredToken(verifySeed),
    "cleanup-seed rejects a configured token": () => rejectsConfiguredToken(cleanupSeed),
  });
}
