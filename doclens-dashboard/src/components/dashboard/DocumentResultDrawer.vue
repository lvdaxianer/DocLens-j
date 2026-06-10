<script setup lang="ts">
import { computed, shallowRef } from 'vue'
import {
  NAlert,
  NButton,
  NDescriptions,
  NDescriptionsItem,
  NDrawer,
  NDrawerContent,
  NSpin,
  NTag
} from 'naive-ui'

import type { DocumentResultResponse, DocumentRow } from '@/types/dashboard'
import { formatNumber, formatPercent } from '@/utils/formatters'
import {
  isLlmMarkdownApplied,
  llmStageDescription,
  llmPostProcessingStatus,
  resultTextTitle
} from '@/utils/llmResultDisplayRules'
import { renderMarkdownPreviewHtml } from '@/utils/markdownPreviewRules'

const RESULT_DRAWER_WIDTH = 720
const TEXT_PREVIEW_MAX_HEIGHT = '52vh'
const COPY_MARKDOWN_IDLE_LABEL = '复制 Markdown'
const COPY_MARKDOWN_DONE_LABEL = '已复制'
const COPY_MARKDOWN_FAILED_LABEL = '复制失败'

/**
 * 文档解析结果抽屉，展示上传文件名、纯文本结果和落盘路径。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
const isOpen = defineModel<boolean>('show', { required: true })

const props = defineProps<{
  document: DocumentRow | null
  result: DocumentResultResponse | null
  loading: boolean
  error: string
}>()

const emit = defineEmits<{
  retry: []
}>()

const copyButtonLabel = shallowRef(COPY_MARKDOWN_IDLE_LABEL)
const textTitle = computed(() => props.result ? resultTextTitle(props.result.result) : '解析内容')
const textLength = computed(() => props.result?.result.finalText.length ?? 0)
const markdownPreviewHtml = computed(() => renderMarkdownPreviewHtml(props.result?.result.finalText ?? ''))
const llmStatus = computed(() => props.result ? llmPostProcessingStatus(props.result.result) : 'LLM 未生效')
const llmApplied = computed(() => props.result ? isLlmMarkdownApplied(props.result.result) : false)
const llmDescription = computed(() => props.result ? llmStageDescription(props.result.result) : '未配置 LLM 后处理，返回 OCR 纯文本')
const llmErrorMessage = computed(() => {
  const message = props.result?.result.llm_error_message?.trim() ?? ''
  return message
})

/**
 * 复制当前 Markdown 解析内容。
 *
 * @returns 复制完成信号
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
async function copyMarkdown(): Promise<void> {
  try {
    await navigator.clipboard.writeText(props.result?.result.finalText ?? '')
    copyButtonLabel.value = COPY_MARKDOWN_DONE_LABEL
  } catch {
    // 浏览器拒绝剪贴板权限时只更新按钮状态，不影响用户继续查看内容。
    copyButtonLabel.value = COPY_MARKDOWN_FAILED_LABEL
  }
}
</script>

<template>
  <NDrawer v-model:show="isOpen" :width="RESULT_DRAWER_WIDTH" placement="right">
    <NDrawerContent title="解析内容" closable>
      <div class="document-result" :style="{ '--document-result-text-max-height': TEXT_PREVIEW_MAX_HEIGHT }">
        <section class="document-result__header">
          <div class="document-result__identity">
            <span>上传文件</span>
            <strong>{{ document?.file_name ?? '-' }}</strong>
          </div>
          <NTag v-if="document" round>
            {{ document.file_type.toUpperCase() }}
          </NTag>
        </section>

        <NAlert v-if="error" type="error" :title="error">
          <NButton size="small" secondary @click="emit('retry')">
            重新加载
          </NButton>
        </NAlert>

        <NSpin :show="loading">
          <template v-if="result">
            <NDescriptions label-placement="left" bordered :column="1" size="small">
              <NDescriptionsItem label="结果 ID">
                {{ result.result_id }}
              </NDescriptionsItem>
              <NDescriptionsItem label="保存路径">
                {{ result.result.markdownStorageUri || '-' }}
              </NDescriptionsItem>
              <NDescriptionsItem label="页数">
                {{ formatNumber(result.result.summary.pageCount) }}
              </NDescriptionsItem>
              <NDescriptionsItem label="文本块">
                {{ formatNumber(result.result.summary.blockCount) }}
              </NDescriptionsItem>
              <NDescriptionsItem label="置信度">
                {{ formatPercent(result.result.confidence * 100) }}
              </NDescriptionsItem>
              <NDescriptionsItem label="LLM 排版">
                <NTag :type="llmApplied ? 'success' : 'warning'" round>
                  {{ llmStatus }}
                </NTag>
              </NDescriptionsItem>
              <NDescriptionsItem label="LLM 结果说明">
                {{ llmDescription }}
              </NDescriptionsItem>
              <NDescriptionsItem v-if="!llmApplied && llmErrorMessage" label="LLM 失败原因">
                {{ llmErrorMessage }}
              </NDescriptionsItem>
            </NDescriptions>

            <section class="document-result__text">
              <div class="document-result__text-header">
                <span>{{ textTitle }}</span>
                <div class="document-result__text-actions">
                  <NTag round>{{ formatNumber(textLength) }} 字符</NTag>
                  <NButton size="tiny" secondary :disabled="!textLength" @click="copyMarkdown">
                    {{ copyButtonLabel }}
                  </NButton>
                </div>
              </div>
              <article class="document-result__markdown" v-html="markdownPreviewHtml" />
            </section>
          </template>

          <NAlert v-else-if="!loading && !error" type="info" title="暂无解析内容">
            文档解析完成后会在这里显示最终内容。
          </NAlert>
        </NSpin>
      </div>
    </NDrawerContent>
  </NDrawer>
</template>

<style scoped>
.document-result {
  display: grid;
  gap: 16px;
}

.document-result__header {
  display: flex;
  min-width: 0;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.document-result__identity {
  display: grid;
  min-width: 0;
  gap: 4px;
}

.document-result__identity span,
.document-result__text-header {
  color: var(--ink-muted);
  font-size: 12px;
  font-weight: 700;
}

.document-result__identity strong {
  overflow-wrap: anywhere;
  color: var(--ink-strong);
  font-size: 16px;
}

.document-result__text {
  display: grid;
  gap: 10px;
}

.document-result__text-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.document-result__text-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.document-result__markdown {
  max-height: var(--document-result-text-max-height);
  margin: 0;
  padding: 14px 16px;
  border: 1px solid var(--rail-border);
  border-radius: 8px;
  overflow: auto;
  background: var(--surface-inset);
  color: var(--ink-strong);
  font-size: 13px;
  line-height: 1.65;
  word-break: break-word;
}

.document-result__markdown :deep(h1),
.document-result__markdown :deep(h2),
.document-result__markdown :deep(h3),
.document-result__markdown :deep(h4),
.document-result__markdown :deep(h5),
.document-result__markdown :deep(h6) {
  margin: 0 0 10px;
  color: var(--ink-strong);
  font-weight: 800;
  line-height: 1.35;
}

.document-result__markdown :deep(p) {
  margin: 0 0 12px;
}

.document-result__markdown :deep(a) {
  color: var(--active);
  font-weight: 700;
}

.document-result__markdown :deep(strong) {
  color: var(--ink-strong);
}
</style>
