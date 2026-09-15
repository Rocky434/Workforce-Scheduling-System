import { defineStore } from "pinia";
import { ref } from "vue";
import {
  api,
  apiError,
  endSession,
  refreshSession,
  setAccessToken,
  type AuthSession,
} from "../api";
import type { User } from "../types";

export const useAuthStore = defineStore("auth", () => {
  const user = ref<User | null>(null);
  let initialized = false;
  let initialization: Promise<void> | null = null;

  function applySession(session: AuthSession) {
    setAccessToken(session.accessToken);
    user.value = session.user;
  }

  function clearSession() {
    setAccessToken(null);
    user.value = null;
  }

  async function initialize() {
    if (initialized) return;
    if (!initialization) {
      initialization = refreshSession()
        .then(applySession)
        .catch(clearSession)
        .finally(() => {
          initialized = true;
          initialization = null;
        });
    }
    await initialization;
  }

  async function login(email: string, password: string) {
    try {
      const { data } = await api.post<AuthSession>("/auth/login", { email, password });
      applySession(data);
      initialized = true;
    } catch (error) {
      throw apiError(error);
    }
  }

  async function logout() {
    try {
      await endSession();
    } catch {
      // The browser must still forget the in-memory session if the API is unavailable.
    } finally {
      clearSession();
      initialized = true;
    }
  }

  window.addEventListener("auth:expired", clearSession);

  return { user, initialize, login, logout };
});
