<script setup lang="ts">
import { ref, watch } from 'vue';
import { RouterLink, useRouter } from 'vue-router';

import { weekendGoApi } from '../services';
import type { Place } from '../services';
import { useAsyncAction, useApiError } from '../composables';
import MapView from '../components/MapView.vue';

const router = useRouter();

const keyword = ref('');
const city = ref('');
const longitude = ref('');
const latitude = ref('');
const radius = ref(1000);
const searchMode = ref<'keyword' | 'nearby'>('keyword');
const hasSearched = ref(false);

const currentPage = ref(1);
const pageSize = ref(10);
const allPlaces = ref<Place[]>([]);
const hasMore = ref(false);

const locationStatus = ref<'idle' | 'loading' | 'success' | 'error'>('idle');
const locationError = ref('');

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
    locationStatus.value = 'error';
    locationError.value = '您的浏览器不支持定位功能';
    return;
  }
  locationStatus.value = 'loading';
  locationError.value = '';
  navigator.geolocation.getCurrentPosition(
    (position) => {
      latitude.value = String(position.coords.latitude);
      longitude.value = String(position.coords.longitude);
      locationStatus.value = 'success';
    },
    (err) => {
      locationStatus.value = 'error';
      locationError.value = '定位失败：' + err.message;
    }
  );
}

watch(searchMode, (mode) => {
  if (mode === 'nearby') {
    requestLocation();
  }
});

async function doSearch(reset = true): Promise<void> {
  clearError();
  hasSearched.value = true;

  if (searchMode.value === 'keyword' && !keyword.value.trim()) {
    setError(new Error('请输入搜索关键词'));
    return;
  }
  if (searchMode.value === 'nearby' && (!longitude.value.trim() || !latitude.value.trim())) {
    setError(new Error('请允许浏览器定位或手动输入经纬度'));
    return;
  }

  if (reset) {
    currentPage.value = 1;
    allPlaces.value = [];
  }

  await loadPlaces(() =>
    searchMode.value === 'keyword'
      ? weekendGoApi.searchPlaces({
          keyword: keyword.value.trim(),
          city: city.value.trim() || undefined,
          page: currentPage.value,
          offset: pageSize.value
        })
      : weekendGoApi.nearbyPlaces({
          longitude: longitude.value.trim(),
          latitude: latitude.value.trim(),
          keyword: keyword.value.trim() || undefined,
          radius: radius.value,
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
  <section class="page">
    <header class="page-header">
      <div>
        <h1 class="page-title">地点发现</h1>
        <p class="page-subtitle">搜索或查看附近适合学习办公的空间，结果直接来自后端 API。</p>
      </div>
      <span class="status-pill">Demo loop</span>
    </header>

    <form class="toolbar" @submit.prevent="doSearch()">
      <div class="segmented" aria-label="搜索模式">
        <button type="button" :class="{ active: searchMode === 'keyword' }" @click="searchMode = 'keyword'">
          搜索
        </button>
        <button type="button" :class="{ active: searchMode === 'nearby' }" @click="searchMode = 'nearby'">
          附近
        </button>
      </div>

      <label class="field wide">
        <span>关键词</span>
        <input v-model="keyword" placeholder="library / cafe / study" />
      </label>
      <label v-if="searchMode === 'keyword'" class="field">
        <span>城市</span>
        <input v-model="city" placeholder="Shanghai" />
      </label>
      <template v-else>
        <label class="field">
          <span>经度</span>
          <input v-model="longitude" />
        </label>
        <label class="field">
          <span>纬度</span>
          <input v-model="latitude" />
        </label>
      </template>

      <button class="primary-button" type="submit" :disabled="loading">
        {{ loading ? '查询中...' : '查询地点' }}
      </button>
    </form>

    <div
      v-if="searchMode === 'nearby'"
      class="notice"
      :class="{ success: locationStatus === 'success', error: locationStatus === 'error' }"
    >
      <span v-if="locationStatus === 'loading'">正在获取您的位置...</span>
      <span v-else-if="locationStatus === 'success'">已定位到当前位置</span>
      <span v-else-if="locationStatus === 'error'">{{ locationError }}</span>
      <span v-else>点击"附近"模式可自动获取当前位置</span>
    </div>

    <MapView
      v-if="hasSearched && !loading && allPlaces.length > 0"
      :places="allPlaces"
      @open-detail="id => router.push(`/places/${id}`)"
    />

    <p v-if="errorMessage" class="notice error">{{ errorMessage }}</p>
    <p v-else-if="loading && allPlaces.length === 0" class="notice">正在加载地点...</p>
    <p v-else-if="hasSearched && !loading && allPlaces.length === 0" class="notice">
      没有找到地点，可换一个关键词或使用本地演示 placeId。
    </p>

    <div class="list-grid">
      <article v-for="place in allPlaces" :key="place.id" class="panel place-card">
        <div>
          <h2>{{ place.name }}</h2>
          <p>{{ place.address || '暂无地址' }}</p>
        </div>
        <dl class="meta-list">
          <div>
            <dt>区域</dt>
            <dd>{{ place.city || place.district || '未知' }}</dd>
          </div>
          <div>
            <dt>状态</dt>
            <dd>{{ place.workspaceStatus || '候选' }}</dd>
          </div>
          <div>
            <dt>评分</dt>
            <dd>{{ place.workspaceProfile?.score ?? '待共建' }}</dd>
          </div>
        </dl>
        <RouterLink class="text-button" :to="`/places/${place.id}`">查看详情</RouterLink>
      </article>
    </div>

    <div v-if="hasSearched && allPlaces.length > 0" class="load-more-wrap">
      <button v-if="hasMore" class="ghost-button" type="button" :disabled="loading" @click="loadMore">
        {{ loading ? '加载中...' : '加载更多' }}
      </button>
      <p v-else class="muted">已显示全部结果</p>
    </div>
  </section>
</template>

<style scoped>
.load-more-wrap {
  display: flex;
  justify-content: center;
  padding: 8px 0;
}
</style>
