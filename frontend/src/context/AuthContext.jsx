import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { loginRequest, registerRequest } from "../services/authApi";

const AuthContext = createContext(null);
const STORAGE_KEY = "filenexus.auth";

function readStoredAuth() {
  try {
    const value = JSON.parse(localStorage.getItem(STORAGE_KEY) || "null");
    return value?.token ? value : null;
  } catch {
    return null;
  }
}

function normalizeLoginResponse(response) {
  const body = response?.data?.data ?? response?.data ?? response;
  const token =
    body?.token ?? body?.accessToken ?? body?.jwtToken ??
    body?.jwt ?? body?.access_token;

  if (!token) {
    throw new Error("Login response did not contain a JWT token. Check LoginResponse on the backend.");
  }

  const user = body?.user ?? body?.userResponse ?? {
    userId: body?.userId ?? body?.id,
    username: body?.username,
    email: body?.email,
    role: body?.role
  };

  return { token, user };
}

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(readStoredAuth);

  useEffect(() => {
    if (auth?.token) localStorage.setItem(STORAGE_KEY, JSON.stringify(auth));
    else localStorage.removeItem(STORAGE_KEY);
  }, [auth]);

  async function signIn(credentials) {
    const response = await loginRequest(credentials);
    const normalized = normalizeLoginResponse(response);
    setAuth(normalized);
    return normalized;
  }

  async function signUp(details) {
    return registerRequest(details);
  }

  function signOut() {
    setAuth(null);
  }

  const value = useMemo(() => ({
    token: auth?.token ?? null,
    user: auth?.user ?? null,
    isAuthenticated: Boolean(auth?.token),
    role: String(auth?.user?.role ?? "").replace(/^ROLE_/, "").toUpperCase(),
    signIn,
    signUp,
    signOut
  }), [auth]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside AuthProvider");
  return context;
}
