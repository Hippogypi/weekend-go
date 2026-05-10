<script setup lang="ts">
import { ref } from 'vue';
import { RouterLink } from 'vue-router';

import { weekendGoApi } from '../services';
import type { Place } from '../services';
import { useAsyncAction, useApiError } from '../composables';
import MapView from '../components/MapView.vue';

const keyword = ref('library');
const city = ref('');
const longitude = ref('116.481488');
const latitude = ref('39.990464');
const searchMode = ref<'keyword' | 'nearby'>('keyword');
const hasSearched = ref(false);

const { errorMessage, setError, clearError } = useApiError();

const {
  loading,
  data: places,
  execute: loadPlaces
} = useAsyncAction<Place[]>({
  onError: (msg) => setError(new Error(msg))
});

async function doLoadPlaces(): Promise<void> {
  clearError();
  hasSearched.value = true;

  await loadPlaces(() =>
    searchMode.value === 'keyword'
      ? weekendGoApi.searchPlaces({ keyword: keyword.value.trim(), city: city.value.trim() || undefined })
      : weekendGoApi.nearbyPlaces({
        longitude: longitude.value.trim(),
        latitude: latitude.value.trim(),
        keyword: keyword.value.trim() || undefined
      })
  );
}

doLoadPlaces();
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

    <form class="toolbar" @submit.prevent="doLoadPlaces">
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
        <input v-model="keyword" required placeholder="library / cafe / study" />
      </label>
      <label v-if="searchMode === 'keyword'" class="field">
        <span>城市</span>
        <input v-model="city" placeholder="Shanghai" />
      </label>
      <template v-else>
        <label class="field">
          <span>经度</span>
          <input v-model="longitude" required />
        </label>
        <label class="field">
          <span>纬度</span>
          <input v-model="latitude" required />
        </label>
      </template>

      <button class="primary-button" type="submit" :disabled="loading">
        {{ loading ? '查询中...' : '查询地点' }}
      </button>
    </form>

    <MapView v-if="hasSearched && !loading && places && places.length > 0" :places="places" />

    <p v-if="errorMessage" class="notice error">{{ errorMessage }}</p>
    <p v-else-if="loading" class="notice">正在加载地点...</p>
    <p v-else-if="hasSearched && (!places || places.length === 0)" class="notice">没有找到地点，可换一个关键词或使用本地演示 placeId。</p>

    <div class="list-grid">
      <article v-for="place in places" :key="place.id" class="panel place-card">
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
  </section>
</template>
