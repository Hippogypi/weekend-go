<script setup lang="ts">
import { computed, ref } from 'vue';
import { RouterLink } from 'vue-router';

import { ApiError, sessionStore, weekendGoApi } from '../services';
import type { FavoritePlace } from '../services';

const mode = ref<'login' | 'register'>('login');
const username = ref('api-user-demo');
const password = ref('secret123');
const nickname = ref('API User');
const loading = ref(false);
const favoritesLoading = ref(false);
const message = ref('');
const error = ref('');
const favorites = ref<FavoritePlace[]>([]);

const isLoggedIn = computed(() => sessionStore.isLoggedIn.value);

function describeError(value: unknown): string {
  if (value instanceof ApiError) {
    const payload = value.payload as { message?: string; code?: string } | undefined;
    return payload?.message ?? payload?.code ?? `${value.status} ${value.message}`;
  }

  return value instanceof Error ? value.message : '请求失败，请稍后重试。';
}

async function submitAuth(): Promise<void> {
  error.value = '';
  message.value = '';
  loading.value = true;

  try {
    if (mode.value === 'register') {
      await weekendGoApi.register({
        username: username.value.trim(),
        password: password.value,
        nickname: nickname.value.trim()
      });
      message.value = '注册成功，请继续登录。';
      mode.value = 'login';
      return;
    }

    const session = await weekendGoApi.login({
      username: username.value.trim(),
      password: password.value
    });
    sessionStore.setSession(session);
    message.value = '登录成功。';
    await loadFavorites();
  } catch (err) {
    error.value = describeError(err);
  } finally {
    loading.value = false;
  }
}

async function loadFavorites(): Promise<void> {
  if (!sessionStore.isLoggedIn.value) {
    return;
  }

  favoritesLoading.value = true;
  error.value = '';
  try {
    favorites.value = await weekendGoApi.favorites();
  } catch (err) {
    error.value = describeError(err);
  } finally {
    favoritesLoading.value = false;
  }
}

async function logout(): Promise<void> {
  try {
    await weekendGoApi.logout();
  } catch {
    // Token may already be invalid; local logout should still complete.
  } finally {
    sessionStore.clearSession();
    favorites.value = [];
    message.value = '已退出登录。';
  }
}

if (sessionStore.isLoggedIn.value) {
  loadFavorites();
}
</script>

<template>
  <section class="page">
    <header class="page-header">
      <div>
        <h1 class="page-title">账号与收藏</h1>
        <p class="page-subtitle">登录后可提交共建、打卡、评价、图片并管理收藏。</p>
      </div>
      <span class="status-pill">{{ sessionStore.user.value?.role || 'Guest' }}</span>
    </header>

    <p v-if="message" class="notice success">{{ message }}</p>
    <p v-if="error" class="notice error">{{ error }}</p>

    <div class="two-column">
      <form class="panel form-panel" @submit.prevent="submitAuth">
        <h2>{{ mode === 'login' ? '登录' : '注册' }}</h2>
        <div class="segmented">
          <button type="button" :class="{ active: mode === 'login' }" @click="mode = 'login'">登录</button>
          <button type="button" :class="{ active: mode === 'register' }" @click="mode = 'register'">注册</button>
        </div>
        <label class="field stacked">
          <span>用户名</span>
          <input v-model="username" required />
        </label>
        <label class="field stacked">
          <span>密码</span>
          <input v-model="password" type="password" required minlength="8" />
        </label>
        <label v-if="mode === 'register'" class="field stacked">
          <span>昵称</span>
          <input v-model="nickname" required />
        </label>
        <button class="primary-button" type="submit" :disabled="loading">
          {{ loading ? '处理中' : mode === 'login' ? '登录' : '注册' }}
        </button>
      </form>

      <article class="panel">
        <div class="section-heading">
          <h2>当前账号</h2>
          <button v-if="isLoggedIn" class="ghost-button" type="button" @click="logout">退出</button>
        </div>
        <p v-if="!isLoggedIn" class="muted">未登录。登录后将自动带上 Bearer token 调用写接口。</p>
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
    </div>

    <section class="panel">
      <div class="section-heading">
        <h2>我的收藏</h2>
        <button class="ghost-button" type="button" :disabled="!isLoggedIn || favoritesLoading" @click="loadFavorites">
          {{ favoritesLoading ? '加载中' : '刷新' }}
        </button>
      </div>
      <p v-if="!isLoggedIn" class="muted">登录后查看收藏列表。</p>
      <p v-else-if="favorites.length === 0" class="muted">暂无收藏地点。</p>
      <ul v-else class="simple-list">
        <li v-for="favorite in favorites" :key="favorite.placeId">
          <RouterLink :to="`/places/${favorite.placeId}`">{{ favorite.placeName }}</RouterLink>
          <span>{{ new Date(favorite.createdAt).toLocaleString() }}</span>
        </li>
      </ul>
    </section>
  </section>
</template>
