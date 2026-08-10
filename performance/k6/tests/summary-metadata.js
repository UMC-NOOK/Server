import { createSummary } from "../lib/summary.js";

if (__ENV.K6_TEST_INCREMENTING_CLOCK === "yes") {
  let now = 1000;
  Date.now = () => {
    now += 1;
    return now;
  };
}

export const options = {
  vus: 1,
  iterations: 1,
};

export default function () {}

export function handleSummary(data) {
  return createSummary(data);
}
