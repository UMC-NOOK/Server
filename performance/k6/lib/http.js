import http from "k6/http";

import { stringEnv } from "./env.js";

export const BASE_URL = stringEnv("BASE_URL", "http://localhost:8080").replace(/\/$/, "");

export function url(path) {
  if (path.startsWith("http://") || path.startsWith("https://")) {
    return path;
  }

  return `${BASE_URL}${path.startsWith("/") ? path : `/${path}`}`;
}

export function jsonHeaders(extraHeaders = {}) {
  return {
    "Content-Type": "application/json",
    ...extraHeaders,
  };
}

export function authHeaders(accessToken, extraHeaders = {}) {
  return jsonHeaders({
    Authorization: `Bearer ${accessToken}`,
    ...extraHeaders,
  });
}

export function queryString(params = {}) {
  return Object.entries(params)
    .filter(([, value]) => value !== undefined && value !== null && value !== "")
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
    .join("&");
}

export function withQuery(path, params = {}) {
  const qs = queryString(params);
  if (!qs) {
    return path;
  }

  return `${path}${path.includes("?") ? "&" : "?"}${qs}`;
}

export function request(method, path, body = null, params = {}) {
  const payload = body === null || body === undefined ? null : JSON.stringify(body);
  const requestParams = {
    ...params,
    headers: jsonHeaders(params.headers || {}),
  };

  return http.request(method, url(path), payload, requestParams);
}

export function get(path, params = {}) {
  return request("GET", path, null, params);
}

export function post(path, body = {}, params = {}) {
  return request("POST", path, body, params);
}

export function patch(path, body = {}, params = {}) {
  return request("PATCH", path, body, params);
}

export function del(path, params = {}) {
  return request("DELETE", path, null, params);
}
