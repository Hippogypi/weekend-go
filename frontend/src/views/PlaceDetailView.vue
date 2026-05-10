<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { sessionStore, weekendGoApi } from '../services';
import type { CurrentStatus, Place, PlaceImage, Review } from '../services';
import { useAsyncAction, useApiError } from '../composables';

const route = useRoute();
const router = useRouter();
const placeId = computed(() => String(route.params.placeId ?? ''));

const place = ref<Place | null>(null);
const currentStatus = ref<CurrentStatus | null>(null);
const reviews = ref<Review[]>([]);
const images = ref<PlaceImage[]>([]);
const favorited = ref(false);
const actionLoading = ref('');
const message = ref('');
const activeTab = ref<'overview' | 'reviews' | 'contribute'>('overview');

const tabs = [
  { key: 'overview' as const, label: '概况' },
  { key: 'reviews' as const, label: '评价' },
  { key: 'contribute' as const, label: '去贡献' }
];

const { errorMessage, setError, clearError } = useApiError();

const {
  loading: detailLoading,
  execute: loadDetail
} = useAsyncAction<void>({
  onError: (msg) => setError(new Error(msg))
});

async function doLoadDetail(): Promise<void> {
  clearError();
  await loadDetail(async () => {
    const [detail, status, publicReviews, publicImages] = await Promise.all([
      weekendGoApi.placeDetail(placeId.value),
      weekendGoApi.currentStatus(placeId.value).catch(() => null),
      weekendGoApi.reviews(placeId.value).catch(() => []),
      weekendGoApi.images(placeId.value).catch(() => [])
    ]);
    place.value = detail;
    currentStatus.value = status;
    reviews.value = publicReviews;
    images.value = publicImages;
    if (sessionStore.isLoggedIn.value) {
      await loadFavoriteStatus();
    }
  });
}

async function loadFavoriteStatus(): Promise<void> {
  try {
    favorited.value = (await weekendGoApi.favoriteStatus(placeId.value)).favorited;
  } catch {
    favorited.value = false;
  }
}

async function toggleFavorite(): Promise<void> {
  if (!sessionStore.isLoggedIn.value) {
    router.push({ path: '/login', query: { redirect: route.fullPath } });
    return;
  }

  actionLoading.value = 'favorite';
  clearError();
  message.value = '';

  try {
    const result = favorited.value
      ? await weekendGoApi.removeFavorite(placeId.value)
      : await weekendGoApi.addFavorite(placeId.value);
    favorited.value = result.favorited;
    message.value = result.favorited ? '已收藏。' : '已取消收藏。';
  } catch (err) {
    setError(err);
  } finally {
    actionLoading.value = '';
  }
}

doLoadDetail();
</script>

