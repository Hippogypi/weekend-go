<script setup lang="ts">
import { useToast } from '../composables';

const { toasts, removeToast } = useToast();
</script>

<template>
  <div class="toast-container">
    <div
      v-for="toast in toasts"
      :key="toast.id"
      :class="['notice', toast.type]"
      @click="removeToast(toast.id)"
    >
      {{ toast.message }}
    </div>
  </div>
</template>

<style scoped>
.toast-container {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 1000;
  display: grid;
  gap: 10px;
  max-width: 360px;
}

.notice {
  cursor: pointer;
  animation: slideIn 0.2s ease;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateX(20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}
</style>
