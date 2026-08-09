import { createSummary } from "../lib/summary.js";

export const options = {
  vus: 1,
  iterations: 1,
};

export default function () {}

export function handleSummary(data) {
  return createSummary(data);
}
