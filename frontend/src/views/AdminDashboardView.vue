<script setup lang="ts">
import { ref, watch, computed } from 'vue';

import { weekendGoApi } from '../services';
import { useAsyncAction, useApiError, useToast } from '../composables';
import type { PendingAuditItem, PageResult, AuditStats } from '../services/weekendGoApi';

const { errorMessage, setError, clearError } = useApiError();
const toast = useToast();

const activeTab = ref<'review' | 'image'>('review');
const stats = ref<AuditStats | null>(null);
const pendingResult = ref<PageResult<PendingAuditItem> | null>(null);
const currentPage = ref(1);
const pageSize = ref(20);
const auditingId = ref<number | null>(null);

const tabs = [
  { key: 'review' as const, label: '⭐ 评价' },
  { key: 'image' as const, label: '🖼️ 图片' }
];

const totalPages = computed(() => {
  if (!pendingResult.value) return 0;
  return Math.ceil(pendingResult.value.total / pendingResult.value.size);
});

const { execute: loadStats } = useAsyncAction<AuditStats>({
  onError: (msg) => setError(new Error(msg))
});

const { loading: listLoading, execute: loadList } = useAsyncAction<PageResult<PendingAuditItem>>({
  onError: (msg) => setError(new Error(msg))
});

async function fetchStats(): Promise<void> {
  clearError();
  await loadStats(async () => {
    const result = await weekendGoApi.auditStats();
    stats.value = result;
    return result;
  });
}

async function fetchPendingList(): Promise<void> {
  clearError();
  await loadList(async () => {
    const result = await weekendGoApi.pendingList(activeTab.value, currentPage.value, pageSize.value);
    pendingResult.value = result;
    return result;
  });
}

async function goToPage(page: number): Promise<void> {
  if (page < 1 || (totalPages.value > 0 && page > totalPages.value)) return;
  currentPage.value = page;
  await fetchPendingList();
}

async function approveItem(item: PendingAuditItem): Promise<void> {
  await auditItem(item, 'APPROVED');
}

async function rejectItem(item: PendingAuditItem): Promise<void> {
  await auditItem(item, 'REJECTED');
}

async function auditItem(item: PendingAuditItem, status: 'APPROVED' | 'REJECTED'): Promise<void> {
  clearError();
  auditingId.value = item.id;
  try {
    if (item.type === 'review') {
      await weekendGoApi.auditReview(item.id, { auditStatus: status, reason: 'ok' });
    } else if (item.type === 'image') {
      await weekendGoApi.auditImage(item.id, { auditStatus: status, reason: 'ok' });
    }
    toast.showToast(`${getTypeLabel(item.type)} ${item.id} 已${status === 'APPROVED' ? '通过' : '驳回'}`, 'success');
    await fetchStats();
    await fetchPendingList();
  } catch (err) {
    const message = err instanceof Error ? err.message : '审核失败';
    setError(new Error(message));
  } finally {
    auditingId.value = null;
  }
}

function getTypeLabel(type: string): string {
  switch (type) {
    case 'review': return '评价';
    case 'image': return '图片';
    default: return type;
  }
}

function formatDate(dateStr: string): string {
  if (!dateStr) return '-';
  const d = new Date(dateStr);
  return d.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
}

watch(activeTab, () => {
  currentPage.value = 1;
  pendingResult.value = null;
  fetchPendingList();
});

fetchStats();
fetchPendingList();
</script>

<template>
  <section class="page">
    <header class="page-header">
      <div>
        <h1 class="page-title">审核工作台</h1>
        <p class="page-subtitle">管理待审核的评价与图片。</p>
      </div>
      <span class="status-pill">ADMIN</span>
    </header>

    <p v-if="errorMessage" class="notice error">{{ errorMessage }}</p>

    <!-- Stats Cards -->
    <div class="panel-grid">
      <div class="panel">
        <h3>待审核评价</h3>
        <div class="metric">{{ stats?.pendingReviews ?? 0 }}</div>
      </div>
      <div class="panel">
        <h3>待审核图片</h3>
        <div class="metric">{{ stats?.pendingImages ?? 0 }}</div>
      </div>
      <div class="panel">
        <h3>今日处理</h3>
        <div class="metric">{{ (stats?.todayApproved ?? 0) + (stats?.todayRejected ?? 0) }}</div>
        <p class="muted">通过 {{ stats?.todayApproved ?? 0 }} / 驳回 {{ stats?.todayRejected ?? 0 }}</p>
      </div>
    </div>

    <!-- Tab Navigation -->
    <div class="segmented">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- Pending List Table -->
    <div class="panel">
      <div v-if="listLoading && !pendingResult" class="muted">加载中...</div>
      <table v-else class="placeholder-table">
        <thead>
          <tr>
            <th>地点名称</th>
            <th>提交人</th>
            <th>提交时间</th>
            <th>内容预览</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="!pendingResult?.items.length">
            <td colspan="5" class="muted" style="text-align: center;">暂无待审核项</td>
          </tr>
          <tr v-for="item in pendingResult?.items" :key="item.id">
            <td>{{ item.placeName }}</td>
            <td>{{ item.username }}</td>
            <td>{{ formatDate(item.createdAt) }}</td>
            <td>
              <span v-if="item.type === 'image'" class="muted">{{ item.content || item.id }}</span>
              <span v-else>{{ item.content || '-' }}</span>
            </td>
            <td>
              <div style="display: flex; gap: 8px;">
                <button
                  class="primary-button"
                  style="padding: 6px 10px; min-height: 32px; font-size: 13px;"
                  :disabled="auditingId === item.id"
                  @click="approveItem(item)"
                >
                  通过
                </button>
                <button
                  class="ghost-button"
                  style="padding: 6px 10px; min-height: 32px; font-size: 13px;"
                  :disabled="auditingId === item.id"
                  @click="rejectItem(item)"
                >
                  驳回
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Pagination -->
      <div
        v-if="pendingResult && pendingResult.total > 0"
        style="display: flex; align-items: center; justify-content: space-between; margin-top: 14px;"
      >
        <span class="muted">
          共 {{ pendingResult.total }} 条，第 {{ pendingResult.page }} / {{ totalPages }} 页
        </span>
        <div style="display: flex; gap: 8px;">
          <button
            class="ghost-button"
            style="padding: 6px 12px; min-height: 32px; font-size: 13px;"
            :disabled="currentPage <= 1 || listLoading"
            @click="goToPage(currentPage - 1)"
          >
            上一页
          </button>
          <button
            class="ghost-button"
            style="padding: 6px 12px; min-height: 32px; font-size: 13px;"
            :disabled="currentPage >= totalPages || listLoading"
            @click="goToPage(currentPage + 1)"
          >
            下一页
          </button>
        </div>
      </div>
    </div>
  </section>
</template>
