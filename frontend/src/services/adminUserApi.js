import api, { unwrapResponse } from "./apiClient";
const unwrap = unwrapResponse;
export async function getAdminUsers() {
  return unwrap(await api.get("/api/admin/users"));
}
export async function updateAdminUserStatus(userId, active) {
  return unwrap(await api.patch(`/api/admin/users/${userId}/status`, { active }));
}
export async function updateAdminUserRole(userId, role) {
  return unwrap(await api.patch(`/api/admin/users/${userId}/role`, { role }));
}
