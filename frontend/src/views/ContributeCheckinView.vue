<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { sessionStore, weekendGoApi } from '../services';
import { useApiError } from '../composables';

const route = useRoute();
const router = useRouter();
const placeId = computed(() => String(route.params.placeId ?? ''));

const checkinForm = reactive({
  crowdLevel: 'NORMAL',
  noiseLevel: 'RELATIVELY_QUIET',
  hasSeat: true,
  remark: ''
});

const loading = ref(false);
const message = ref('');
const { errorMessage, setError, clearError } = useApiError();

function goBack(): void {
  router.push(`/places/${placeId.value}`);
}

async function submitCheckin(): Promise<void> {
  if (!sessionStore.isLoggedIn.value) {
    router.push({ path: '/login', query: { redirect: route.fullPath } });
    return;
  }

  loading.value = true;
  clearError();
  message.value = '';

  try {
    await weekendGoApi.submitCheckin(placeId.value, {
      crowdLevel: checkinForm.crowdLevel,
      noiseLevel: checkinForm.noiseLevel,
      hasSeat: checkinForm.hasSeat,
      remark: checkinForm.remark
    });
    message.value = '打卡已提交。';
    checkinForm.remark = '';
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
        <h1 class="page-title">打卡</h1>
        <p class="page-subtitle">记录你到过这里；下面的实时状态可以顺手补充。</p>
      </div>
      <button class="ghost-button" type="button" @click="goBack">返回详情</button>
    </header>

    <p v-if="errorMessage" class="notice error">{{ errorMessage }}</p>
    <p v-if="message" class="notice success">{{ message }}</p>

    <form class="panel form-panel narrow" @submit.prevent="submitCheckin">
      <label class="field stacked">
        <span>当前人流（可选）</span>
        <select v-model="checkinForm.crowdLevel">
          <option value="FREE">空闲</option>
          <option value="NORMAL">适中</option>
          <option value="CROWDED">较拥挤</option>
          <option value="FULL">爆满</option>
        </select>
      </label>
      <label class="field stacked">
        <span>当前噪音（可选）</span>
        <select v-model="checkinForm.noiseLevel">
          <option value="QUIET">安静</option>
          <option value="RELATIVELY_QUIET">较安静</option>
          <option value="NORMAL">一般</option>
          <option value="NOISY">较吵</option>
          <option value="VERY_NOISY">很吵</option>
        </select>
      </label>
      <label class="check-field">
        <input v-model="checkinForm.hasSeat" type="checkbox" />
        <span>现在有空座（可选）</span>
      </label>
      <label class="field stacked">
        <span>备注（可选）</span>
        <textarea v-model="checkinForm.remark" rows="3" placeholder="例如：周末下午人很多，建议上午来..."></textarea>
      </label>
      <button class="primary-button" type="submit" :disabled="loading">
        {{ loading ? '提交中...' : '提交打卡' }}
      </button>
    </form>
  </section>
</template>
