<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router';

const navItems = [
  { to: '/', label: '地点' },
  { to: '/places/1', label: '详情' },
  { to: '/profile', label: '账号' },
  { to: '/admin/reviews', label: '审核' }
];
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar" aria-label="主导航">
      <div class="brand">
        <span class="brand-mark">W</span>
        <div>
          <strong>weekend-go</strong>
          <span>城市学习办公空间</span>
        </div>
      </div>

      <nav class="nav-list">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="nav-link"
        >
          {{ item.label }}
        </RouterLink>
      </nav>
    </aside>

    <main class="content">
      <RouterView />
    </main>
  </div>
</template>
