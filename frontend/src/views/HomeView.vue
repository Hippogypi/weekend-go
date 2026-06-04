<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { RouterLink, useRouter } from 'vue-router';

import { weekendGoApi } from '../services';
import type { Place } from '../services';
import { workspaceStatusLabel } from '../services/displayLabels';
import { useAsyncAction, useApiError } from '../composables';
import MapView from '../components/MapView.vue';

const router = useRouter();

const keyword = ref('');
const city = ref('');
const longitude = ref('');
const latitude = ref('');
const hasSearched = ref(false);
const locationSuccess = ref(false);

const currentPage = ref(1);
const pageSize = ref(10);
const allPlaces = ref<Place[]>([]);
const hasMore = ref(false);

const { errorMessage, setError, clearError } = useApiError();

const {
  loading,
  data: places,
  execute: loadPlaces
} = useAsyncAction<Place[]>({
  onError: (msg) => setError(new Error(msg))
});

function requestLocation(): void {
  if (!navigator.geolocation) {
    setError(new Error('您的浏览器不支持定位功能'));
    return;
  }
  navigator.geolocation.getCurrentPosition(
    (position) => {
      latitude.value = String(position.coords.latitude);
      longitude.value = String(position.coords.longitude);
      locationSuccess.value = true;
    },
    (err) => {
      console.warn('定位失败：' + err.message);
    }
  );
}

onMounted(() => {
  requestLocation();
});

async function doSearch(reset = true): Promise<void> {
  clearError();
  hasSearched.value = true;

  if (!keyword.value.trim()) {
    setError(new Error('请输入搜索关键词'));
    return;
  }

  if (reset) {
    currentPage.value = 1;
    allPlaces.value = [];
  }

  await loadPlaces(() =>
    weekendGoApi.searchPlaces({
      keyword: keyword.value.trim(),
      city: city.value.trim() || undefined,
      page: currentPage.value,
      offset: pageSize.value
    })
  );

  if (places.value) {
    if (reset) {
      allPlaces.value = places.value;
    } else {
      allPlaces.value = [...allPlaces.value, ...places.value];
    }
    hasMore.value = places.value.length === pageSize.value;
  }
}

async function loadMore(): Promise<void> {
  if (loading.value || !hasMore.value) return;
  currentPage.value += 1;
  await doSearch(false);
}
</script>

