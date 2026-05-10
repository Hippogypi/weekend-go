import { createRouter, createWebHistory } from 'vue-router';

import { getGlobalToastService } from '../composables/useToast';
import { sessionStore } from '../services';

import { appRoutes } from './routes';

export const router = createRouter({
  history: createWebHistory(),
  routes: appRoutes
});

router.beforeEach((to, _from, next) => {
  document.title = `${to.meta.title as string} - weekend-go`;

  if (to.path === '/login' && sessionStore.isLoggedIn.value) {
    next({ path: '/' });
    return;
  }

  if (to.meta.requiresAuth && !sessionStore.isLoggedIn.value) {
    next({ path: '/login', query: { redirect: to.fullPath } });
    return;
  }

  if (to.meta.requiresAdmin && !sessionStore.isAdmin.value) {
    getGlobalToastService().showToast('权限不足', 'error');
    next({ path: '/' });
    return;
  }

  next();
});
