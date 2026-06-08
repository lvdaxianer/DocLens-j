<script setup lang="ts">
import { computed, reactive, shallowRef, useTemplateRef } from 'vue'
import { FilePlus2, UploadCloud, X } from '@lucide/vue'
import {
  NButton,
  NIcon,
  NTag
} from 'naive-ui'

import UploadAdvancedOptions from '@/components/upload/UploadAdvancedOptions.vue'
import OcrRoutingSelector from '@/components/upload/OcrRoutingSelector.vue'
import type { UploadAdvancedOptionsValue, UploadBatchOptions, UploadOcrRoutingOptions } from '@/types/upload'
import { formatNumber } from '@/utils/formatters'

const ACCEPTED_FILE_TYPES = '.pdf,.doc,.docx,.md,.markdown,.png,.jpg,.jpeg,.webp,.tif,.tiff,.txt'
const EMPTY_UPLOAD_OPTIONS: UploadBatchOptions = {
  files: [],
  metadata: '{}',
  callbackUrl: '',
  idempotencyKey: '',
  adapterOverride: '',
  pdfMode: '',
  ocrRoutingMode: '',
  ocrModelKey: '',
  ocrNodeId: '',
  ocrLoadBalanceStrategy: ''
}

const emit = defineEmits<{
  submit: [options: UploadBatchOptions]
}>()

defineProps<{
  loading?: boolean
}>()

const fileInput = useTemplateRef<HTMLInputElement>('fileInput')
const ocrRoutingSelector = useTemplateRef<InstanceType<typeof OcrRoutingSelector>>('ocrRoutingSelector')
const selectedFiles = shallowRef<File[]>([])
const form = reactive<UploadAdvancedOptionsValue>({
  metadata: EMPTY_UPLOAD_OPTIONS.metadata,
  callbackUrl: EMPTY_UPLOAD_OPTIONS.callbackUrl,
  idempotencyKey: EMPTY_UPLOAD_OPTIONS.idempotencyKey,
  adapterOverride: EMPTY_UPLOAD_OPTIONS.adapterOverride,
  pdfMode: EMPTY_UPLOAD_OPTIONS.pdfMode
})

const hasFiles = computed(() => selectedFiles.value.length > 0)
const totalSize = computed(() => selectedFiles.value.reduce((sum, file) => sum + file.size, 0))
const ocrRouting = reactive<UploadOcrRoutingOptions>({
  ocrRoutingMode: 'DEFAULT',
  ocrModelKey: '',
  ocrNodeId: '',
  ocrLoadBalanceStrategy: ''
})

function openFilePicker(): void {
  if (fileInput.value) {
    fileInput.value.click()
  } else {
    // 文件输入框尚未挂载时不执行操作。
  }
}

function updateFiles(files: FileList | null): void {
  if (files) {
    selectedFiles.value = Array.from(files)
  } else {
    selectedFiles.value = []
  }
}

function removeFile(fileName: string): void {
  selectedFiles.value = selectedFiles.value.filter((file) => file.name !== fileName)
}

function handleFileChange(event: Event): void {
  const target = event.target as HTMLInputElement
  updateFiles(target.files)
}

function handleDrop(event: DragEvent): void {
  event.preventDefault()
  updateFiles(event.dataTransfer?.files ?? null)
}

function resetForm(): void {
  selectedFiles.value = []
  form.metadata = EMPTY_UPLOAD_OPTIONS.metadata
  form.callbackUrl = EMPTY_UPLOAD_OPTIONS.callbackUrl
  form.idempotencyKey = EMPTY_UPLOAD_OPTIONS.idempotencyKey
  form.adapterOverride = EMPTY_UPLOAD_OPTIONS.adapterOverride
  form.pdfMode = EMPTY_UPLOAD_OPTIONS.pdfMode
  ocrRoutingSelector.value?.reset()
  if (fileInput.value) {
    fileInput.value.value = ''
  } else {
    // 文件输入框尚未挂载，无需重置 DOM 值。
  }
}

function updateOcrRouting(value: UploadOcrRoutingOptions): void {
  ocrRouting.ocrRoutingMode = value.ocrRoutingMode
  ocrRouting.ocrModelKey = value.ocrModelKey
  ocrRouting.ocrNodeId = value.ocrNodeId
  ocrRouting.ocrLoadBalanceStrategy = value.ocrLoadBalanceStrategy
}

function submitUpload(): void {
  const validationMessage = ocrRoutingSelector.value?.validateRouting() ?? ''
  if (validationMessage) {
    // OCR 路由字段不完整时保持表单不提交，由选择器展示校验信息。
  } else {
    emit('submit', {
      files: selectedFiles.value,
      metadata: form.metadata,
      callbackUrl: form.callbackUrl,
      idempotencyKey: form.idempotencyKey,
      adapterOverride: form.adapterOverride,
      pdfMode: form.pdfMode,
      ocrRoutingMode: ocrRouting.ocrRoutingMode,
      ocrModelKey: ocrRouting.ocrModelKey,
      ocrNodeId: ocrRouting.ocrNodeId,
      ocrLoadBalanceStrategy: ocrRouting.ocrLoadBalanceStrategy
    })
  }
}

