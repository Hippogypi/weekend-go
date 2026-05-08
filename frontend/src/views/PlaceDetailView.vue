<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();
const placeId = computed(() => String(route.params.placeId ?? ''));
</script>

<template>
  <section class="page">
    <header class="page-header">
      <div>
        <h1 class="page-title">地点详情</h1>
        <p class="page-subtitle">当前地点：{{ placeId }}</p>
      </div>
      <span class="status-pill">Place</span>
    </header>

    <div class="panel-grid">
      <article class="panel">
        <h2>基础信息</h2>
        <p>名称、地址、营业状态和来源信息占位。</p>
      </article>
      <article class="panel">
        <h2>空间属性</h2>
        <p>安静度、插座、Wi-Fi 和座位信息占位。</p>
      </article>
      <article class="panel">
        <h2>互动记录</h2>
        <p>评价、收藏、图片和打卡反馈占位。</p>
      </article>
    </div>
  </section>
</template>
