import { describe, expect, it, vi } from 'vitest';

const mockQuery: Record<string, unknown> = {};

vi.mock('vue-router', () => ({
  useRoute: () => ({
    query: mockQuery
  })
}));

import { useAuthRedirect } from './useAuthRedirect';

describe('useAuthRedirect', () => {
  it('defaults redirect to / when no query param', () => {
    mockQuery.redirect = undefined;
    const { getRedirectPath } = useAuthRedirect();
    expect(getRedirectPath()).toBe('/');
  });

  it('reads redirect from query when present', () => {
    mockQuery.redirect = '/places/123';
    const { getRedirectPath } = useAuthRedirect();
    expect(getRedirectPath()).toBe('/places/123');
  });

  it('ignores non-string redirect values', () => {
    mockQuery.redirect = 123;
    const { getRedirectPath } = useAuthRedirect();
    expect(getRedirectPath()).toBe('/');
  });

  it('ignores redirect that does not start with /', () => {
    mockQuery.redirect = 'https://evil.com';
    const { getRedirectPath } = useAuthRedirect();
    expect(getRedirectPath()).toBe('/');
  });

  it('navigates to redirect path', () => {
    mockQuery.redirect = '/profile';
    const { navigateAfterLogin } = useAuthRedirect();
    const push = vi.fn();
    navigateAfterLogin({ push } as unknown as Parameters<typeof navigateAfterLogin>[0]);
    expect(push).toHaveBeenCalledWith('/profile');
  });
});
