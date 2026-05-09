import { beforeEach, describe, expect, it } from 'vitest';

const memoryStorage = (() => {
  let data: Record<string, string> = {};
  return {
    clear: () => {
      data = {};
    },
    getItem: (key: string) => data[key] ?? null,
    removeItem: (key: string) => {
      delete data[key];
    },
    setItem: (key: string, value: string) => {
      data[key] = value;
    }
  };
})();

Object.defineProperty(globalThis, 'localStorage', {
  value: memoryStorage,
  configurable: true
});

describe('SessionStore', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('persists and restores the current token and user', async () => {
    const { createSessionStore } = await import('./session');
    const session = createSessionStore();

    session.setSession({
      token: 'token-1',
      user: { id: 1, username: 'admin', role: 'ADMIN', nickname: 'Admin' }
    });

    const restored = createSessionStore();

    expect(restored.token.value).toBe('token-1');
    expect(restored.user.value?.role).toBe('ADMIN');
    expect(restored.isAdmin.value).toBe(true);
  });

  it('clears invalid stored session data', async () => {
    const { createSessionStore } = await import('./session');
    localStorage.setItem('weekend-go-session', '{bad json');

    const session = createSessionStore();

    expect(session.token.value).toBeNull();
    expect(session.user.value).toBeNull();
    expect(localStorage.getItem('weekend-go-session')).toBeNull();
  });
});
