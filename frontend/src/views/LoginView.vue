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
  <div class="login-view">
    <h1 class="platform-brand">城市书房共建平台</h1>

    <p v-if="errorMessage" class="notice error">{{ errorMessage }}</p>

    <form class="panel form-panel narrow glass" @submit.prevent="submitAuth">
      <header class="form-header">
        <h2 class="form-title">{{ mode === 'login' ? '欢迎登录' : '欢迎注册' }}</h2>
        <p class="form-subtitle">登录后可提交共建、打卡、评价、图片并管理收藏。</p>
      </header>

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
  </div>
</template>

<style scoped>
.login-view {
  width: 100%;
  max-width: 480px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 80px 24px 24px;
}

.platform-brand {
  position: absolute;
  top: 32px;
  left: 40px;
  margin: 0;
  color: #0f766e;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 1.5px;
  text-shadow: 0 1px 3px rgba(255, 255, 255, 0.9);
}

.notice {
  text-align: center;
  width: 100%;
  background: rgba(255, 245, 245, 0.92);
  backdrop-filter: blur(4px);
}

.glass {
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 12px 40px rgba(15, 118, 110, 0.12);
  border-radius: 16px;
  padding: 32px;
  width: 100%;
}

.form-header {
  text-align: center;
  margin-bottom: 4px;
}

.form-title {
  margin: 0 0 8px;
  color: #0f5f59;
  font-size: 24px;
  font-weight: 700;
}

.form-subtitle {
  margin: 0;
  color: #667085;
  font-size: 14px;
  line-height: 1.5;
}

.segmented {
  margin-bottom: 4px;
}
</style>
