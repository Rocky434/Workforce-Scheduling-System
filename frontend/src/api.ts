import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios";
import type { User } from "./types";

export interface AuthSession {
  accessToken: string;
  user: User;
}

type RetryableRequest = InternalAxiosRequestConfig & { _retriedAfterRefresh?: boolean };

const baseURL = import.meta.env.VITE_API_URL ?? "/api";
let accessToken: string | null = null;
let pendingRefresh: Promise<AuthSession> | null = null;
let endingSession = false;

const clientOptions = {
  baseURL,
  withCredentials: true,
  headers: { "X-Requested-With": "XMLHttpRequest" },
};
export const api = axios.create(clientOptions);
const refreshClient = axios.create(clientOptions);

export function setAccessToken(token: string | null) {
  accessToken = token;
}

export function refreshSession(): Promise<AuthSession> {
  if (endingSession) {
    return Promise.reject(new Error("正在登出"));
  }
  if (!pendingRefresh) {
    const request = () => refreshClient.post<AuthSession>("/auth/refresh");
    const response = "locks" in navigator
      ? navigator.locks.request("scheduling-refresh-token", request)
      : request();
    pendingRefresh = response
      .then(({ data }) => {
        setAccessToken(data.accessToken);
        window.dispatchEvent(new CustomEvent<AuthSession>("auth:refreshed", { detail: data }));
        return data;
      })
      .finally(() => {
        pendingRefresh = null;
      });
  }
  return pendingRefresh;
}

export async function endSession() {
  endingSession = true;
  try {
    if (pendingRefresh) {
      await pendingRefresh.catch(() => undefined);
    }
    await refreshClient.post("/auth/logout");
  } finally {
    setAccessToken(null);
    endingSession = false;
  }
}

export function apiError(error: unknown) {
  if (axios.isAxiosError(error)) {
    return new Error(error.response?.data?.message ?? "連線失敗，請稍後再試");
  }
  return error instanceof Error ? error : new Error("連線失敗，請稍後再試");
}

api.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const request = error.config as RetryableRequest | undefined;
    const isAuthEndpoint = request?.url?.startsWith("/auth/");

    if (error.response?.status === 401 && request && !request._retriedAfterRefresh && !isAuthEndpoint) {
      request._retriedAfterRefresh = true;
      try {
        const session = await refreshSession();
        request.headers.Authorization = `Bearer ${session.accessToken}`;
        return api(request);
      } catch (refreshError) {
        setAccessToken(null);
        window.dispatchEvent(new Event("auth:expired"));
        return Promise.reject(apiError(refreshError));
      }
    }

    return Promise.reject(apiError(error));
  },
);
