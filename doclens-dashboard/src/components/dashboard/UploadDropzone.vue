<script setup lang="ts">
import { computed, reactive, shallowRef, useTemplateRef } from 'vue'
import { FilePlus2, UploadCloud, X } from '@lucide/vue'
import {
  NButton,
  NIcon,
  NInput,
  NSelect,
  NTag
} from 'naive-ui'

import type { UploadBatchOptions } from '@/types/upload'
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
const selectedFiles = shallowRef<File[]>([])
const form = reactive({
  metadata: EMPTY_UPLOAD_OPTIONS.metadata,
  callbackUrl: EMPTY_UPLOAD_OPTIONS.callbackUrl,
  idempotencyKey: EMPTY_UPLOAD_OPTIONS.idempotencyKey,
  adapterOverride: EMPTY_UPLOAD_OPTIONS.adapterOverride,
  pdfMode: EMPTY_UPLOAD_OPTIONS.pdfMode
})

const pdfModeOptions = [
  { label: '服务端默认', value: '' },
  { label: '页图 OCR', value: 'page_image_fallback' },
  { label: '直接解析', value: 'direct' }
]

const hasFiles = computed(() => selectedFiles.value.length > 0)
const totalSize = computed(() => selectedFiles.value.reduce((sum, file) => sum + file.size, 0))

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
  if (fileInput.value) {
    fileInput.value.value = ''
  } else {
    // 文件输入框尚未挂载，无需重置 DOM 值。
  }
}

function submitUpload(): void {
  emit('submit', {
    files: selectedFiles.value,
    metadata: form.metadata,
    callbackUrl: form.callbackUrl,
    idempotencyKey: form.idempotencyKey,
    adapterOverride: form.adapterOverride,
    pdfMode: form.pdfMode,
    ocrRoutingMode: EMPTY_UPLOAD_OPTIONS.ocrRoutingMode,
    ocrModelKey: EMPTY_UPLOAD_OPTIONS.ocrModelKey,
    ocrNodeId: EMPTY_UPLOAD_OPTIONS.ocrNodeId,
    ocrLoadBalanceStrategy: EMPTY_UPLOAD_OPTIONS.ocrLoadBalanceStrategy
  })
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

      <div class="upload-dropzone__options">
        <label class="upload-dropzone__label" for="upload-metadata">元数据 JSON</label>
      <NInput
        id="upload-metadata"
        v-model:value="form.metadata"
        type="textarea"
        :autosize="{ minRows: 3, maxRows: 5 }"
        placeholder="metadata JSON，例如 {}"
      />
      <div class="upload-dropzone__option-grid">
        <label class="upload-dropzone__field">
          <span>回调地址</span>
          <NInput v-model:value="form.callbackUrl" placeholder="callback_url，可选" />
        </label>
        <label class="upload-dropzone__field">
          <span>幂等键</span>
          <NInput v-model:value="form.idempotencyKey" placeholder="idempotency_key，可选" />
        </label>
        <label class="upload-dropzone__field">
          <span>OCR 适配器</span>
          <NInput v-model:value="form.adapterOverride" placeholder="adapter_override，可选" />
        </label>
        <label class="upload-dropzone__field">
          <span>PDF 模式</span>
          <NSelect v-model:value="form.pdfMode" :options="pdfModeOptions" />
        </label>
      </div>
    </div>

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

.upload-dropzone__options {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.upload-dropzone__label,
.upload-dropzone__field span {
  color: var(--ink-soft);
  font-size: 12px;
  font-weight: 700;
}

.upload-dropzone__field {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 6px;
}

.upload-dropzone__option-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.upload-dropzone__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 760px) {
  .upload-dropzone__target,
  .upload-dropzone__option-grid {
    grid-template-columns: 1fr;
  }
}
</style>
