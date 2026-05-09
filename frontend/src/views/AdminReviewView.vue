<script setup lang="ts">
import { reactive, ref } from 'vue';

import { ApiError, sessionStore, weekendGoApi } from '../services';

const loading = ref('');
const message = ref('');
const error = ref('');
const form = reactive({
  targetType: 'review',
  targetId: '',
  auditStatus: 'APPROVED',
  reason: 'ok'
});

function describeError(value: unknown): string {
  if (value instanceof ApiError) {
    const payload = value.payload as { message?: string; code?: string } | undefined;
    if (value.status === 401) {
      return '需要先登录管理员账号。';
    }
    if (value.status === 403) {
      return '当前账号不是管理员，无法审核。';
    }
    return payload?.message ?? payload?.code ?? `${value.status} ${value.message}`;
  }

  return value instanceof Error ? value.message : '请求失败，请稍后重试。';
}

async function submitAudit(): Promise<void> {
  if (!sessionStore.isAdmin.value) {
    error.value = '请先在账号页登录 ADMIN 账号。';
    return;
  }

  error.value = '';
  message.value = '';
  loading.value = 'audit';
  try {
    if (form.targetType === 'review') {
      const result = await weekendGoApi.auditReview(form.targetId, {
        auditStatus: form.auditStatus as 'APPROVED' | 'REJECTED',
        reason: form.reason
      });
      message.value = `评价 ${result.id} 已更新为 ${result.auditStatus}`;
    } else if (form.targetType === 'image') {
      const result = await weekendGoApi.auditImage(form.targetId, {
        auditStatus: form.auditStatus as 'APPROVED' | 'REJECTED',
        reason: form.reason
      });
      message.value = `图片 ${result.id} 已更新为 ${result.auditStatus}`;
    } else if (form.auditStatus === 'APPROVED') {
      const result = await weekendGoApi.approveProfileSubmission(form.targetId, form.reason);
      message.value = `属性共建 ${result.id} 已通过`;
    } else {
      const result = await weekendGoApi.rejectProfileSubmission(form.targetId, form.reason);
      message.value = `属性共建 ${result.id} 已驳回`;
    }
  } catch (err) {
    error.value = describeError(err);
  } finally {
    loading.value = '';
  }
}
</script>

<template>
  <section class="page">
    <header class="page-header">
      <div>
        <h1 class="page-title">管理员审核</h1>
        <p class="page-subtitle">根据提交后返回的 id 审核属性共建、评价和图片。</p>
      </div>
      <span class="status-pill">{{ sessionStore.isAdmin.value ? 'ADMIN' : 'No permission' }}</span>
    </header>

    <p v-if="message" class="notice success">{{ message }}</p>
    <p v-if="error" class="notice error">{{ error }}</p>
    <p v-if="!sessionStore.isAdmin.value" class="notice">当前未以管理员身份登录，审核提交会返回权限提示。</p>

    <form class="panel form-panel narrow" @submit.prevent="submitAudit">
      <h2>审核操作</h2>
      <label class="field stacked">
        <span>类型</span>
        <select v-model="form.targetType">
          <option value="review">评价</option>
          <option value="image">图片</option>
          <option value="profile">属性共建</option>
        </select>
      </label>
      <label class="field stacked">
        <span>ID</span>
        <input v-model="form.targetId" required placeholder="提交后返回的 id" />
      </label>
      <label class="field stacked">
        <span>结果</span>
        <select v-model="form.auditStatus">
          <option value="APPROVED">APPROVED</option>
          <option value="REJECTED">REJECTED</option>
        </select>
      </label>
      <label class="field stacked">
        <span>原因</span>
        <textarea v-model="form.reason" rows="3" />
      </label>
      <button class="primary-button" :disabled="loading === 'audit'">
        {{ loading === 'audit' ? '提交中' : '提交审核' }}
      </button>
    </form>

    <section class="panel">
      <h2>审核入口说明</h2>
      <ul class="simple-list">
        <li>
          <strong>属性共建</strong>
          <span>详情页提交后返回 profile submission id，可在这里通过或驳回。</span>
        </li>
        <li>
          <strong>评价 / 图片</strong>
          <span>详情页提交后默认为 PENDING，管理员审核通过后才会出现在公开列表。</span>
        </li>
      </ul>
    </section>
  </section>
</template>
