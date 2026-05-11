<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { sessionStore, weekendGoApi } from '../services';
import type { CurrentStatus, Place, PlaceImage, Review, ReviewReply } from '../services';
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
const reviewSort = ref<'time' | 'hot'>('time');

const tabs = [
  { key: 'overview' as const, label: '概况' },
  { key: 'reviews' as const, label: '评价' },
  { key: 'contribute' as const, label: '去贡献' }
];

const expandedReplyIds = ref<Set<number>>(new Set());
const repliesMap = reactive<Record<number, ReviewReply[]>>({});
const replyLoadingIds = ref<Set<number>>(new Set());
const replyInputs = reactive<Record<number, string>>({});
const likeLoadingIds = ref<Set<number>>(new Set());

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
      weekendGoApi.getReviews(placeId.value, reviewSort.value).catch(() => []),
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

async function switchSort(sort: 'time' | 'hot'): Promise<void> {
  reviewSort.value = sort;
  await doLoadDetail();
}

async function toggleLike(review: Review): Promise<void> {
  if (!sessionStore.isLoggedIn.value) {
    router.push({ path: '/login', query: { redirect: route.fullPath } });
    return;
  }

  likeLoadingIds.value.add(review.id);
  clearError();

  try {
    if (review.liked) {
      await weekendGoApi.unlikeReview(review.id);
      review.liked = false;
      review.likeCount = Math.max(0, review.likeCount - 1);
    } else {
      await weekendGoApi.likeReview(review.id);
      review.liked = true;
      review.likeCount++;
    }
  } catch (err) {
    setError(err);
  } finally {
    likeLoadingIds.value.delete(review.id);
  }
}

function toggleReplies(reviewId: number): void {
  if (expandedReplyIds.value.has(reviewId)) {
    expandedReplyIds.value.delete(reviewId);
  } else {
    expandedReplyIds.value.add(reviewId);
    if (!repliesMap[reviewId]) {
      loadReplies(reviewId);
    }
  }
}

async function loadReplies(reviewId: number): Promise<void> {
  replyLoadingIds.value.add(reviewId);
  try {
    repliesMap[reviewId] = await weekendGoApi.getReplies(reviewId);
  } catch (err) {
    setError(err);
  } finally {
    replyLoadingIds.value.delete(reviewId);
  }
}

