<script setup lang="ts">
import { computed, ref } from 'vue';
import { RouterLink, useRouter } from 'vue-router';

import { sessionStore, weekendGoApi } from '../services';
import type { FavoritePlace, MyCheckin, MyReview } from '../services';
import {
  allowLongStayLabel,
  auditStatusLabel,
  crowdLevelLabel,
  noiseLevelLabel,
  sceneLabel
} from '../services/displayLabels';
import { useAsyncAction, useApiError } from '../composables';

const router = useRouter();
const { errorMessage, setError, clearError } = useApiError();

const isLoggedIn = computed(() => sessionStore.isLoggedIn.value);
const activeTab = ref<'favorites' | 'checkins' | 'reviews'>('favorites');

const isEditingNickname = ref(false);
const nicknameDraft = ref('');
const nicknameSaving = ref(false);

const {
  loading: favoritesLoading,
  data: favorites,
  execute: loadFavorites
} = useAsyncAction<FavoritePlace[]>({
  onError: (msg) => setError(new Error(msg))
});

const {
  loading: checkinsLoading,
  data: checkins,
  execute: loadCheckins
} = useAsyncAction<MyCheckin[]>({
  onError: (msg) => setError(new Error(msg))
});

const {
  loading: reviewsLoading,
  data: reviews,
  execute: loadReviews
} = useAsyncAction<MyReview[]>({
  onError: (msg) => setError(new Error(msg))
});

async function doLoadFavorites(): Promise<void> {
  if (!isLoggedIn.value) return;
  clearError();
  await loadFavorites(() => weekendGoApi.favorites());
}

async function doLoadCheckins(): Promise<void> {
  if (!isLoggedIn.value) return;
  clearError();
  await loadCheckins(() => weekendGoApi.myCheckins());
}

async function doLoadReviews(): Promise<void> {
  if (!isLoggedIn.value) return;
  clearError();
  await loadReviews(() => weekendGoApi.myReviews());
}

function switchTab(tab: 'favorites' | 'checkins' | 'reviews') {
  activeTab.value = tab;
  if (tab === 'favorites' && (!favorites.value || favorites.value.length === 0)) {
    doLoadFavorites();
  } else if (tab === 'checkins' && (!checkins.value || checkins.value.length === 0)) {
    doLoadCheckins();
  } else if (tab === 'reviews' && (!reviews.value || reviews.value.length === 0)) {
    doLoadReviews();
  }
}

function startEditNickname() {
  nicknameDraft.value = sessionStore.user.value?.nickname ?? '';
  isEditingNickname.value = true;
}

function cancelEditNickname() {
  isEditingNickname.value = false;
  nicknameDraft.value = '';
}

async function saveNickname() {
  const trimmed = nicknameDraft.value.trim();
  if (!trimmed) return;
  nicknameSaving.value = true;
  try {
    const updated = await weekendGoApi.updateNickname(trimmed);
    sessionStore.updateUser(updated);
    isEditingNickname.value = false;
  } catch (e) {
    setError(e instanceof Error ? e : new Error('保存昵称失败'));
  } finally {
    nicknameSaving.value = false;
  }
}

async function logout(): Promise<void> {
  try {
    await weekendGoApi.logout();
  } catch {
    // Token may already be invalid; local logout should still complete.
  } finally {
    sessionStore.clearSession();
    router.push('/');
  }
}

function formatDate(iso: string | undefined): string {
  if (!iso) return '';
  return new Date(iso).toLocaleString();
}

function auditStatusClass(status: string | null | undefined): string {
  if (!status) return '';
  return 'status-' + status.toLowerCase();
}

if (isLoggedIn.value) {
  doLoadFavorites();
}
</script>

