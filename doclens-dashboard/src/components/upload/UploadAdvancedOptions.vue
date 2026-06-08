<script setup lang="ts">
import { NInput, NSelect } from 'naive-ui'

import type { UploadAdvancedOptionsValue } from '@/types/upload'

const model = defineModel<UploadAdvancedOptionsValue>({ required: true })

const pdfModeOptions = [
  { label: '服务端默认', value: '' },
  { label: '页图 OCR', value: 'page_image_fallback' },
  { label: '直接解析', value: 'direct' }
]
</script>

<template>
  <div class="upload-advanced-options">
    <label class="upload-advanced-options__label" for="upload-metadata">元数据 JSON</label>
    <NInput
      id="upload-metadata"
      v-model:value="model.metadata"
      type="textarea"
      :autosize="{ minRows: 3, maxRows: 5 }"
      placeholder="metadata JSON，例如 {}"
    />
    <div class="upload-advanced-options__grid">
      <label class="upload-advanced-options__field">
        <span>回调地址</span>
        <NInput v-model:value="model.callbackUrl" placeholder="callback_url，可选" />
      </label>
      <label class="upload-advanced-options__field">
        <span>幂等键</span>
        <NInput v-model:value="model.idempotencyKey" placeholder="idempotency_key，可选" />
      </label>
      <label class="upload-advanced-options__field">
        <span>OCR 适配器</span>
        <NInput v-model:value="model.adapterOverride" placeholder="adapter_override，可选" />
      </label>
      <label class="upload-advanced-options__field">
        <span>PDF 模式</span>
        <NSelect v-model:value="model.pdfMode" :options="pdfModeOptions" />
      </label>
    </div>
  </div>
</template>

<style scoped>
.upload-advanced-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.upload-advanced-options__label,
.upload-advanced-options__field span {
  color: var(--ink-soft);
  font-size: 12px;
  font-weight: 700;
}

.upload-advanced-options__field {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 6px;
}

.upload-advanced-options__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

@media (max-width: 760px) {
  .upload-advanced-options__grid {
    grid-template-columns: 1fr;
  }
}
</style>
