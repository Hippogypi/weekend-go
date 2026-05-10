import type { RouteRecordRaw } from 'vue-router';

export const appRoutes = [
  {
    path: '/',
    name: 'home',
    component: () => import('../views/HomeView.vue'),
    meta: { title: '地点发现' }
  },
  {
    path: '/places/:placeId',
    name: 'place-detail',
    component: () => import('../views/PlaceDetailView.vue'),
    meta: { title: '地点详情' }
  },
  {
    path: '/places/:placeId/contribute',
    name: 'contribute',
    component: () => import('../views/ContributeView.vue'),
    meta: { title: '贡献信息', requiresAuth: true }
  },
  {
    path: '/places/:placeId/contribute/checkin',
    name: 'contribute-checkin',
    component: () => import('../views/ContributeCheckinView.vue'),
    meta: { title: '打卡反馈', requiresAuth: true }
  },
  {
    path: '/places/:placeId/contribute/review',
    name: 'contribute-review',
    component: () => import('../views/ContributeReviewView.vue'),
    meta: { title: '写评价', requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('../views/ProfileView.vue'),
    meta: { title: '账号与收藏', requiresAuth: true }
  },
  {
    path: '/admin',
    name: 'admin',
    component: () => import('../views/AdminDashboardView.vue'),
    meta: { title: '审核工作台', requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: { title: '登录' }
  }
] satisfies RouteRecordRaw[];
