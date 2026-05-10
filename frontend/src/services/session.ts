import { computed, ref } from 'vue';

import type { WeekendGoApi } from './weekendGoApi';

export interface UserProfile {
  id: number;
  username: string;
  role: 'USER' | 'ADMIN' | string;
  nickname: string;
}

export interface AuthSession {
  token: string;
  user: UserProfile;
}

const STORAGE_KEY = 'weekend-go-session';

function loadStoredSession(): AuthSession | null {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) {
    return null;
  }

  try {
    const parsed = JSON.parse(raw) as AuthSession;
    if (!parsed.token || !parsed.user?.username) {
      throw new Error('Invalid session');
    }
    return parsed;
  } catch {
    localStorage.removeItem(STORAGE_KEY);
    localStorage.removeItem('weekend-go.access-token');
    return null;
  }
}

export function createSessionStore() {
  const stored = loadStoredSession();
  const token = ref<string | null>(stored?.token ?? localStorage.getItem('weekend-go.access-token'));
  const user = ref<UserProfile | null>(stored?.user ?? null);

  function setSession(session: AuthSession): void {
    token.value = session.token;
    user.value = session.user;
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    localStorage.setItem('weekend-go.access-token', session.token);
  }

  function updateUser(updated: UserProfile): void {
    if (user.value) {
      user.value = updated;
      const stored = loadStoredSession();
      if (stored) {
        stored.user = updated;
        localStorage.setItem(STORAGE_KEY, JSON.stringify(stored));
      }
    }
  }

  function clearSession(): void {
    token.value = null;
    user.value = null;
    localStorage.removeItem(STORAGE_KEY);
    localStorage.removeItem('weekend-go.access-token');
  }

  async function restoreSession(api: WeekendGoApi): Promise<boolean> {
    if (!token.value) {
      return false;
    }

    try {
      const profile = await api.me();
      user.value = profile;
      return true;
    } catch {
      clearSession();
      return false;
    }
  }

  return {
    token,
    user,
    isLoggedIn: computed(() => Boolean(token.value && user.value)),
    isAdmin: computed(() => user.value?.role === 'ADMIN'),
    setSession,
    updateUser,
    clearSession,
    restoreSession
  };
}

export type SessionStore = ReturnType<typeof createSessionStore>;

export const sessionStore = createSessionStore();
