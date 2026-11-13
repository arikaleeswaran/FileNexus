import api, { unwrapResponse, getApiError } from "./apiClient";

export const errorMessage = getApiError;
const unwrap = unwrapResponse;

export async function listFiles(params = {}) {
  return unwrap(await api.get("/api/files", {
    params: {
      page: params.page ?? 0,
      size: params.size ?? 100,
      sortBy: params.sortBy ?? "uploadedAt",
      sortDir: params.sortDir ?? "desc",
      ...(params.status ? { status: params.status } : {}),
      ...(params.keyword ? { keyword: params.keyword } : {})
    }
  }));
}

export async function getFile(id) { return unwrap(await api.get(`/api/files/${id}`)); }

export async function createMetadata(file) {
  return unwrap(await api.post("/api/files/metadata", {
    originalName: file.name,
    fileSize: file.size,
    declaredMimeType: file.type || "application/octet-stream"
  }));
}

export async function uploadFile(id, file, onProgress) {
  const form = new FormData();
  form.append("file", file);
  return unwrap(await api.post(`/api/files/${id}/upload`, form, {
    onUploadProgress: event => {
      if (event.total && onProgress) onProgress(Math.round(event.loaded * 100 / event.total));
    }
  }));
}

export async function downloadFile(id, fallback = "download") {
  const response = await api.get(`/api/files/${id}/download`, { responseType: "blob" });
  const disposition = response.headers["content-disposition"] || "";
  const match = disposition.match(/filename\\*=UTF-8''([^;]+)/i) || disposition.match(/filename="?([^";]+)"?/i);
  const filename = match ? decodeURIComponent(match[1]) : fallback;
  const url = URL.createObjectURL(response.data);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

export async function getValidation(id) { return unwrap(await api.get(`/api/files/${id}/validation-history`)); }
export async function getScans(id) { return unwrap(await api.get(`/api/scan-jobs/files/${id}`)); }
export async function getAudit(id) { return unwrap(await api.get(`/api/audit-logs/files/${id}`)); }
