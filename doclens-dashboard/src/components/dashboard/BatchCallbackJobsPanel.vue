<script setup lang="ts">
import { computed } from 'vue'
import { NTag } from 'naive-ui'

import type { CallbackJobRow } from '@/types/dashboard'
import { formatDateTime } from '@/utils/formatters'

type CallbackStatusTagType = 'success' | 'error' | 'warning' | 'default'

const EMPTY_DISPLAY_VALUE = '-'
const CALLBACK_STATUS_LABELS: Record<string, string> = {
  success: '成功',
  failed: '失败',
  retrying: '重试中',
  pending: '待投递'
}
const CALLBACK_STATUS_TAG_TYPES: Record<string, CallbackStatusTagType> = {
  success: 'success',
  failed: 'error',
  retrying: 'warning',
  pending: 'default'
}

const props = defineProps<{
  callbackJobs: CallbackJobRow[]
}>()

const totalJobs = computed(() => props.callbackJobs.length)

/**
 * 转换回调状态文案。
 *
 * @param status 回调状态编码
 * @returns 回调状态中文文案
 * @author lvdaxianerplus
 * @date 2026-06-16
 */
function callbackStatusLabel(status: string): string {
  const normalized = status.toLowerCase()
  const label = CALLBACK_STATUS_LABELS[normalized]
  if (label) {
    // 已知状态使用稳定中文文案，避免原始编码影响扫描效率。
    return label
  } else {
    // 未识别状态保留原始编码，方便排查新状态。
    return status || EMPTY_DISPLAY_VALUE
  }
}

/**
 * 转换回调状态标签类型。
 *
 * @param status 回调状态编码
 * @returns Naive UI 标签类型
 * @author lvdaxianerplus
 * @date 2026-06-16
 */
function callbackStatusType(status: string): CallbackStatusTagType {
  const normalized = status.toLowerCase()
  const tagType = CALLBACK_STATUS_TAG_TYPES[normalized]
  if (tagType) {
    // 已知状态使用对应标签色，帮助快速区分成功、失败和重试中。
    return tagType
  } else {
    // 未知状态使用默认标签，避免误报失败。
    return 'default'
  }
}

/**
 * 格式化回调字段空值。
 *
 * @param value 原始字段值
 * @returns 可展示字段值
 * @author lvdaxianerplus
 * @date 2026-06-16
 */
function displayValue(value: string | undefined): string {
  return value && value.trim().length > 0 ? value : EMPTY_DISPLAY_VALUE
}
</script>

<template>
  <section class="panel callback-panel">
    <div class="panel__header">
      <div>
        <h2 class="panel__title">回调投递结果</h2>
        <span class="panel__hint">查看 callback_url 的投递状态、重试次数和失败原因</span>
      </div>
      <NTag round>
        {{ totalJobs }} 条
      </NTag>
    </div>

    <div v-if="callbackJobs.length > 0" class="callback-panel__list">
      <article
        v-for="callbackJob in callbackJobs"
        :key="callbackJob.callback_job_id"
        class="callback-panel__item"
      >
        <div class="callback-panel__item-header">
          <div class="callback-panel__identity">
            <strong>{{ callbackJob.callback_url }}</strong>
            <span>{{ callbackJob.document_id || callbackJob.callback_job_id }}</span>
          </div>
          <NTag :type="callbackStatusType(callbackJob.status)" round>
            {{ callbackStatusLabel(callbackJob.status) }}
          </NTag>
        </div>

        <dl class="callback-panel__facts">
          <div>
            <dt>重试次数</dt>
            <dd>{{ callbackJob.retry_count }}</dd>
          </div>
          <div>
            <dt>下次重试</dt>
            <dd>{{ displayValue(formatDateTime(callbackJob.next_retry_at)) }}</dd>
          </div>
          <div>
            <dt>失败原因</dt>
            <dd>{{ displayValue(callbackJob.failure_reason) }}</dd>
          </div>
          <div>
            <dt>失败详情</dt>
            <dd>{{ displayValue(callbackJob.failure_detail) }}</dd>
          </div>
        </dl>
      </article>
    </div>

    <p v-else class="callback-panel__empty">当前批次没有回调任务</p>
  </section>
</template>

<style scoped>
.callback-panel {
  display: grid;
  gap: 10px;
}

.callback-panel__list {
  display: grid;
  gap: 8px;
}

.callback-panel__item {
  display: grid;
  gap: 10px;
  padding: 10px;
  border: 1px solid var(--rail-border);
  border-radius: 8px;
  background: var(--surface-inset);
}

.callback-panel__item-header {
  display: flex;
  min-width: 0;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.callback-panel__identity {
  display: grid;
  min-width: 0;
  gap: 3px;
}

.callback-panel__identity strong,
.callback-panel__facts dd {
  margin: 0;
  overflow-wrap: anywhere;
  color: var(--ink-strong);
  font-weight: 700;
}

.callback-panel__identity span,
.callback-panel__facts dt,
.callback-panel__empty {
  color: var(--ink-muted);
  font-size: 12px;
}

.callback-panel__facts {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  margin: 0;
}

.callback-panel__facts div {
  display: grid;
  min-width: 0;
  gap: 3px;
}

.callback-panel__empty {
  margin: 0;
  padding: 10px;
  border: 1px dashed var(--rail-border);
  border-radius: 8px;
  background: var(--surface-inset);
}

@media (max-width: 900px) {
  .callback-panel__item-header,
  .callback-panel__facts {
    grid-template-columns: 1fr;
  }

  .callback-panel__item-header {
    display: grid;
  }
}
</style>
