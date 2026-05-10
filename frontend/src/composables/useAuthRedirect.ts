import type { Router } from 'vue-router';
import { useRoute } from 'vue-router';

export function useAuthRedirect() {
  const route = useRoute();

  function getRedirectPath(): string {
    const redirect = route.query.redirect;
    return typeof redirect === 'string' && redirect.startsWith('/') ? redirect : '/';
  }

  function navigateAfterLogin(router: Router): void {
    const redirect = getRedirectPath();
    router.push(redirect);
  }

  return {
    getRedirectPath,
    navigateAfterLogin
  };
}
