<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink, useRouter } from 'vue-router';

import { sessionStore, weekendGoApi } from '../services';
import type { FavoritePlace } from '../services';
import { useAsyncAction, useApiError } from '../composables';

const router = useRouter();
const { errorMessage, setError, clearError } = useApiError();

const isLoggedIn = computed(() => sessionStore.isLoggedIn.value);

const {
  loading: favoritesLoading,
  data: favorites,
  execute: loadFavorites
} = useAsyncAction<FavoritePlace[]>({
  onError: (msg) => setError(new Error(msg))
});

async function doLoadFavorites(): Promise<void> {
  if (!isLoggedIn.value) {
    return;
  }
  clearError();
  await loadFavorites(() => weekendGoApi.favorites());
}

async function logout(): Promise<void> {
  try {
    await weekendGoApi.logout();
  } catch {
    // Token may already be invalid; local logout should still complete.
  } finally {
    sessionStore.clearSession();
    router.push('/');
  }
}

if (isLoggedIn.value) {
  doLoadFavorites();
}
</script>

<template>
  <section class="page">
    <header class="page-header">
      <div>
        <h1 class="page-title">账号与收藏</h1>
        <p class="page-subtitle">管理个人信息和收藏地点。</p>
      </div>
      <span class="status-pill">{{ sessionStore.user.value?.role || 'Guest' }}</span>
    </header>

    <p v-if="errorMessage" class="notice error">{{ errorMessage }}</p>

    <div class="two-column">
      <article class="panel">
        <div class="section-heading">
          <h2>当前账号</h2>
          <button v-if="isLoggedIn" class="ghost-button" type="button" @click="logout">退出</button>
        </div>
        <div v-if="!isLoggedIn" class="login-prompt">
          <p class="muted">未登录。登录后可提交共建、打卡、评价、图片并管理收藏。</p>
          <button class="primary-button" type="button" @click="router.push('/login')">去登录</button>
        </div>
        <dl v-else class="meta-list vertical">
          <div>
            <dt>用户名</dt>
            <dd>{{ sessionStore.user.value?.username }}</dd>
          </div>
          <div>
            <dt>昵称</dt>
            <dd>{{ sessionStore.user.value?.nickname }}</dd>
          </div>
          <div>
            <dt>角色</dt>
            <dd>{{ sessionStore.user.value?.role }}</dd>
          </div>
        </dl>
      </article>

      <section class="panel">
        <div class="section-heading">
          <h2>我的收藏</h2>
          <button class="ghost-button" type="button" :disabled="!isLoggedIn || favoritesLoading" @click="doLoadFavorites">
            {{ favoritesLoading ? '加载中...' : '刷新' }}
          </button>
        </div>
        <div v-if="!isLoggedIn" class="login-prompt">
          <p class="muted">登录后查看收藏列表。</p>
          <button class="primary-button" type="button" @click="router.push('/login')">去登录</button>
        </div>
        <p v-else-if="!favorites || favorites.length === 0" class="muted">暂无收藏地点。</p>
        <ul v-else class="simple-list">
          <li v-for="favorite in favorites" :key="favorite.placeId">
            <RouterLink :to="`/places/${favorite.placeId}`">{{ favorite.placeName }}</RouterLink>
            <span>{{ new Date(favorite.createdAt).toLocaleString() }}</span>
          </li>
        </ul>
      </section>
    </div>
  </section>
</template>

<style scoped>
.login-prompt {
  display: grid;
  gap: 12px;
  justify-items: start;
}
</style>
