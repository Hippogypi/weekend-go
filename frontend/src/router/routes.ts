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
    path: '/profile',
    name: 'profile',
    component: () => import('../views/ProfileView.vue'),
    meta: { title: '账号与收藏' }
  },
  {
    path: '/admin/reviews',
    name: 'admin-reviews',
    component: () => import('../views/AdminReviewView.vue'),
    meta: { title: '管理员审核' }
  }
] satisfies RouteRecordRaw[];
