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

export const api = axios.create({ baseURL, withCredentials: true });
const refreshClient = axios.create({ baseURL, withCredentials: true });

export function setAccessToken(token: string | null) {
  accessToken = token;
}

export function refreshSession(): Promise<AuthSession> {
  if (!pendingRefresh) {
    pendingRefresh = refreshClient
      .post<AuthSession>("/auth/refresh")
      .then(({ data }) => {
        setAccessToken(data.accessToken);
        return data;
      })
      .finally(() => {
        pendingRefresh = null;
      });
  }
  return pendingRefresh;
}

export async function endSession() {
  await refreshClient.post("/auth/logout");
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