async function submitReply(reviewId: number): Promise<void> {
  if (!sessionStore.isLoggedIn.value) {
    router.push({ path: '/login', query: { redirect: route.fullPath } });
    return;
  }

  const content = replyInputs[reviewId]?.trim();
  if (!content) return;

  replyLoadingIds.value.add(reviewId);
  clearError();

  try {
    const reply = await weekendGoApi.createReply(reviewId, content);
    if (!repliesMap[reviewId]) {
      repliesMap[reviewId] = [];
    }
    repliesMap[reviewId].push(reply);
    replyInputs[reviewId] = '';

    const review = reviews.value.find(r => r.id === reviewId);
    if (review) {
      review.replyCount++;
    }
  } catch (err) {
    setError(err);
  } finally {
    replyLoadingIds.value.delete(reviewId);
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
          <div class="reviews-header">
            <h2>公开评价</h2>
            <div class="sort-toggle">
              <button
                :class="{ active: reviewSort === 'time' }"
                type="button"
                @click="switchSort('time')"
              >
                最新
              </button>
              <button
                :class="{ active: reviewSort === 'hot' }"
                type="button"
                @click="switchSort('hot')"
              >
                最热
              </button>
            </div>
          </div>

          <p v-if="reviews.length === 0" class="muted">暂无审核通过的评价。</p>
          <ul v-else class="simple-list review-list">
            <li v-for="review in reviews" :key="review.id" class="review-card">
              <p class="review-content">{{ review.content || '(纯共建评价)' }}</p>

              <div class="review-scores">
                <span>安静 {{ review.quietScore }}</span>
                <span>Wi-Fi {{ review.wifiScore }}</span>
                <span>插座 {{ review.socketScore }}</span>
                <span>舒适 {{ review.comfortScore }}</span>
                <span>性价比 {{ review.costScore }}</span>
                <span v-if="review.seatScore !== undefined">座位 {{ review.seatScore }}</span>
              </div>

              <div v-if="review.suitableScenes && review.suitableScenes.length > 0" class="review-scenes">
                <span
                  v-for="scene in review.suitableScenes"
                  :key="scene"
                  class="scene-tag"
                >
                  {{ scene }}
                </span>
              </div>

              <div v-if="review.images && review.images.length > 0" class="review-images">
                <img
                  v-for="img in review.images"
                  :key="img.id"
                  :src="img.imageUrl"
                  :alt="img.description || ''"
                />
              </div>

              <div class="review-actions">
                <button
                  v-if="sessionStore.isLoggedIn.value"
                  type="button"
                  class="text-button like-button"
                  :class="{ liked: review.liked }"
                  :disabled="likeLoadingIds.has(review.id)"
                  @click="toggleLike(review)"
                >
                  {{ review.liked ? '❤️' : '🤍' }} {{ review.likeCount }}
                </button>
                <span v-else class="like-count">🤍 {{ review.likeCount }}</span>

                <button
                  type="button"
                  class="text-button reply-toggle"
                  @click="toggleReplies(review.id)"
                >
                  💬 {{ review.replyCount }}
                  {{ expandedReplyIds.has(review.id) ? '收起' : '回复' }}
                </button>
              </div>

              <div v-if="expandedReplyIds.has(review.id)" class="replies-section">
                <p v-if="replyLoadingIds.has(review.id)" class="muted">加载回复中...</p>
                <ul v-else-if="repliesMap[review.id] && repliesMap[review.id].length > 0" class="reply-list">
                  <li v-for="reply in repliesMap[review.id]" :key="reply.id" class="reply-item">
                    <p class="reply-content">{{ reply.content }}</p>
                    <span class="reply-time">{{ reply.createdAt }}</span>
                  </li>
                </ul>
                <p v-else class="muted">暂无回复。</p>

                <div v-if="sessionStore.isLoggedIn.value" class="reply-form">
                  <input
                    v-model="replyInputs[review.id]"
                    type="text"
                    placeholder="写下你的回复..."
                    @keydown.enter.prevent="submitReply(review.id)"
                  />
                  <button
                    type="button"
                    class="primary-button"
                    :disabled="!replyInputs[review.id]?.trim() || replyLoadingIds.has(review.id)"
                    @click="submitReply(review.id)"
                  >
                    {{ replyLoadingIds.has(review.id) ? '发送中...' : '发送' }}
                  </button>
                </div>
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
.reviews-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.reviews-header h2 {
  margin: 0;
}

.sort-toggle {
  display: flex;
  gap: 4px;
}

.sort-toggle button {
  padding: 4px 10px;
  border: 1px solid #cfd6e1;
  background: #ffffff;
  border-radius: 6px;
  font-size: 13px;
  color: #475467;
  cursor: pointer;
}

.sort-toggle button.active {
  border-color: #0f766e;
  background: #e8f3f1;
  color: #0f5f59;
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-card {
  border: 1px solid #eaecf0;
  border-radius: 8px;
  padding: 14px;
  background: #ffffff;
}

.review-content {
  margin: 0 0 10px;
  font-size: 15px;
  color: #101828;
  line-height: 1.5;
}

.review-scores {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 13px;
  color: #475467;
  margin-bottom: 10px;
}

.review-scenes {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

.scene-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 999px;
  background: #f2f4f7;
  color: #344054;
}

.review-images {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.review-images img {
  width: 96px;
  height: 72px;
  border-radius: 6px;
  object-fit: cover;
  background: #eef2f6;
}

.review-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.like-button,
.reply-toggle {
  font-size: 13px;
  color: #475467;
}

.like-button.liked {
  color: #dc2626;
}

.like-count {
  font-size: 13px;
  color: #475467;
}

.replies-section {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #eaecf0;
}

.reply-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 12px;
}

.reply-item {
  background: #f9fafb;
  border-radius: 6px;
  padding: 10px;
}

.reply-content {
  margin: 0 0 4px;
  font-size: 14px;
  color: #101828;
}

.reply-time {
  font-size: 12px;
  color: #667085;
}

.reply-form {
  display: flex;
  gap: 8px;
}

.reply-form input {
  flex: 1;
  min-width: 0;
  padding: 8px 12px;
  border: 1px solid #cfd6e1;
  border-radius: 6px;
  font-size: 14px;
}

.reply-form button {
  padding: 8px 14px;
  font-size: 13px;
}
</style>
