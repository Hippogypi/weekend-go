<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { sessionStore, weekendGoApi } from '../services';
import { useApiError } from '../composables';

const route = useRoute();
const router = useRouter();
const placeId = computed(() => String(route.params.placeId ?? ''));

const reviewForm = reactive({
  quietScore: 4,
  wifiScore: 4,
  socketScore: 4,
  comfortScore: 4,
  costScore: 3,
  content: ''
});

const showAttributes = ref(false);
const attrForm = reactive({
  seatScore: undefined as number | undefined,
  minConsumption: undefined as number | undefined,
  allowLongStay: null as string | null,
  suitableScenes: [] as string[]
});

const sceneOptions = [
  { value: 'SELF_STUDY', label: '自习' },
  { value: 'READING', label: '阅读' },
  { value: 'REMOTE_WORK', label: '远程办公' },
  { value: 'TEMPORARY_WORK', label: '临时办公' },
  { value: 'GROUP_DISCUSSION', label: '小组讨论' },
  { value: 'VIDEO_MEETING', label: '视频会议' }
];

const imageList = ref<Array<{ imageUrl: string; description: string }>>([]);
const showAddImage = ref(false);
const newImageUrl = ref('');
const newImageDesc = ref('');

const loading = ref(false);
const message = ref('');
const { errorMessage, setError, clearError } = useApiError();

function goBack(): void {
  router.push(`/places/${placeId.value}`);
}

function toggleScene(sceneValue: string): void {
  const idx = attrForm.suitableScenes.indexOf(sceneValue);
  if (idx >= 0) {
    attrForm.suitableScenes.splice(idx, 1);
  } else {
    attrForm.suitableScenes.push(sceneValue);
  }
}

function addImage(): void {
  const url = newImageUrl.value.trim();
  if (!url) return;
  imageList.value.push({
    imageUrl: url,
    description: newImageDesc.value.trim()
  });
  newImageUrl.value = '';
  newImageDesc.value = '';
  showAddImage.value = false;
}

function removeImage(index: number): void {
  imageList.value.splice(index, 1);
}