<template>
  <section class="page">
    <header class="page-header">
      <div>
        <h1 class="page-title">{{ place?.name || '地点详情' }}</h1>
        <p class="page-subtitle">{{ place?.address || `当前地点：${placeId}` }}</p>
      </div>
      <button class="primary-button" type="button" :disabled="actionLoading === 'favorite'" @click="toggleFavorite">
        {{ favorited ? '取消收藏' : '收藏' }}
      </button>
    </header>

    <p v-if="errorMessage" class="notice error">{{ errorMessage }}</p>
    <p v-if="message" class="notice success">{{ message }}</p>
    <p v-if="detailLoading" class="notice">正在加载地点详情...</p>

    <template v-else-if="place">
      <div class="segmented" role="tablist">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          :class="{ active: activeTab === tab.key }"
          role="tab"
          :aria-selected="activeTab === tab.key"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </div>

      <template v-if="activeTab === 'overview'">
        <div class="two-column">
          <article class="panel">
            <h2>基础信息</h2>
            <dl class="meta-list vertical">
              <div><dt>区域</dt><dd>{{ place.city || place.district || '未知' }}</dd></div>
              <div><dt>类型</dt><dd>{{ place.amapType || '未知' }}</dd></div>
              <div><dt>状态</dt><dd>{{ place.workspaceStatus || '候选' }}</dd></div>
              <div><dt>坐标</dt><dd>{{ place.longitude || '-' }}, {{ place.latitude || '-' }}</dd></div>
            </dl>
          </article>

          <article class="panel">
            <h2>当前状态</h2>
            <p class="metric">{{ currentStatus?.message || '暂无近期反馈' }}</p>
            <dl class="meta-list">
              <div><dt>样本</dt><dd>{{ currentStatus?.sampleCount ?? 0 }}</dd></div>
              <div><dt>拥挤</dt><dd>{{ currentStatus?.crowdLevel || '-' }}</dd></div>
              <div><dt>噪音</dt><dd>{{ currentStatus?.noiseLevel || '-' }}</dd></div>
              <div><dt>空座</dt><dd>{{ currentStatus?.hasSeat === true ? '有' : currentStatus?.hasSeat === false ? '无' : '-' }}</dd></div>
            </dl>
          </article>
        </div>

        <article class="panel">
          <h2>学习办公属性</h2>
          <p v-if="!place.workspaceProfile" class="muted">暂无审核通过的共建属性。</p>
          <dl v-else class="score-grid">
            <div><dt>总分</dt><dd>{{ place.workspaceProfile.score }}</dd></div>
            <div><dt>安静</dt><dd>{{ place.workspaceProfile.quietScore }}</dd></div>
            <div><dt>Wi-Fi</dt><dd>{{ place.workspaceProfile.wifiScore }}</dd></div>
            <div><dt>插座</dt><dd>{{ place.workspaceProfile.socketScore }}</dd></div>
            <div><dt>座位</dt><dd>{{ place.workspaceProfile.seatScore }}</dd></div>
            <div><dt>可信度</dt><dd>{{ place.workspaceProfile.trustLevel }}</dd></div>
          </dl>
        </article>

        <section class="panel">
          <h2>公开图片</h2>
          <p v-if="images.length === 0" class="muted">暂无审核通过的图片。</p>
          <div v-else class="image-grid">
            <figure v-for="image in images" :key="image.id">
              <img :src="image.imageUrl" :alt="image.description || '地点图片'" />
              <figcaption>{{ image.description || '地点图片' }}</figcaption>
            </figure>
          </div>
        </section>
      </template>

      <template v-if="activeTab === 'reviews'">
        <section class="panel">
          <h2>公开评价</h2>
          <p v-if="reviews.length === 0" class="muted">暂无审核通过的评价。</p>
          <ul v-else class="simple-list">
            <li v-for="review in reviews" :key="review.id">
              <strong>{{ review.content }}</strong>
              <span>综合 {{ review.comfortScore }} / 安静 {{ review.quietScore }}</span>
              <div v-if="review.images && review.images.length > 0" class="review-images">
                <img
                  v-for="img in review.images"
                  :key="img.id"
                  :src="img.imageUrl"
                  :alt="img.description || ''"
                />
              </div>
            </li>
          </ul>
        </section>
      </template>

      <template v-if="activeTab === 'contribute'">
        <div class="two-column">
          <article class="panel action-card" @click="router.push(`/places/${placeId}/contribute/checkin`)">
            <div class="action-icon">📍</div>
            <h2>打卡反馈</h2>
            <p class="muted">告诉其他人这里现在人多不多、吵不吵、有没有座位。</p>
            <button class="primary-button" type="button">去打卡</button>
          </article>

          <article class="panel action-card" @click="router.push(`/places/${placeId}/contribute/review`)">
            <div class="action-icon">⭐</div>
            <h2>写评价</h2>
            <p class="muted">分享你的整体体验：环境怎么样、适不适合学习办公。</p>
            <button class="primary-button" type="button">写评价</button>
          </article>
        </div>
      </template>
    </template>
  </section>
</template>

<style scoped>
.review-images {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 6px;
}

.review-images img {
  width: 96px;
  height: 72px;
  border-radius: 6px;
  object-fit: cover;
  background: #eef2f6;
}
</style>
