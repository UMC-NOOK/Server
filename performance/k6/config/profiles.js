import { floatEnv, intEnv, runId, stringEnv } from "../lib/env.js";

const DEFAULT_STAGES = [
  { target: 10, duration: "2m" },
  { target: 20, duration: "2m" },
  { target: 40, duration: "2m" },
  { target: 0, duration: "30s" },
];

const MIXED_READ_REQUEST_NAMES = [
  "read:book-detail",
  "read:books-search-library",
  "read:books-search-library-home",
  "read:library-books",
  "read:library-status-before",
  "read:library-count",
  "read:library-before-reading",
  "read:library-years",
  "read:library-recent-focus",
  "read:records-list",
  "read:records-book-list",
  "read:records-emotions",
  "read:records-detail",
  "read:focus-themes",
  "read:focus-recent",
  "read:timeline-summary",
  "read:timeline-preview",
  "read:timeline-detail",
];

function commonThresholds({
  p95Ms,
  failedRate,
  requestNames = [],
  maxDroppedIterations,
  aggregateRequestThresholds = true,
}) {
  const thresholds = {
    checks: ["rate>0.99"],
  };

  if (aggregateRequestThresholds) {
    thresholds.http_req_failed = [`rate<${failedRate}`];
    thresholds.http_req_duration = [`p(95)<${p95Ms}`];
  }

  if (maxDroppedIterations !== undefined) {
    thresholds.dropped_iterations = [`count<=${maxDroppedIterations}`];
  }

  return requestNames.reduce((requestThresholds, requestName) => ({
    ...requestThresholds,
    [`http_req_duration{name:${requestName}}`]: [`p(95)<${p95Ms}`],
    [`http_req_failed{name:${requestName}}`]: [`rate<${failedRate}`],
  }), thresholds);
}

function singleApiThresholds(requestName) {
  if (!requestName) {
    throw new Error("single-API options require a request name tag");
  }

  return commonThresholds({
    failedRate: floatEnv("FAILED_RATE_THRESHOLD", 0.01),
    p95Ms: intEnv("P95_THRESHOLD_MS", 1000),
    requestNames: [requestName],
    maxDroppedIterations: intEnv("MAX_DROPPED_ITERATIONS", 0),
    aggregateRequestThresholds: false,
  });
}

function commonTags() {
  return {
    run_id: runId(),
    test_name: stringEnv("K6_REPORT_NAME", "k6"),
  };
}

function parseStages(rawStages) {
  if (!rawStages) {
    return DEFAULT_STAGES;
  }

  const stages = rawStages
    .split(",")
    .map((stage) => {
      const [target, duration] = stage.split(":");
      return {
        target: Number.parseInt(target, 10),
        duration,
      };
    })
    .filter((stage) => Number.isFinite(stage.target) && Boolean(stage.duration));

  return stages.length > 0 ? stages : DEFAULT_STAGES;
}

export const smokeOptions = {
  tags: commonTags(),
  scenarios: {
    smoke: {
      executor: "shared-iterations",
      vus: intEnv("VUS", 1),
      iterations: intEnv("ITERATIONS", 1),
      maxDuration: stringEnv("MAX_DURATION", "30s"),
    },
  },
  thresholds: commonThresholds({
    failedRate: floatEnv("FAILED_RATE_THRESHOLD", 0.001),
    p95Ms: intEnv("P95_THRESHOLD_MS", 500),
  }),
};

export const internalApiOptions = {
  tags: commonTags(),
  scenarios: {
    internal_api: {
      executor: "shared-iterations",
      vus: intEnv("VUS", 1),
      iterations: intEnv("ITERATIONS", 1),
      maxDuration: stringEnv("MAX_DURATION", "1m"),
    },
  },
  thresholds: commonThresholds({
    failedRate: floatEnv("FAILED_RATE_THRESHOLD", 0.01),
    p95Ms: intEnv("P95_THRESHOLD_MS", 800),
  }),
};

export const seedOptions = {
  tags: commonTags(),
  scenarios: {
    seed: {
      executor: "shared-iterations",
      vus: intEnv("VUS", 1),
      iterations: intEnv("ITERATIONS", 1),
      maxDuration: stringEnv("MAX_DURATION", "5m"),
    },
  },
  thresholds: commonThresholds({
    failedRate: floatEnv("FAILED_RATE_THRESHOLD", 0.01),
    p95Ms: intEnv("P95_THRESHOLD_MS", 1200),
  }),
};

