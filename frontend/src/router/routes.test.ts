// @vitest-environment jsdom

import { beforeEach, describe, expect, it, vi } from 'vitest';

import { appRoutes } from './routes';

describe('appRoutes', () => {
  it('defines the required bootstrap route placeholders', () => {
    expect(appRoutes.map((route) => route.path)).toEqual([
      '/',
      '/places/:placeId',
      '/places/:placeId/contribute',
      '/places/:placeId/contribute/checkin',
      '/places/:placeId/contribute/review',
      '/profile',
      '/admin',
      '/login'
    ]);
  });

  it('uses stable route names for later feature integration', () => {
    expect(appRoutes.map((route) => route.name)).toEqual([
      'home',
      'place-detail',
      'contribute',
      'contribute-checkin',
      'contribute-review',
      'profile',
      'admin',
      'login'
    ]);
  });

  it('marks protected routes with requiresAuth', () => {
    const protectedRoutes = appRoutes.filter((r) => r.meta?.requiresAuth);
    expect(protectedRoutes.map((r) => r.path)).toEqual([
      '/places/:placeId/contribute',
      '/places/:placeId/contribute/checkin',
      '/places/:placeId/contribute/review',
      '/profile',
      '/admin'
    ]);
  });

  it('marks admin routes with requiresAdmin', () => {
    const adminRoutes = appRoutes.filter((r) => r.meta?.requiresAdmin);
    expect(adminRoutes.map((r) => r.path)).toEqual(['/admin']);
  });
});

describe('router beforeEach guard', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('redirects unauthenticated users from protected routes to login with redirect', async () => {
    const { router } = await import('./index');
    const { sessionStore } = await import('../services');

    vi.spyOn(sessionStore, 'isLoggedIn', 'get').mockReturnValue({ value: false } as never);

    await router.push('/profile');
    expect(router.currentRoute.value.path).toBe('/login');
    expect(router.currentRoute.value.query.redirect).toBe('/profile');
  });

  it('redirects logged-in users away from /login to home', async () => {
    const { router } = await import('./index');
    const { sessionStore } = await import('../services');

    vi.spyOn(sessionStore, 'isLoggedIn', 'get').mockReturnValue({ value: true } as never);

    await router.push('/login');
    expect(router.currentRoute.value.path).toBe('/');
  });
});