defineExpose({ resetForm })
</script>

<template>
  <section class="upload-dropzone">
    <div class="upload-dropzone__target" @drop="handleDrop" @dragover.prevent>
      <input
        ref="fileInput"
        class="upload-dropzone__input"
        type="file"
        multiple
        :accept="ACCEPTED_FILE_TYPES"
        @change="handleFileChange"
      >
      <NIcon class="upload-dropzone__target-icon" :component="UploadCloud" />
      <div class="upload-dropzone__target-copy">
        <strong>选择或拖入文件</strong>
        <span>PDF、Word、Markdown、图片、TXT</span>
      </div>
      <NButton size="small" @click="openFilePicker">
        选择文件
      </NButton>
    </div>

    <div class="upload-dropzone__files">
      <div class="upload-dropzone__files-header">
        <span>待上传文件</span>
        <NTag round>
          {{ formatNumber(selectedFiles.length) }} 个 · {{ formatNumber(Math.round(totalSize / 1024)) }} KB
        </NTag>
      </div>
      <div v-if="hasFiles" class="upload-dropzone__file-list">
        <article v-for="file in selectedFiles" :key="`${file.name}-${file.size}`" class="upload-file">
          <NIcon class="upload-file__icon" :component="FilePlus2" />
          <div class="upload-file__body">
            <strong>{{ file.name }}</strong>
            <span>{{ formatNumber(Math.round(file.size / 1024)) }} KB</span>
          </div>
          <NButton quaternary circle size="small" aria-label="移除文件" @click="removeFile(file.name)">
            <template #icon>
              <NIcon :component="X" />
            </template>
          </NButton>
        </article>
      </div>
      <p v-else class="upload-dropzone__empty">还没有选择文件</p>
    </div>

    <UploadAdvancedOptions v-model="form" />

    <OcrRoutingSelector ref="ocrRoutingSelector" @change="updateOcrRouting" />

    <div class="upload-dropzone__actions">
      <NButton quaternary :disabled="loading" @click="resetForm">
        清空
      </NButton>
      <NButton type="primary" :loading="loading" :disabled="!hasFiles" @click="submitUpload">
        上传并解析
      </NButton>
    </div>
  </section>
</template>

<style scoped>
.upload-dropzone {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 16px;
}

.upload-dropzone__target {
  display: grid;
  min-height: 164px;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  padding: 22px;
  border: 1px dashed rgba(37, 109, 133, 0.48);
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(220, 239, 245, 0.88), rgba(255, 255, 255, 0.82)),
    var(--surface-inset);
  transition: border-color 180ms ease, box-shadow 180ms ease, background-color 180ms ease;
}

.upload-dropzone__target:hover {
  border-color: var(--active);
  box-shadow: var(--focus-ring);
}

.upload-dropzone__input {
  display: none;
}

.upload-dropzone__target-icon {
  display: grid;
  width: 48px;
  height: 48px;
  place-items: center;
  border-radius: 8px;
  background: #ffffff;
  color: var(--active);
  font-size: 32px;
  box-shadow: var(--shadow-soft);
}

.upload-dropzone__target-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.upload-dropzone__target-copy strong {
  color: var(--ink-strong);
  font-size: 18px;
}

.upload-dropzone__target-copy span,
.upload-dropzone__empty {
  color: var(--ink-muted);
  font-size: 13px;
}

.upload-dropzone__files {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.upload-dropzone__files-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--ink-soft);
  font-size: 13px;
  font-weight: 650;
}

.upload-dropzone__file-list {
  display: flex;
  max-height: 280px;
  flex-direction: column;
  gap: 8px;
  overflow: auto;
}

.upload-file {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border: 1px solid var(--rail-border);
  border-radius: 8px;
  background: var(--surface-raised);
  transition: border-color 180ms ease, background-color 180ms ease;
}

.upload-file:hover {
  border-color: var(--rail-border-strong);
  background: var(--surface-hover);
}

.upload-file__icon {
  color: var(--active);
  font-size: 19px;
}

.upload-file__body {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.upload-file__body strong,
.upload-file__body span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.upload-file__body strong {
  color: var(--ink-strong);
  font-size: 13px;
}

.upload-file__body span {
  color: var(--ink-muted);
  font-size: 12px;
}

.upload-dropzone__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 760px) {
  .upload-dropzone__target {
    grid-template-columns: 1fr;
  }
}
</style>
