<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';

import { ApiError, sessionStore, weekendGoApi } from '../services';
import type { CurrentStatus, Place, PlaceImage, Review } from '../services';

const route = useRoute();
const placeId = computed(() => String(route.params.placeId ?? ''));
const place = ref<Place | null>(null);
const currentStatus = ref<CurrentStatus | null>(null);
const reviews = ref<Review[]>([]);
const images = ref<PlaceImage[]>([]);
const favorited = ref(false);
const loading = ref(true);
const actionLoading = ref('');
const error = ref('');
const message = ref('');

const profileForm = reactive({
  quietScore: 4,
  wifiScore: 4,
  socketScore: 4,
  seatScore: 4,
  costScore: 3,
  minConsumption: 20,
  allowLongStay: 'TRUE',
  suitableScenes: 'READING,REMOTE_WORK',
  remark: ''
});
const checkinForm = reactive({
  crowdLevel: 'NORMAL',
  noiseLevel: 'RELATIVELY_QUIET',
  hasSeat: true,
  remark: ''
});
const reviewForm = reactive({
  quietScore: 4,
  wifiScore: 4,
  socketScore: 4,
  comfortScore: 4,
  costScore: 3,
  content: ''
});
const imageForm = reactive({
  imageUrl: '',
  description: ''
});

function describeError(value: unknown): string {
  if (value instanceof ApiError) {
    const payload = value.payload as { message?: string; code?: string } | undefined;
    if (value.status === 401) {
      return '需要先登录后才能执行该操作。';
    }
    if (value.status === 403) {
      return '当前账号权限不足。';
    }
    return payload?.message ?? payload?.code ?? `${value.status} ${value.message}`;
  }

  return value instanceof Error ? value.message : '请求失败，请稍后重试。';
}

async function loadDetail(): Promise<void> {
  loading.value = true;
  error.value = '';

  try {
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
  } catch (err) {
    error.value = describeError(err);
  } finally {
    loading.value = false;
  }
}

async function loadFavoriteStatus(): Promise<void> {
  try {
    favorited.value = (await weekendGoApi.favoriteStatus(placeId.value)).favorited;
  } catch {
    favorited.value = false;
  }
}

async function runAction(name: string, action: () => Promise<void>): Promise<void> {
  if (!sessionStore.isLoggedIn.value) {
    error.value = '请先登录后再提交。';
    return;
  }

  actionLoading.value = name;
  error.value = '';
  message.value = '';
  try {
    await action();
  } catch (err) {
    error.value = describeError(err);
  } finally {
    actionLoading.value = '';
  }
}

async function submitProfile(): Promise<void> {
  await runAction('profile', async () => {
    const submission = await weekendGoApi.submitProfile(placeId.value, {
      quietScore: Number(profileForm.quietScore),
      wifiScore: Number(profileForm.wifiScore),
      socketScore: Number(profileForm.socketScore),
      seatScore: Number(profileForm.seatScore),
      costScore: Number(profileForm.costScore),
      minConsumption: Number(profileForm.minConsumption),
      allowLongStay: profileForm.allowLongStay,
      suitableScenes: profileForm.suitableScenes.split(',').map((item) => item.trim()).filter(Boolean),
      remark: profileForm.remark
    });
    message.value = `属性共建已提交，审核状态：${submission.auditStatus}`;
  });
}

async function submitCheckin(): Promise<void> {
  await runAction('checkin', async () => {
    await weekendGoApi.submitCheckin(placeId.value, {
      crowdLevel: checkinForm.crowdLevel,
      noiseLevel: checkinForm.noiseLevel,
      hasSeat: checkinForm.hasSeat,
      remark: checkinForm.remark
    });
    currentStatus.value = await weekendGoApi.currentStatus(placeId.value);
    message.value = '打卡反馈已提交。';
  });
}

async function submitReview(): Promise<void> {
  await runAction('review', async () => {
    const review = await weekendGoApi.submitReview(placeId.value, {
      quietScore: Number(reviewForm.quietScore),
      wifiScore: Number(reviewForm.wifiScore),
      socketScore: Number(reviewForm.socketScore),
      comfortScore: Number(reviewForm.comfortScore),
      costScore: Number(reviewForm.costScore),
      content: reviewForm.content
    });
    reviewForm.content = '';
    message.value = `评价已提交，审核状态：${review.auditStatus}`;
  });
}

async function submitImage(): Promise<void> {
  await runAction('image', async () => {
    const image = await weekendGoApi.submitImage(placeId.value, {
      imageUrl: imageForm.imageUrl,
      description: imageForm.description
    });
    imageForm.imageUrl = '';
    imageForm.description = '';
    message.value = `图片已提交，审核状态：${image.auditStatus}`;
  });
}

