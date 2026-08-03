import { check } from "k6";

function expectedStatusLabel(expectedStatuses) {
  return expectedStatuses.join("/");
}

export function checkStatus(response, expectedStatuses, label, tags = {}) {
  const statuses = Array.isArray(expectedStatuses) ? expectedStatuses : [expectedStatuses];

  return check(response, {
    [`${label}: status is ${expectedStatusLabel(statuses)}`]: (res) => statuses.includes(res.status),
  }, tags);
}

export function checkApiSuccess(response, label, tags = {}) {
  return check(response, {
    [`${label}: response is successful`]: (res) => {
      try {
        return res.json("isSuccess") === true;
      } catch (_) {
        return false;
      }
    },
  }, tags);
}

export function checkApiResult(response, label, tags = {}) {
  return check(response, {
    [`${label}: result exists`]: (res) => {
      try {
        return res.json("result") !== undefined && res.json("result") !== null;
      } catch (_) {
        return false;
      }
    },
  }, tags);
}

export function checkApiResponse(response, { label, statuses = [200], requireResult = false }, tags = {}) {
  const statusOk = checkStatus(response, statuses, label, tags);
  const successOk = checkApiSuccess(response, label, tags);
  const resultOk = requireResult ? checkApiResult(response, label, tags) : true;

  return statusOk && successOk && resultOk;
}

export function requireApiResponse(response, options, tags = {}) {
  const ok = checkApiResponse(response, options, tags);
  if (!ok) {
    throw new Error(`${options.label} failed. status=${response.status}`);
  }

  return response;
}

export function requirePositiveNumber(value, label) {
  if (typeof value !== "number" || value <= 0) {
    throw new Error(`${label} is required`);
  }

  return value;
}
