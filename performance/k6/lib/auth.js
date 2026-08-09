import { check } from "k6";
import http from "k6/http";

import { checkApiResponse } from "./checks.js";
import { post } from "./http.js";
import { smokeUser } from "./data.js";
import { stringEnv } from "./env.js";

function extractToken(response, fieldName) {
  try {
    return response.json(`result.${fieldName}`);
  } catch (_) {
    return null;
  }
}

export function signupDevUser(user = smokeUser(), { allowDuplicate = false } = {}) {
  const response = post("/api/v1/auth/dev/signup", user, {
    tags: { name: "auth:dev-signup" },
    responseCallback: allowDuplicate ? http.expectedStatuses(200, 201, 409) : http.expectedStatuses(200, 201),
  });

  if (response.status === 201 || response.status === 200) {
    checkApiResponse(response, {
      label: "dev signup",
      statuses: [201, 200],
      requireResult: true,
    });
  } else if (allowDuplicate && response.status === 409) {
    check(response, {
      "dev signup: user already exists": () => true,
    });
  }

  return response;
}

export function loginDevUser(user = smokeUser(), { allowNotFound = false } = {}) {
  const response = post("/api/v1/auth/dev/login", user, {
    tags: { name: "auth:dev-login" },
    responseCallback: allowNotFound ? http.expectedStatuses(200, 404) : http.expectedStatuses(200),
  });

  if (allowNotFound && response.status === 404) {
    return null;
  }

  checkApiResponse(response, {
    label: "dev login",
    statuses: [200],
    requireResult: true,
  });

  const accessToken = extractToken(response, "accessToken");
  const refreshToken = extractToken(response, "refreshToken");

  if (!accessToken) {
    throw new Error(`DEV login did not return accessToken. status=${response.status}`);
  }

  return {
    user,
    accessToken,
    refreshToken,
  };
}

function resolveAuthUser(user, { useConfiguredUser = true } = {}) {
  if (!useConfiguredUser) {
    return user;
  }

  return {
    email: stringEnv("K6_USER_EMAIL") || user.email,
    nickName: stringEnv("K6_USER_NICKNAME") || user.nickName,
  };
}

export function authenticateDevUser(user = smokeUser(), { useConfiguredUser = true } = {}) {
  const authUser = resolveAuthUser(user, { useConfiguredUser });
  const accessToken = stringEnv("TOKEN") || stringEnv("K6_ACCESS_TOKEN");
  if (accessToken) {
    return {
      user: authUser,
      accessToken,
      refreshToken: stringEnv("K6_REFRESH_TOKEN") || null,
    };
  }

  const existingUserAuth = loginDevUser(authUser, { allowNotFound: true });
  if (existingUserAuth) {
    return existingUserAuth;
  }

  signupDevUser(authUser, { allowDuplicate: true });
  return loginDevUser(authUser);
}
