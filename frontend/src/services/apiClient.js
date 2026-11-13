import axios from "axios";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "",
  timeout: 30000
});

api.interceptors.request.use((config) => {
  try {
    const stored = JSON.parse(localStorage.getItem("filenexus.auth") || "null");
    if (stored?.token) config.headers.Authorization = `Bearer ${stored.token}`;
  } catch {
    localStorage.removeItem("filenexus.auth");
  }
  return config;
});

api.interceptors.response.use(
  response => response,
  error => {
    // 403 means authenticated but not authorized. Never log out for 403.
    if (error.response?.status === 401) {
      const url = error.config?.url || "";
      if (!url.includes("/api/auth/login")) {
        localStorage.removeItem("filenexus.auth");
        if (window.location.pathname !== "/login") {
          window.location.replace("/login");
        }
      }
    }
    return Promise.reject(error);
  }
);

export const unwrapResponse = response => {
  const body = response?.data;
  return body && typeof body === "object" && "data" in body ? body.data : body;
};

export const getApiError = error =>
  error?.response?.data?.message ||
  error?.response?.data?.error ||
  error?.message ||
  "Something went wrong. Please try again.";

export default api;
