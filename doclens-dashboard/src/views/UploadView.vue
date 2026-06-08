<script setup lang="ts">
import { useTemplateRef } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { CheckCircle2 } from '@lucide/vue'
import {
  NAlert,
  NButton,
  NIcon,
  NTag,
  useMessage
} from 'naive-ui'

import UploadDropzone from '@/components/dashboard/UploadDropzone.vue'
import { useUploadStore } from '@/stores/upload'
import type { UploadBatchOptions } from '@/types/upload'
import { formatNumber } from '@/utils/formatters'

const router = useRouter()
const message = useMessage()
const uploadStore = useUploadStore()
const { latestBatch, uploadState } = storeToRefs(uploadStore)
const uploadDropzone = useTemplateRef<InstanceType<typeof UploadDropzone>>('uploadDropzone')

async function handleSubmit(options: UploadBatchOptions): Promise<void> {
  if (options.files.length > 0) {
    const result = await uploadStore.submitBatch(options)
    uploadDropzone.value?.resetForm()
    message.success(`批次 ${result.batch_id} 已创建`)
    await router.push({ name: 'batch-detail', params: { batchId: result.batch_id } })
  } else {
    message.warning('请先选择文件')
  }
}

function openLatestBatch(): void {
  if (latestBatch.value) {
    void router.push({ name: 'batch-detail', params: { batchId: latestBatch.value.batch_id } })
  } else {
    // 尚无上传成功批次，无需跳转。
  }
}
</script>

<template>
  <div class="upload-view">
    <section class="panel upload-view__main">
      <div class="panel__header">
        <div>
          <h2 class="panel__title">上传文件</h2>
          <span class="panel__hint">创建批次后自动进入解析队列</span>
        </div>
        <NTag type="info" round>
          multipart
        </NTag>
      </div>
      <NAlert v-if="uploadState.error" type="error" :title="uploadState.error" />
      <UploadDropzone ref="uploadDropzone" :loading="uploadState.loading" @submit="handleSubmit" />
    </section>

    <aside class="panel upload-view__side">
      <div class="panel__header">
        <h2 class="panel__title">最近上传</h2>
      </div>
      <div v-if="latestBatch" class="upload-result">
        <NIcon class="upload-result__icon" :component="CheckCircle2" />
        <strong class="upload-result__batch">{{ latestBatch.batch_id }}</strong>
        <span class="upload-result__meta">
          {{ formatNumber(latestBatch.total_files) }} 个文件 · {{ latestBatch.status }}
        </span>
        <div class="upload-result__documents">
          <span v-for="document in latestBatch.documents" :key="document.document_id">
            {{ document.file_name }}
          </span>
        </div>
        <NButton size="small" type="primary" @click="openLatestBatch">
          查看批次
        </NButton>
      </div>
      <p v-else class="upload-result__empty">暂无上传成功批次</p>
    </aside>
  </div>
</template>

<style scoped>
.upload-view {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 16px;
}

.upload-view__main {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.upload-view__side {
  align-self: start;
}

.upload-result {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 10px;
}

.upload-result__icon {
  color: var(--success);
  font-size: 28px;
}

.upload-result__batch {
  overflow-wrap: anywhere;
  color: var(--ink-strong);
  font-size: 16px;
}

.upload-result__meta,
.upload-result__empty {
  color: var(--ink-muted);
  font-size: 13px;
}

.upload-result__documents {
  display: flex;
  max-height: 180px;
  flex-direction: column;
  gap: 6px;
  overflow: auto;
}

.upload-result__documents span {
  overflow: hidden;
  padding: 7px 8px;
  border-radius: 7px;
  background: var(--surface-inset);
  color: var(--ink-soft);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 980px) {
  .upload-view {
    grid-template-columns: 1fr;
  }
}
</style>
