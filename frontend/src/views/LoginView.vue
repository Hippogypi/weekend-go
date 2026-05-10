<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRouter } from 'vue-router';

import { sessionStore, weekendGoApi } from '../services';
import { useAsyncAction, useApiError, useAuthRedirect } from '../composables';

const router = useRouter();
const { navigateAfterLogin } = useAuthRedirect();
const { errorMessage, setError, clearError } = useApiError();

const mode = ref<'login' | 'register'>('login');
const username = ref('');
const password = ref('');
const nickname = ref('');

const { loading, execute } = useAsyncAction<void>({
  onError: (msg) => setError(new Error(msg))
});

const isLoggedIn = computed(() => sessionStore.isLoggedIn.value);

watch(isLoggedIn, (loggedIn) => {
  if (loggedIn) {
    navigateAfterLogin(router);
  }
}, { immediate: true });

watch(mode, () => {
  clearError();
});

async function submitAuth(): Promise<void> {
  clearError();

  await execute(async () => {
    if (mode.value === 'register') {
      await weekendGoApi.register({
        username: username.value.trim(),
        password: password.value,
        nickname: nickname.value.trim()
      });

      const session = await weekendGoApi.login({
        username: username.value.trim(),
        password: password.value
      });
      sessionStore.setSession(session);
      return;
    }

    const session = await weekendGoApi.login({
      username: username.value.trim(),
      password: password.value
    });
    sessionStore.setSession(session);
  });

  if (!errorMessage.value && sessionStore.isLoggedIn.value) {
    navigateAfterLogin(router);
  }
}
</script>

<template>
  <section class="page">
    <header class="page-header">
      <div>
        <h1 class="page-title">{{ mode === 'login' ? '登录' : '注册' }}</h1>
        <p class="page-subtitle">登录后可提交共建、打卡、评价、图片并管理收藏。</p>
      </div>
    </header>

    <p v-if="errorMessage" class="notice error">{{ errorMessage }}</p>

    <form class="panel form-panel narrow" @submit.prevent="submitAuth">
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
        {{ loading ? '处理中...' : mode === 'login' ? '登录' : '注册' }}
      </button>
    </form>
  </section>
</template>