export function singleApiRampingArrivalRateOptions(requestName) {
  return {
    tags: commonTags(),
    scenarios: {
      api_bottleneck: {
        executor: "ramping-arrival-rate",
        startRate: intEnv("START_RPS", 1),
        timeUnit: "1s",
        preAllocatedVUs: intEnv("PRE_ALLOCATED_VUS", 20),
        maxVUs: intEnv("MAX_VUS", 200),
        stages: parseStages(stringEnv("RPS_STAGES")),
      },
    },
    thresholds: singleApiThresholds(requestName),
  };
}

export function singleApiArrivalRateOptions(requestName) {
  return {
    tags: commonTags(),
    scenarios: {
      steady_state: {
        executor: "constant-arrival-rate",
        rate: intEnv("TARGET_RPS", 10),
        timeUnit: "1s",
        duration: stringEnv("DURATION", "10m"),
        preAllocatedVUs: intEnv("PRE_ALLOCATED_VUS", 20),
        maxVUs: intEnv("MAX_VUS", 200),
      },
    },
    thresholds: singleApiThresholds(requestName),
  };
}

export function cacheColdOptions(requestName) {
  return {
    tags: commonTags(),
    scenarios: {
      cache_cold: {
        executor: "shared-iterations",
        vus: intEnv("VUS", 1),
        iterations: intEnv("ITERATIONS", 1),
        maxDuration: stringEnv("MAX_DURATION", "1m"),
      },
    },
    thresholds: singleApiThresholds(requestName),
  };
}

export function cacheWarmOptions(requestName) {
  return {
    tags: commonTags(),
    scenarios: {
      cache_warm: {
        executor: "constant-arrival-rate",
        rate: intEnv("TARGET_RPS", 10),
        timeUnit: "1s",
        duration: stringEnv("DURATION", "10m"),
        preAllocatedVUs: intEnv("PRE_ALLOCATED_VUS", 20),
        maxVUs: intEnv("MAX_VUS", 200),
      },
    },
    thresholds: singleApiThresholds(requestName),
  };
}

export const mixedReadJourneyOptions = {
  tags: commonTags(),
  scenarios: {
    mixed_read_journey: {
      executor: "constant-arrival-rate",
      rate: intEnv("JOURNEYS_PER_SECOND", 1),
      timeUnit: stringEnv("TIME_UNIT", "1s"),
      duration: stringEnv("DURATION", "10m"),
      preAllocatedVUs: intEnv("PRE_ALLOCATED_VUS", 20),
      maxVUs: intEnv("MAX_VUS", 200),
    },
  },
  thresholds: commonThresholds({
    failedRate: floatEnv("FAILED_RATE_THRESHOLD", 0.01),
    p95Ms: intEnv("P95_THRESHOLD_MS", 1000),
    requestNames: MIXED_READ_REQUEST_NAMES,
    maxDroppedIterations: intEnv("MAX_DROPPED_ITERATIONS", 0),
  }),
};

export const externalApiOptions = {
  tags: commonTags(),
  scenarios: {
    external_api: {
      executor: "constant-arrival-rate",
      rate: intEnv("TARGET_RPS", 1),
      timeUnit: stringEnv("TIME_UNIT", "1s"),
      duration: stringEnv("DURATION", "1m"),
      preAllocatedVUs: intEnv("PRE_ALLOCATED_VUS", 5),
      maxVUs: intEnv("MAX_VUS", 20),
    },
  },
  thresholds: {
    "http_req_failed{name:books-search:global-first}": [`rate<${floatEnv("FAILED_RATE_THRESHOLD", 0.05)}`],
    "http_req_failed{name:books-search:global-next}": [`rate<${floatEnv("FAILED_RATE_THRESHOLD", 0.05)}`],
    "http_req_duration{name:books-search:global-first}": [`p(95)<${intEnv("P95_THRESHOLD_MS", 5000)}`],
    "http_req_duration{name:books-search:global-next}": [`p(95)<${intEnv("P95_THRESHOLD_MS", 5000)}`],
    "checks{scope:global-search}": ["rate>0.99"],
  },
};
