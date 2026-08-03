export function stringEnv(name, fallback = "") {
  const value = __ENV[name];
  if (value === undefined || value === null || String(value).trim() === "") {
    return fallback;
  }
  return String(value);
}

export function intEnv(name, fallback) {
  const rawValue = stringEnv(name);
  if (rawValue === "") {
    return fallback;
  }

  const parsed = Number.parseInt(rawValue, 10);
  return Number.isFinite(parsed) ? parsed : fallback;
}

export function floatEnv(name, fallback) {
  const rawValue = stringEnv(name);
  if (rawValue === "") {
    return fallback;
  }

  const parsed = Number.parseFloat(rawValue);
  return Number.isFinite(parsed) ? parsed : fallback;
}

export function boolEnv(name, fallback = false) {
  const rawValue = stringEnv(name);
  if (rawValue === "") {
    return fallback;
  }

  return ["1", "true", "yes", "y", "on"].includes(rawValue.toLowerCase());
}

export function runId() {
  return stringEnv("RUN_ID", `${Date.now()}`).replace(/[^a-zA-Z0-9_-]/g, "");
}

export function reportName(defaultName = "summary") {
  return stringEnv("K6_REPORT_NAME", defaultName).replace(/[^a-zA-Z0-9_-]/g, "");
}