<template>
  <section class="page">
    <header class="page-header">
      <div>
        <h1 class="page-title">个人中心</h1>
        <p class="page-subtitle">管理个人信息和贡献记录。</p>
      </div>
      <span class="status-pill">{{ sessionStore.user.value?.role || 'Guest' }}</span>
    </header>

    <p v-if="errorMessage" class="notice error">{{ errorMessage }}</p>

    <article class="panel">
      <div class="section-heading">
        <h2>当前账号</h2>
        <button v-if="isLoggedIn" class="ghost-button" type="button" @click="logout">退出登录</button>
      </div>
      <div v-if="!isLoggedIn" class="login-prompt">
        <p class="muted">未登录。登录后可提交共建、打卡、评价、图片并管理收藏。</p>
        <button class="primary-button" type="button" @click="router.push('/login')">去登录</button>
      </div>
      <dl v-else class="meta-list vertical">
        <div>
          <dt>用户名</dt>
          <dd>{{ sessionStore.user.value?.username }}</dd>
        </div>
        <div>
          <dt>昵称</dt>
          <dd>
            <template v-if="!isEditingNickname">
              <span>{{ sessionStore.user.value?.nickname }}</span>
              <button class="text-button" type="button" @click="startEditNickname">编辑昵称</button>
            </template>
            <template v-else>
              <div class="nickname-edit">
                <input v-model="nicknameDraft" type="text" maxlength="50" placeholder="输入新昵称" />
                <div class="nickname-actions">
                  <button class="primary-button" type="button" :disabled="nicknameSaving" @click="saveNickname">
                    {{ nicknameSaving ? '保存中...' : '保存' }}
                  </button>
                  <button class="ghost-button" type="button" @click="cancelEditNickname">取消</button>
                </div>
              </div>
            </template>
          </dd>
        </div>
        <div>
          <dt>角色</dt>
          <dd>{{ sessionStore.user.value?.role }}</dd>
        </div>
      </dl>
    </article>

    <div class="panel">
      <div class="tab-nav">
        <button
          :class="['tab-button', { active: activeTab === 'favorites' }]"
          type="button"
          @click="switchTab('favorites')"
        >
          ❤️ 我的收藏
        </button>
        <button
          :class="['tab-button', { active: activeTab === 'checkins' }]"
          type="button"
          @click="switchTab('checkins')"
        >
          📍 我的打卡
        </button>
        <button
          :class="['tab-button', { active: activeTab === 'reviews' }]"
          type="button"
          @click="switchTab('reviews')"
        >
          ⭐ 我的评价
        </button>
      </div>

      <div v-if="!isLoggedIn" class="login-prompt">
        <p class="muted">登录后查看个人记录。</p>
        <button class="primary-button" type="button" @click="router.push('/login')">去登录</button>
      </div>

      <div v-else-if="activeTab === 'favorites'" class="tab-content">
        <div class="section-heading">
          <h3>我的收藏</h3>
          <button class="ghost-button" type="button" :disabled="favoritesLoading" @click="doLoadFavorites">
            {{ favoritesLoading ? '加载中...' : '刷新' }}
          </button>
        </div>
        <p v-if="!favorites || favorites.length === 0" class="muted">暂无收藏地点。</p>
        <ul v-else class="simple-list">
          <li v-for="favorite in favorites" :key="favorite.placeId">
            <RouterLink :to="`/places/${favorite.placeId}`">{{ favorite.placeName }}</RouterLink>
            <span>{{ formatDate(favorite.createdAt) }}</span>
          </li>
        </ul>
      </div>

      <div v-else-if="activeTab === 'checkins'" class="tab-content">
        <div class="section-heading">
          <h3>我的打卡</h3>
          <button class="ghost-button" type="button" :disabled="checkinsLoading" @click="doLoadCheckins">
            {{ checkinsLoading ? '加载中...' : '刷新' }}
          </button>
        </div>
        <p v-if="!checkins || checkins.length === 0" class="muted">暂无打卡记录。</p>
        <ul v-else class="simple-list">
          <li v-for="checkin in checkins" :key="checkin.id">
            <RouterLink :to="`/places/${checkin.placeId}`">{{ checkin.placeName }}</RouterLink>
            <div class="item-meta">
              <span>{{ formatDate(checkin.createdAt) }}</span>
            </div>
            <div class="item-preview">
              <span>拥挤 {{ crowdLevelLabel(checkin.crowdLevel) }}</span>
              <span>噪音 {{ noiseLevelLabel(checkin.noiseLevel) }}</span>
              <span>有座 {{ checkin.hasSeat ? '是' : '否' }}</span>
            </div>
            <p v-if="checkin.remark" class="item-remark">{{ checkin.remark }}</p>
          </li>
        </ul>
      </div>

      <div v-else-if="activeTab === 'reviews'" class="tab-content">
        <div class="section-heading">
          <h3>我的评价</h3>
          <button class="ghost-button" type="button" :disabled="reviewsLoading" @click="doLoadReviews">
            {{ reviewsLoading ? '加载中...' : '刷新' }}
          </button>
        </div>
        <p v-if="!reviews || reviews.length === 0" class="muted">暂无评价记录。</p>
        <ul v-else class="simple-list">
          <li v-for="review in reviews" :key="review.id">
            <RouterLink :to="`/places/${review.placeId}`">{{ review.placeName }}</RouterLink>
            <div class="item-meta">
              <span :class="['audit-badge', auditStatusClass(review.auditStatus)]">{{ auditStatusLabel(review.auditStatus) }}</span>
              <span class="muted">{{ formatDate(review.createdAt) }}</span>
            </div>
            <div class="item-preview">
              <span>安静 {{ review.quietScore }}</span>
              <span>Wi-Fi {{ review.wifiScore }}</span>
              <span>插座 {{ review.socketScore }}</span>
              <span>舒适 {{ review.comfortScore }}</span>
              <span>性价比 {{ review.costScore }}</span>
              <span v-if="review.seatScore !== undefined">座位 {{ review.seatScore }}</span>
              <span v-if="review.minConsumption !== undefined">最低消费 ¥{{ review.minConsumption }}</span>
              <span v-if="review.allowLongStay">{{ allowLongStayLabel(review.allowLongStay) }}</span>
            </div>
            <div v-if="review.suitableScenes && review.suitableScenes.length > 0" class="scene-tags">
              <span v-for="scene in review.suitableScenes" :key="scene" class="scene-tag">{{ sceneLabel(scene) }}</span>
            </div>
            <p class="item-content">{{ review.content }}</p>
            <div v-if="review.images && review.images.length > 0" class="thumb-list">
              <img
                v-for="img in review.images" :key="img.id"
                :src="img.imageUrl"
                :alt="img.description || ''"
                class="thumb"
              />
            </div>
          </li>
        </ul>
      </div>
    </div>
  </section>