async function toggleFavorite(): Promise<void> {
  await runAction('favorite', async () => {
    const result = favorited.value
      ? await weekendGoApi.removeFavorite(placeId.value)
      : await weekendGoApi.addFavorite(placeId.value);
    favorited.value = result.favorited;
    message.value = result.favorited ? '已收藏。' : '已取消收藏。';
  });
}

loadDetail();
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

    <p v-if="error" class="notice error">{{ error }}</p>
    <p v-if="message" class="notice success">{{ message }}</p>
    <p v-if="loading" class="notice">正在加载地点详情...</p>

    <template v-else-if="place">
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

      <div class="form-grid">
        <form class="panel form-panel" @submit.prevent="submitProfile">
          <h2>提交属性共建</h2>
          <div class="compact-grid">
            <label class="field stacked"><span>安静</span><input v-model.number="profileForm.quietScore" type="number" min="1" max="5" step="0.5" /></label>
            <label class="field stacked"><span>Wi-Fi</span><input v-model.number="profileForm.wifiScore" type="number" min="1" max="5" step="0.5" /></label>
            <label class="field stacked"><span>插座</span><input v-model.number="profileForm.socketScore" type="number" min="1" max="5" step="0.5" /></label>
            <label class="field stacked"><span>座位</span><input v-model.number="profileForm.seatScore" type="number" min="1" max="5" step="0.5" /></label>
          </div>
          <label class="field stacked"><span>适合场景</span><input v-model="profileForm.suitableScenes" /></label>
          <label class="field stacked"><span>备注</span><textarea v-model="profileForm.remark" rows="3" /></label>
          <button class="primary-button" :disabled="actionLoading === 'profile'">提交共建</button>
        </form>

        <form class="panel form-panel" @submit.prevent="submitCheckin">
          <h2>打卡反馈</h2>
          <label class="field stacked"><span>拥挤度</span><select v-model="checkinForm.crowdLevel"><option>NORMAL</option><option>QUIET</option><option>CROWDED</option></select></label>
          <label class="field stacked"><span>噪音</span><select v-model="checkinForm.noiseLevel"><option>RELATIVELY_QUIET</option><option>QUIET</option><option>NORMAL</option><option>NOISY</option></select></label>
          <label class="check-field"><input v-model="checkinForm.hasSeat" type="checkbox" /> 有空座</label>
          <label class="field stacked"><span>备注</span><textarea v-model="checkinForm.remark" rows="3" /></label>
          <button class="primary-button" :disabled="actionLoading === 'checkin'">提交打卡</button>
        </form>

        <form class="panel form-panel" @submit.prevent="submitReview">
          <h2>提交评价</h2>
          <div class="compact-grid">
            <label class="field stacked"><span>安静</span><input v-model.number="reviewForm.quietScore" type="number" min="1" max="5" step="0.5" /></label>
            <label class="field stacked"><span>Wi-Fi</span><input v-model.number="reviewForm.wifiScore" type="number" min="1" max="5" step="0.5" /></label>
            <label class="field stacked"><span>插座</span><input v-model.number="reviewForm.socketScore" type="number" min="1" max="5" step="0.5" /></label>
            <label class="field stacked"><span>舒适</span><input v-model.number="reviewForm.comfortScore" type="number" min="1" max="5" step="0.5" /></label>
          </div>
          <label class="field stacked"><span>内容</span><textarea v-model="reviewForm.content" required rows="3" /></label>
          <button class="primary-button" :disabled="actionLoading === 'review'">提交评价</button>
        </form>

        <form class="panel form-panel" @submit.prevent="submitImage">
          <h2>提交图片</h2>
          <label class="field stacked"><span>图片 URL</span><input v-model="imageForm.imageUrl" required placeholder="https://example.com/photo.jpg" /></label>
          <label class="field stacked"><span>说明</span><textarea v-model="imageForm.description" rows="3" /></label>
          <button class="primary-button" :disabled="actionLoading === 'image'">提交图片</button>
        </form>
      </div>

      <div class="two-column">
        <section class="panel">
          <h2>公开评价</h2>
          <p v-if="reviews.length === 0" class="muted">暂无审核通过的评价。</p>
          <ul v-else class="simple-list">
            <li v-for="review in reviews" :key="review.id">
              <strong>{{ review.content }}</strong>
              <span>综合 {{ review.comfortScore }} / 安静 {{ review.quietScore }}</span>
            </li>
          </ul>
        </section>

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
      </div>
    </template>
  </section>
</template>
