<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const route = useRoute();
const router = useRouter();
const placeId = computed(() => String(route.params.placeId ?? ''));

function goCheckin(): void {
  router.push(`/places/${placeId.value}/contribute/checkin`);
}

function goReview(): void {
  router.push(`/places/${placeId.value}/contribute/review`);
}

function goBack(): void {
  router.push(`/places/${placeId.value}`);
}
</script>

<template>
  <section class="page">
    <header class="page-header">
      <div>
        <h1 class="page-title">贡献信息</h1>
        <p class="page-subtitle">打卡记录到访；评价和照片用于沉淀地点长期画像。</p>
      </div>
      <button class="ghost-button" type="button" @click="goBack">返回详情</button>
    </header>

    <div class="two-column">
      <article class="panel action-card" @click="goCheckin">
        <div class="action-icon">📍</div>
        <h2>打卡</h2>
        <p class="muted">记录你到过这里；也可以顺手反馈当前人流、噪音和座位情况。</p>
        <button class="primary-button">去打卡</button>
      </article>

      <article class="panel action-card" @click="goReview">
        <div class="action-icon">⭐</div>
        <h2>写评价 / 上传照片</h2>
        <p class="muted">分享整体体验，补充地点照片、Wi-Fi、插座和座位等信息。</p>
        <button class="primary-button">写评价 / 上传照片</button>
      </article>
    </div>
  </section>
</template>

<style scoped>
.action-card {
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
  display: grid;
  gap: 12px;
  justify-items: start;
}
.action-card:hover {
  border-color: #0f766e;
  box-shadow: 0 2px 8px rgba(15, 118, 110, 0.08);
}
.action-icon {
  font-size: 2rem;
  line-height: 1;
}
</style>