</template>

<style scoped>
.login-prompt {
  display: grid;
  gap: 12px;
  justify-items: start;
}

.nickname-edit {
  display: grid;
  gap: 8px;
  margin-top: 4px;
}

.nickname-actions {
  display: flex;
  gap: 8px;
}

.text-button {
  margin-left: 8px;
  padding: 4px 8px;
  font-size: 13px;
  border: 1px solid #cfd6e1;
  border-radius: 6px;
  background: #ffffff;
  color: #0f5f59;
  cursor: pointer;
}

.tab-nav {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 14px;
  border-bottom: 1px solid #eaecf0;
  padding-bottom: 10px;
}

.tab-button {
  padding: 8px 12px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  color: #475467;
  cursor: pointer;
  font-size: 14px;
}

.tab-button:hover {
  background: #f6f7f9;
}

.tab-button.active {
  background: #e8f3f1;
  color: #0f5f59;
  font-weight: 600;
}

.tab-content {
  min-height: 120px;
}

.item-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 4px;
}

.audit-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.status-pending {
  background: #fef3c7;
  color: #92400e;
}

.status-approved {
  background: #d1fae5;
  color: #065f46;
}

.status-rejected {
  background: #fee2e2;
  color: #991b1b;
}

.item-preview {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 6px;
  font-size: 13px;
  color: #475467;
}

.item-remark,
.item-content {
  margin: 6px 0 0;
  font-size: 13px;
  color: #344054;
  line-height: 1.5;
}

.thumb-list {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 8px;
}

.scene-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 6px;
}

.scene-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  background: #e8f3f1;
  color: #0f5f59;
}

.thumb {
  width: 80px;
  height: 60px;
  border-radius: 6px;
  object-fit: cover;
  background: #eef2f6;
}

@media (max-width: 780px) {
  .tab-nav {
    gap: 6px;
  }

  .tab-button {
    padding: 6px 8px;
    font-size: 13px;
  }

  .item-preview {
    gap: 6px;
  }
}
</style>