<template>
  <section class="page home-page">
    <!-- 标题区 -->
    <header class="page-header home-header">
      <div>
        <h1 class="page-title">地点发现</h1>
        <p class="page-subtitle">搜索或查看附近适合学习办公的空间。</p>
      </div>
      <span class="status-pill">城市学习办公空间</span>
    </header>

    <!-- 搜索卡片 -->
    <div class="card search-card">
      <form class="search-form" @submit.prevent="doSearch()">
        <div class="search-fields">
          <div class="input-group">
            <span class="input-icon">🔍</span>
            <input v-model="keyword" placeholder="图书馆 / 咖啡店 / 自习室" />
          </div>

          <div class="input-group">
            <span class="input-icon">🏙️</span>
            <input v-model="city" placeholder="城市，如济南" />
          </div>
        </div>

        <button class="search-btn" type="submit" :disabled="loading">
          <span v-if="loading" class="btn-spinner"></span>
          <span v-else class="btn-icon">🔎</span>
          {{ loading ? '查询中...' : '查询地点' }}
        </button>
      </form>
    </div>

    <!-- 定位成功提示 -->
    <div v-if="locationSuccess" class="card location-bar success">
      <span class="status-dot"></span>
      <span>已定位到当前位置</span>
    </div>

    <!-- 地图卡片 -->
    <div v-if="longitude && latitude" class="card map-card">
      <div class="card-header">
        <span class="card-header-title">🗺️ 地图预览</span>
        <span v-if="allPlaces.length > 0" class="badge">{{ allPlaces.length }} 个地点</span>
      </div>
      <MapView
        :places="allPlaces"
        :center="[parseFloat(longitude), parseFloat(latitude)]"
        @open-detail="id => router.push(`/places/${id}`)"
      />
    </div>

    <!-- 错误提示 -->
    <div v-if="errorMessage" class="card error-card">
      <span class="error-icon">⚠️</span>
      <p>{{ errorMessage }}</p>
    </div>

    <!-- 加载状态 -->
    <div v-else-if="loading && allPlaces.length === 0" class="card loading-card">
      <div class="spinner"></div>
      <p>正在搜索合适的学习空间...</p>
    </div>

    <!-- 空状态 -->
    <div v-else-if="hasSearched && !loading && allPlaces.length === 0" class="card empty-card">
      <div class="empty-illustration">📚</div>
      <h3>暂无搜索结果</h3>
      <p>换个关键词或城市试试</p>
    </div>

    <!-- 结果列表 -->
    <div v-if="allPlaces.length > 0" class="results-section">
      <h2 class="section-title">
        搜索结果
        <span class="result-count">{{ allPlaces.length }}</span>
      </h2>
      <div class="place-list">
        <article
          v-for="place in allPlaces"
          :key="place.id"
          class="place-card-item"
        >
          <div class="place-main">
            <h3 class="place-name">{{ place.name }}</h3>
            <p class="place-address">📍 {{ place.address || '暂无地址' }}</p>
            <div class="place-tags">
              <span class="tag" :class="place.workspaceStatus ? 'tag-primary' : 'tag-muted'">
                {{ workspaceStatusLabel(place.workspaceStatus) }}
              </span>
              <span v-if="place.workspaceProfile?.score" class="tag tag-amber">
                ⭐ {{ place.workspaceProfile.score }}
              </span>
            </div>
          </div>
          <div class="place-action">
            <span class="place-city">{{ place.city || place.district || '未知' }}</span>
            <RouterLink class="detail-link" :to="`/places/${place.id}`">
              查看详情 <span class="arrow">→</span>
            </RouterLink>
          </div>
        </article>
      </div>
    </div>

    <!-- 加载更多 -->
    <div v-if="hasSearched && allPlaces.length > 0" class="load-more-wrap">
      <button v-if="hasMore" class="ghost-button load-more-btn" type="button" :disabled="loading" @click="loadMore">
        <span v-if="loading" class="btn-spinner"></span>
        {{ loading ? '加载中...' : '加载更多' }}
      </button>
      <p v-else class="muted end-hint">已显示全部结果</p>
    </div>
  </section>
</template>

<style scoped>
/* ===== 标题区 ===== */
.home-header .page-title {
  font-size: 32px;
  font-weight: 800;
  letter-spacing: -0.5px;
}

.home-header .page-subtitle {
  color: #94a3b8;
  font-size: 15px;
  margin-top: 6px;
}

/* ===== 通用卡片 ===== */
.card {
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 4px 12px rgba(0, 0, 0, 0.03);
  padding: 24px;
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}

.card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06), 0 8px 24px rgba(0, 0, 0, 0.04);
}

/* ===== 搜索卡片 ===== */
.search-card {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ===== 定位状态条 ===== */
.location-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 20px;
  font-size: 14px;
  color: #475569;
}

.location-bar.success {
  background: #f0fdf4;
  border-color: #bbf7d0;
  color: #15803d;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
  flex-shrink: 0;
}

/* ===== 搜索表单 ===== */
.search-form {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  flex-wrap: wrap;
}

.search-fields {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  flex: 1;
  min-width: 0;
  flex-wrap: wrap;
}

.input-group {
  position: relative;
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 200px;
}

.input-icon {
  position: absolute;
  left: 12px;
  font-size: 16px;
  color: #94a3b8;
  pointer-events: none;
}

.input-group input {
  width: 100%;
  padding: 10px 12px 10px 38px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #f8fafc;
  font-size: 14px;
  color: #1e293b;
  transition: all 0.2s ease;
}

