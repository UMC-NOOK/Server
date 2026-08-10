import { check } from "k6";

import { countTimelineItems, expectedSeedCounts } from "../lib/seedCardinality.js";

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    checks: ["rate==1"],
  },
};

export default function () {
  const expected = expectedSeedCounts();
  const timelineCount = countTimelineItems({
    dateGroups: [
      { items: [{ timelineId: 1 }, { timelineId: 2 }] },
      { items: [{ timelineId: 3 }] },
    ],
  });

  check({ expected, timelineCount }, {
    "seed cardinality: expected total timeline count": (value) => value.expected.timelines === 17,
    "seed cardinality: timeline groups are flattened": (value) => value.timelineCount === 3,
  });
}
