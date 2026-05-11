<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { sessionStore, weekendGoApi } from '../services';
import type { CurrentStatus, Place, PlaceImage, Review, PlaceQa } from '../services';
import { useAsyncAction, useApiError, useToast } from '../composables';

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
const activeTab = ref<'overview' | 'reviews' | 'qa' | 'contribute'>('overview');

const tabs = [
  { key: 'overview' as const, label: '概况' },
  { key: 'reviews' as const, label: '评价' },
  { key: 'qa' as const, label: '问大家' },
  { key: 'contribute' as const, label: '去贡献' }
];

// Q&A state
const questions = ref<PlaceQa[]>([]);
const expandedQuestionId = ref<number | null>(null);
const answersMap = ref<Record<number, PlaceQa[]>>({});
const newQuestionContent = ref('');
const newAnswerContent = ref('');
const qaSubmitting = ref(false);
const answerSubmitting = ref(false);
const qaLoading = ref(false);
const answersLoading = ref<Record<number, boolean>>({});

const { showToast } = useToast();

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

async function loadQuestions(): Promise<void> {
  qaLoading.value = true;
  try {
    questions.value = await weekendGoApi.getQuestions(placeId.value);
  } catch (err) {
    setError(err);
  } finally {
    qaLoading.value = false;
  }
}

async function toggleQuestion(questionId: number): Promise<void> {
  if (expandedQuestionId.value === questionId) {
    expandedQuestionId.value = null;
    return;
  }
  expandedQuestionId.value = questionId;
  if (!answersMap.value[questionId]) {
    answersLoading.value[questionId] = true;
    try {
      answersMap.value[questionId] = await weekendGoApi.getAnswers(questionId);
    } catch (err) {
      setError(err);
      answersMap.value[questionId] = [];
    } finally {
      answersLoading.value[questionId] = false;
    }
  }
}

async function submitQuestion(): Promise<void> {
  if (!newQuestionContent.value.trim()) return;
  if (!sessionStore.isLoggedIn.value) {
    router.push({ path: '/login', query: { redirect: route.fullPath } });
    return;
  }
  qaSubmitting.value = true;
  clearError();
  try {
    const question = await weekendGoApi.createQuestion(placeId.value, newQuestionContent.value.trim());
    questions.value.unshift(question);
    newQuestionContent.value = '';
    showToast('提问成功', 'success');
  } catch (err) {
    setError(err);
  } finally {
    qaSubmitting.value = false;
  }
}

async function submitAnswer(questionId: number): Promise<void> {
  if (!newAnswerContent.value.trim()) return;
  if (!sessionStore.isLoggedIn.value) {
    router.push({ path: '/login', query: { redirect: route.fullPath } });
    return;
  }
  answerSubmitting.value = true;
  clearError();
  try {
    const answer = await weekendGoApi.createAnswer(questionId, newAnswerContent.value.trim());
    if (!answersMap.value[questionId]) {
      answersMap.value[questionId] = [];
    }
    answersMap.value[questionId].push(answer);
    const q = questions.value.find((item) => item.id === questionId);
    if (q && typeof q.answerCount === 'number') {
      q.answerCount += 1;
    }
    newAnswerContent.value = '';
    showToast('回答成功', 'success');
  } catch (err) {
    setError(err);
  } finally {
    answerSubmitting.value = false;
  }
}

function formatDate(dateStr: string): string {
  const d = new Date(dateStr);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
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

watch(activeTab, (tab) => {
  if (tab === 'qa') {
    loadQuestions();
  }
});
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

      <template v-if="activeTab === 'qa'">
        <section class="panel">
          <h2>问大家</h2>

          <div v-if="sessionStore.isLoggedIn.value" class="qa-input-row">
            <input
              v-model="newQuestionContent"
              type="text"
              class="text-input"
              placeholder="请输入你的问题…"
              :disabled="qaSubmitting"
              @keydown.enter.prevent="submitQuestion"
            />
            <button
              class="primary-button"
              type="button"
              :disabled="qaSubmitting || !newQuestionContent.trim()"
              @click="submitQuestion"
            >
              {{ qaSubmitting ? '提交中…' : '提问' }}
            </button>
          </div>
          <p v-else class="muted">请登录后提问。</p>

          <p v-if="qaLoading" class="notice">正在加载问题…</p>
          <p v-else-if="questions.length === 0" class="muted">暂无问题，快来提问吧。</p>

          <ul v-else class="simple-list qa-list">
            <li v-for="question in questions" :key="question.id" class="qa-item">
              <div class="qa-question-header" @click="toggleQuestion(question.id)">
                <div class="qa-content">{{ question.content }}</div>
                <div class="qa-meta">
                  <span>{{ formatDate(question.createdAt) }}</span>
                  <span>{{ question.answerCount ?? 0 }} 条回答</span>
                  <span class="qa-toggle">{{ expandedQuestionId === question.id ? '收起' : '展开' }}</span>
                </div>
              </div>

              <div v-if="expandedQuestionId === question.id" class="qa-answers">
                <p v-if="answersLoading[question.id]" class="notice">正在加载回答…</p>
                <ul v-else-if="(answersMap[question.id] || []).length > 0" class="simple-list">
                  <li v-for="answer in answersMap[question.id]" :key="answer.id" class="qa-answer-item">
                    <div class="qa-content">{{ answer.content }}</div>
                    <div class="qa-meta">{{ formatDate(answer.createdAt) }}</div>
                  </li>
                </ul>
                <p v-else class="muted">暂无回答。</p>

                <div v-if="sessionStore.isLoggedIn.value" class="qa-input-row answer-input">
                  <input
                    v-model="newAnswerContent"
                    type="text"
                    class="text-input"
                    placeholder="请输入你的回答…"
                    :disabled="answerSubmitting"
                    @keydown.enter.prevent="submitAnswer(question.id)"
                  />
                  <button
                    class="primary-button"
                    type="button"
                    :disabled="answerSubmitting || !newAnswerContent.trim()"
                    @click="submitAnswer(question.id)"
                  >
                    {{ answerSubmitting ? '提交中…' : '回答' }}
                  </button>
                </div>
                <p v-else class="muted">请登录后回答。</p>
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

.qa-input-row {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.qa-input-row .text-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #d0d7de;
  border-radius: 6px;
  font-size: 14px;
}

.qa-input-row .text-input:focus {
  outline: none;
  border-color: #0969da;
}

.qa-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.qa-item {
  border: 1px solid #d0d7de;
  border-radius: 8px;
  padding: 12px;
}

.qa-question-header {
  cursor: pointer;
}

.qa-content {
  font-size: 14px;
  line-height: 1.6;
  color: #1f2328;
  margin-bottom: 4px;
}

.qa-meta {
  font-size: 12px;
  color: #656d76;
  display: flex;
  gap: 12px;
  align-items: center;
}

.qa-toggle {
  color: #0969da;
  font-weight: 500;
}

.qa-answers {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #eaeef2;
}

.qa-answer-item {
  padding: 8px 0;
  border-bottom: 1px solid #f6f8fa;
}

.qa-answer-item:last-child {
  border-bottom: none;
}

.answer-input {
  margin-top: 12px;
  margin-bottom: 0;
}
</style>