.input-group input:hover {
  border-color: #cbd5e1;
  background: #ffffff;
}

.input-group input:focus {
  outline: none;
  border-color: #0f766e;
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(15, 118, 110, 0.08);
}

.search-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 24px;
  border-radius: 12px;
  border: none;
  background: linear-gradient(135deg, #0f766e 0%, #14b8a6 100%);
  color: #ffffff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s ease;
  box-shadow: 0 2px 8px rgba(15, 118, 110, 0.25);
}

.search-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(15, 118, 110, 0.35);
}

.search-btn:active:not(:disabled) {
  transform: translateY(0);
}

.search-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.btn-icon {
  font-size: 15px;
}

/* ===== 地图卡片 ===== */
.map-card {
  padding: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.card-header-title {
  font-size: 15px;
  font-weight: 700;
  color: #334155;
}

.badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: #e8f3f1;
  color: #0f5f59;
  font-size: 12px;
  font-weight: 600;
}

/* ===== 错误/加载/空状态卡片 ===== */
.error-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: #fef2f2;
  border-color: #fecaca;
  color: #991b1b;
  font-size: 14px;
}

.error-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.loading-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 48px 24px;
  color: #64748b;
}

.empty-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 56px 24px;
  text-align: center;
}

.empty-illustration {
  font-size: 56px;
  line-height: 1;
  margin-bottom: 4px;
}

.empty-card h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #334155;
}

.empty-card p {
  margin: 0;
  color: #94a3b8;
  font-size: 14px;
  max-width: 320px;
}

/* ===== Spinner ===== */
.spinner,
.btn-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.btn-spinner {
  width: 14px;
  height: 14px;
  border-color: rgba(255, 255, 255, 0.3);
  border-top-color: #ffffff;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ===== 结果列表 ===== */
.results-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #334155;
}

.result-count {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: 999px;
  background: #e8f3f1;
  color: #0f5f59;
  font-size: 13px;
  font-weight: 600;
}

.place-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.place-card-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 20px;
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  transition: all 0.2s ease;
}

.place-card-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.06);
  border-color: #cbd5e1;
}

.place-main {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
  flex: 1;
}

.place-name {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.place-address {
  margin: 0;
  font-size: 13px;
  color: #64748b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.place-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.tag {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.tag-muted {
  background: #f1f5f9;
  color: #64748b;
}

.tag-primary {
  background: #e8f3f1;
  color: #0f5f59;
}

.tag-amber {
  background: #fffbeb;
  color: #b45309;
}

.place-action {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
  flex-shrink: 0;
}

.place-city {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 500;
}

.detail-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 8px 16px;
  border-radius: 10px;
  background: #f8fafc;
  color: #0f766e;
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
  border: 1px solid #e2e8f0;
  transition: all 0.2s ease;
}

.detail-link:hover {
  background: #e8f3f1;
  border-color: #0f766e;
}

.arrow {
  transition: transform 0.2s ease;
}

.detail-link:hover .arrow {
  transform: translateX(2px);
}

/* ===== 加载更多 ===== */
.load-more-wrap {
  display: flex;
  justify-content: center;
  padding: 8px 0;
}

.load-more-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 28px;
  border-radius: 12px;
  font-weight: 600;
  transition: all 0.2s ease;
}

.load-more-btn:hover:not(:disabled) {
  background: #e8f3f1;
  color: #0f5f59;
  border-color: #0f766e;
}

.end-hint {
  font-size: 14px;
  color: #94a3b8;
}

/* ===== 响应式 ===== */
@media (max-width: 780px) {
  .search-form {
    flex-direction: column;
    align-items: stretch;
  }

  .search-fields {
    flex-direction: column;
    align-items: stretch;
  }

  .input-group {
    min-width: auto;
  }

  .search-btn {
    width: 100%;
  }

  .place-card-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .place-action {
    flex-direction: row;
    align-items: center;
    width: 100%;
    justify-content: space-between;
  }
}
</style>
