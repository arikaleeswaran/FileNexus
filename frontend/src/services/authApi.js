import api, { unwrapResponse } from "./apiClient";

export async function loginRequest(credentials) {
  return api.post("/api/auth/login", credentials);
}

export async function registerRequest(details) {
  return unwrapResponse(await api.post("/api/auth/register", details));
}
