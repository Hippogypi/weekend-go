<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink, RouterView } from 'vue-router';

import ToastContainer from './components/ToastContainer.vue';
import { provideToast } from './composables';
import { sessionStore } from './services';

provideToast();

const navItems = computed(() => {
  const items = [
    { to: '/', label: '发现' },
    { to: '/profile', label: '我的' }
  ];
  if (sessionStore.isAdmin.value) {
    items.push({ to: '/admin/reviews', label: '审核' });
  }
  return items;
});
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

  <ToastContainer />
</template>