async function submitReview(): Promise<void> {
  if (!sessionStore.isLoggedIn.value) {
    router.push({ path: '/login', query: { redirect: route.fullPath } });
    return;
  }

  loading.value = true;
  clearError();
  message.value = '';

  try {
    const body: import('../services').ReviewRequest = {
      quietScore: Number(reviewForm.quietScore),
      wifiScore: Number(reviewForm.wifiScore),
      socketScore: Number(reviewForm.socketScore),
      comfortScore: Number(reviewForm.comfortScore),
      costScore: Number(reviewForm.costScore),
      content: reviewForm.content
    };

    if (showAttributes.value) {
      if (attrForm.seatScore !== undefined) body.seatScore = attrForm.seatScore;
      if (attrForm.minConsumption !== undefined) body.minConsumption = attrForm.minConsumption;
      if (attrForm.allowLongStay !== null) body.allowLongStay = attrForm.allowLongStay;
      if (attrForm.suitableScenes.length > 0) body.suitableScenes = attrForm.suitableScenes;
    }

    if (imageList.value.length > 0) {
      body.images = imageList.value;
    }

    const review = await weekendGoApi.submitReview(placeId.value, body);
    message.value = `评价已提交，审核状态：${review.auditStatus}`;
    reviewForm.content = '';
    attrForm.seatScore = undefined;
    attrForm.minConsumption = undefined;
    attrForm.allowLongStay = null;
    attrForm.suitableScenes = [];
    showAttributes.value = false;
    imageList.value = [];
  } catch (err) {
    setError(err);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <section class="page">
    <header class="page-header">
      <div>
        <h1 class="page-title">写评价</h1>
        <p class="page-subtitle">分享你对这个地方的整体体验。</p>
      </div>
      <button class="ghost-button" type="button" @click="goBack">返回详情</button>
    </header>

    <p v-if="errorMessage" class="notice error">{{ errorMessage }}</p>
    <p v-if="message" class="notice success">{{ message }}</p>

    <form class="panel form-panel" @submit.prevent="submitReview">
      <div class="compact-grid">
        <label class="field stacked">
          <span>安静程度</span>
          <input v-model.number="reviewForm.quietScore" type="number" min="1" max="5" step="0.5" />
        </label>
        <label class="field stacked">
          <span>Wi-Fi</span>
          <input v-model.number="reviewForm.wifiScore" type="number" min="1" max="5" step="0.5" />
        </label>
        <label class="field stacked">
          <span>插座</span>
          <input v-model.number="reviewForm.socketScore" type="number" min="1" max="5" step="0.5" />
        </label>
        <label class="field stacked">
          <span>舒适度</span>
          <input v-model.number="reviewForm.comfortScore" type="number" min="1" max="5" step="0.5" />
        </label>
        <label class="field stacked">
          <span>性价比</span>
          <input v-model.number="reviewForm.costScore" type="number" min="1" max="5" step="0.5" />
        </label>
      </div>
      <label class="field stacked">
        <span>评价内容</span>
        <textarea v-model="reviewForm.content" rows="4" placeholder="例如：周末下午人很多，但插座充足，Wi-Fi稳定，适合带电脑工作...（选填，可空）"></textarea>
      </label>

      <div class="attributes-section">
        <button
          type="button"
          class="text-button toggle-button"
          @click="showAttributes = !showAttributes"
        >
          {{ showAttributes ? '折叠' : '展开' }} 补充客观属性
        </button>
        <div v-if="showAttributes" class="panel attributes-panel">
          <label class="field stacked">
            <span>座位评分</span>
            <input v-model.number="attrForm.seatScore" type="number" min="1" max="5" step="0.5" placeholder="选填，1-5" />
          </label>
          <label class="field stacked">
            <span>最低消费（元）</span>
            <input v-model.number="attrForm.minConsumption" type="number" min="0" placeholder="例如 30" />
          </label>
          <div class="field">
            <span>适合久坐</span>
            <div class="radio-group">
              <label class="check-field">
                <input v-model="attrForm.allowLongStay" type="radio" name="allowLongStay" :value="'TRUE'" />
                是
              </label>
              <label class="check-field">
                <input v-model="attrForm.allowLongStay" type="radio" name="allowLongStay" :value="'FALSE'" />
                否
              </label>
              <label class="check-field">
                <input v-model="attrForm.allowLongStay" type="radio" name="allowLongStay" :value="null" />
                不确定
              </label>
            </div>
          </div>
          <div class="field">
            <span>适合场景</span>
            <div class="scene-tags">
              <button
                v-for="scene in sceneOptions"
                :key="scene.value"
                type="button"
                class="tag-button"
                :class="{ active: attrForm.suitableScenes.includes(scene.value) }"
                @click="toggleScene(scene.value)"
              >
                {{ scene.label }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <div class="images-section">
        <div class="section-heading">
          <h3>已添加图片</h3>
          <span v-if="imageList.length === 0" class="muted">暂无</span>
        </div>
        <div v-if="imageList.length > 0" class="image-list">
          <div v-for="(img, idx) in imageList" :key="idx" class="image-item">
            <img :src="img.imageUrl" :alt="img.description || '评价图片'" />
            <input
              v-model="img.description"
              type="text"
              placeholder="图片描述"
            />
            <button type="button" class="ghost-button remove-button" @click="removeImage(idx)">删除</button>
          </div>
        </div>
        <button
          v-if="!showAddImage"
          type="button"
          class="text-button add-image-button"
          @click="showAddImage = true"
        >
          + 添加图片
        </button>
        <div v-else class="panel add-image-panel">
          <label class="field stacked">
            <span>图片 URL</span>
            <input v-model="newImageUrl" type="url" placeholder="https://example.com/image.jpg" />
          </label>
          <label class="field stacked">
            <span>描述</span>
            <input v-model="newImageDesc" type="text" placeholder="例如：座位区" />
          </label>
          <div class="add-image-actions">
            <button type="button" class="ghost-button" @click="showAddImage = false">取消</button>
            <button type="button" class="primary-button" :disabled="!newImageUrl.trim()" @click="addImage">确认添加</button>
          </div>
        </div>
      </div>

      <button class="primary-button" type="submit" :disabled="loading">
        {{ loading ? '提交中...' : '提交评价' }}
      </button>
    </form>
  </section>
</template>

<style scoped>
.toggle-button {
  justify-self: start;
}

.attributes-panel {
  display: grid;
  gap: 12px;
  margin-top: 10px;
}

.radio-group {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.scene-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.tag-button {
  border: 1px solid #cfd6e1;
  border-radius: 999px;
  background: #ffffff;
  color: #475467;
  padding: 6px 12px;
  font-size: 13px;
}

.tag-button.active {
  border-color: #0f766e;
  background: #e8f3f1;
  color: #0f5f59;
}

.images-section .section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.images-section .section-heading h3 {
  margin: 0;
  font-size: 14px;
  color: #101828;
}

.image-list {
  display: grid;
  gap: 12px;
  margin-bottom: 12px;
}

.image-item {
  display: grid;
  grid-template-columns: 80px 1fr auto;
  gap: 10px;
  align-items: center;
  border: 1px solid #eaecf0;
  border-radius: 8px;
  padding: 10px;
  background: #ffffff;
}

.image-item img {
  width: 80px;
  height: 60px;
  border-radius: 6px;
  object-fit: cover;
  background: #eef2f6;
}

.image-item input {
  min-width: 0;
}

.remove-button {
  padding: 8px 10px;
  font-size: 13px;
}

.add-image-button {
  justify-self: start;
}

.add-image-panel {
  display: grid;
  gap: 10px;
}

.add-image-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}
</style>
