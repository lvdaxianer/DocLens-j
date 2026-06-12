<script setup lang="ts">
import { computed } from 'vue'
import {
  NAlert,
  NButton,
  NDescriptions,
  NDescriptionsItem,
  NDrawer,
  NDrawerContent,
  NSpin,
  NTabPane,
  NTabs,
  NTag,
  useMessage
} from 'naive-ui'

import type { DocumentResultResponse, DocumentRow } from '@/types/dashboard'
import { formatNumber, formatPercent } from '@/utils/formatters'
import {
  extractOcrOriginalText,
  isLlmMarkdownApplied,
  llmStageDescription,
  llmPostProcessingStatus,
  resultTextTitle
} from '@/utils/llmResultDisplayRules'

const RESULT_DRAWER_WIDTH = 720
const TEXT_PREVIEW_MAX_HEIGHT = '52vh'
const PRIMARY_TEXT_TAB = 'primary-text'
const OCR_ORIGINAL_TAB = 'ocr-original'

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

const message = useMessage()
const activeTab = defineModel<string>('activeTab', { default: PRIMARY_TEXT_TAB })
const textTitle = computed(() => props.result ? resultTextTitle(props.result.result) : '解析内容')
const markdownText = computed(() => props.result?.result.finalText ?? '')
const ocrOriginalText = computed(() => props.result ? extractOcrOriginalText(props.result.result) : '')
const textLength = computed(() => markdownText.value.length)
const ocrTextLength = computed(() => ocrOriginalText.value.length)
const llmStatus = computed(() => props.result ? llmPostProcessingStatus(props.result.result) : 'LLM 未生效')
const llmApplied = computed(() => props.result ? isLlmMarkdownApplied(props.result.result) : false)
const llmDescription = computed(() => props.result ? llmStageDescription(props.result.result) : '未配置 LLM 后处理，返回 OCR 纯文本')
const llmErrorMessage = computed(() => {
  const message = props.result?.result.llm_error_message?.trim() ?? ''
  return message
})

/**
 * 判断当前是否展示主结果文本。
 *
 * @returns 是否展示主结果文本
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
function isPrimaryTextTab(): boolean {
  return activeTab.value === PRIMARY_TEXT_TAB
}

/**
 * 判断当前是否展示 OCR 原内容。
 *
 * @returns 是否展示 OCR 原内容
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
function isOcrOriginalTab(): boolean {
  return activeTab.value === OCR_ORIGINAL_TAB
}

/**
 * 复制指定解析内容到剪贴板。
 *
 * @param content 待复制内容
 * @param label 内容标签
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
async function copyContent(content: string, label: string): Promise<void> {
  // 空内容不进入剪贴板，直接提示用户。
  if (!content.trim()) {
    message.warning(`${label}暂无可复制内容`)
    return
  }
  try {
    await navigator.clipboard.writeText(content)
    message.success(`${label}已复制`)
  } catch {
    message.error(`${label}复制失败`)
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

            <NTabs v-model:value="activeTab" class="document-result__tabs" type="line" animated>
              <NTabPane :name="PRIMARY_TEXT_TAB" :tab="textTitle" display-directive="if">
                <section v-if="isPrimaryTextTab()" class="document-result__text">
                  <div class="document-result__text-header">
                    <span>{{ textTitle }}</span>
                    <div class="document-result__text-actions">
                      <NTag round>{{ formatNumber(textLength) }} 字符</NTag>
                      <NButton size="tiny" secondary :disabled="!markdownText" @click="copyContent(markdownText, textTitle)">
                        复制
                      </NButton>
                    </div>
                  </div>
                  <pre>{{ markdownText || '暂无文本内容' }}</pre>
                </section>
              </NTabPane>
              <NTabPane :name="OCR_ORIGINAL_TAB" tab="OCR 原内容" display-directive="if">
                <section v-if="isOcrOriginalTab()" class="document-result__text">
                  <div class="document-result__text-header">
                    <span>OCR 原内容</span>
                    <div class="document-result__text-actions">
                      <NTag round>{{ formatNumber(ocrTextLength) }} 字符</NTag>
                      <NButton size="tiny" secondary :disabled="!ocrOriginalText" @click="copyContent(ocrOriginalText, 'OCR 原内容')">
                        复制
                      </NButton>
                    </div>
                  </div>
                  <pre>{{ ocrOriginalText || '暂无 OCR 原内容' }}</pre>
                </section>
              </NTabPane>
            </NTabs>
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
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  gap: 8px;
}

.document-result__text pre {
  max-height: var(--document-result-text-max-height);
  margin: 0;
  padding: 14px 16px;
  border: 1px solid var(--rail-border);
  border-radius: 8px;
  overflow: auto;
  background: var(--surface-inset);
  color: var(--ink-strong);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
