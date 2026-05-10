<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { sessionStore, weekendGoApi } from '../services';
import { useApiError } from '../composables';

const route = useRoute();
const router = useRouter();
const placeId = computed(() => String(route.params.placeId ?? ''));

const reviewForm = reactive({
  quietScore: 4,
  wifiScore: 4,
  socketScore: 4,
  comfortScore: 4,
  costScore: 3,
  content: ''
});

const loading = ref(false);
const message = ref('');
const { errorMessage, setError, clearError } = useApiError();

function goBack(): void {
  router.push(`/places/${placeId.value}`);
}

async function submitReview(): Promise<void> {
  if (!sessionStore.isLoggedIn.value) {
    router.push({ path: '/login', query: { redirect: route.fullPath } });
    return;
  }

  loading.value = true;
  clearError();
  message.value = '';

  try {
    const review = await weekendGoApi.submitReview(placeId.value, {
      quietScore: Number(reviewForm.quietScore),
      wifiScore: Number(reviewForm.wifiScore),
      socketScore: Number(reviewForm.socketScore),
      comfortScore: Number(reviewForm.comfortScore),
      costScore: Number(reviewForm.costScore),
      content: reviewForm.content
    });
    message.value = `评价已提交，审核状态：${review.auditStatus}`;
    reviewForm.content = '';
  } catch (err) {
    setError(err);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <section class="page">
    <header class="page-header">
      <div>
        <h1 class="page-title">写评价</h1>
        <p class="page-subtitle">分享你对这个地方的整体体验。</p>
      </div>
      <button class="ghost-button" type="button" @click="goBack">返回详情</button>
    </header>

    <p v-if="errorMessage" class="notice error">{{ errorMessage }}</p>
    <p v-if="message" class="notice success">{{ message }}</p>

    <form class="panel form-panel" @submit.prevent="submitReview">
      <div class="compact-grid">
        <label class="field stacked">
          <span>安静程度</span>
          <input v-model.number="reviewForm.quietScore" type="number" min="1" max="5" step="0.5" />
        </label>
        <label class="field stacked">
          <span>Wi-Fi</span>
          <input v-model.number="reviewForm.wifiScore" type="number" min="1" max="5" step="0.5" />
        </label>
        <label class="field stacked">
          <span>插座</span>
          <input v-model.number="reviewForm.socketScore" type="number" min="1" max="5" step="0.5" />
        </label>
        <label class="field stacked">
          <span>舒适度</span>
          <input v-model.number="reviewForm.comfortScore" type="number" min="1" max="5" step="0.5" />
        </label>
        <label class="field stacked">
          <span>性价比</span>
          <input v-model.number="reviewForm.costScore" type="number" min="1" max="5" step="0.5" />
        </label>
      </div>
      <label class="field stacked">
        <span>评价内容</span>
        <textarea v-model="reviewForm.content" required rows="4" placeholder="例如：周末下午人很多，但插座充足，Wi-Fi稳定，适合带电脑工作..."></textarea>
      </label>
      <button class="primary-button" type="submit" :disabled="loading">
        {{ loading ? '提交中...' : '提交评价' }}
      </button>
    </form>
  </section>
</template>
