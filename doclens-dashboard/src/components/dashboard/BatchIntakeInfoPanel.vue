<script setup lang="ts">
import { computed } from 'vue'

import type { BatchRow } from '@/types/dashboard'

const EMPTY_CALLBACK_URL_LABEL = '未提供回调地址'
const EMPTY_IDEMPOTENCY_KEY_LABEL = '未提供幂等值'
const EMPTY_TENANT_KEY_LABEL = '未提供租户键'
const EMPTY_METADATA_LABEL = '无 meta 信息'
const JSON_INDENT_SIZE = 2

const props = defineProps<{
  /** 后端返回的批次聚合数据。 */
  batch: BatchRow
}>()

const metadataText = computed(() => formattedMetadata(props.batch.metadata))

/**
 * 格式化可展示字段值。
 *
 * @param value 原始字段值
 * @param emptyLabel 空值占位
 * @returns 可展示字段值
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
function displayText(value: string | undefined, emptyLabel: string): string {
  const normalizedValue = value?.trim() ?? ''
  if (normalizedValue) {
    // 有真实值时原样展示，方便复制排查第三方请求。
    return normalizedValue
  } else {
    // 空值使用稳定文案，避免用户误以为页面漏渲染。
    return emptyLabel
  }
}

/**
 * 格式化 metadata JSON。
 *
 * @param metadata 批次 metadata
 * @returns 可阅读 JSON 或空态文案
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
function formattedMetadata(metadata: Record<string, unknown> | undefined): string {
  if (metadata && Object.keys(metadata).length > 0) {
    // metadata 使用缩进 JSON，保留原始键名便于和第三方请求比对。
    return JSON.stringify(metadata, null, JSON_INDENT_SIZE)
  } else {
    // 空对象保持可见空态，不隐藏整个 meta 区域。
    return EMPTY_METADATA_LABEL
  }
}
</script>

<template>
  <section class="panel batch-intake">
    <div class="panel__header">
      <div>
        <h2 class="panel__title">接入信息</h2>
        <span class="panel__hint">创建批次时解析出的接入方、回调地址、幂等值和 meta 信息</span>
      </div>
    </div>

    <dl class="batch-intake__facts">
      <div>
        <dt>接入方 ID</dt>
        <dd>{{ displayText(batch.client_id, 'anonymous') }}</dd>
      </div>
      <div>
        <dt>来源应用</dt>
        <dd>{{ displayText(batch.source_app, 'unknown') }}</dd>
      </div>
      <div>
        <dt>租户键</dt>
        <dd>{{ displayText(batch.tenant_key, EMPTY_TENANT_KEY_LABEL) }}</dd>
      </div>
      <div>
        <dt>回调地址</dt>
        <dd>{{ displayText(batch.callback_url, EMPTY_CALLBACK_URL_LABEL) }}</dd>
      </div>
      <div>
        <dt>幂等值</dt>
        <dd>{{ displayText(batch.idempotency_key, EMPTY_IDEMPOTENCY_KEY_LABEL) }}</dd>
      </div>
    </dl>

    <section class="batch-intake__metadata">
      <h3>Meta 信息</h3>
      <pre>{{ metadataText }}</pre>
    </section>
  </section>
</template>

<style scoped>
.batch-intake {
  display: grid;
  gap: 10px;
}

.batch-intake__facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin: 0;
}

.batch-intake__facts div,
.batch-intake__metadata {
  min-width: 0;
  padding: 10px;
  border: 1px solid var(--rail-border);
  border-radius: 8px;
  background: var(--surface-inset);
}

.batch-intake__facts dt,
.batch-intake__metadata h3 {
  margin: 0;
  color: var(--ink-muted);
  font-size: 12px;
}

.batch-intake__facts dd {
  margin: 3px 0 0;
  overflow-wrap: anywhere;
  color: var(--ink-strong);
  font-weight: 700;
}

.batch-intake__metadata {
  display: grid;
  gap: 6px;
}

.batch-intake__metadata pre {
  max-height: 180px;
  margin: 0;
  overflow: auto;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  color: var(--ink-strong);
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12px;
  line-height: 1.5;
}

@media (max-width: 900px) {
  .batch-intake__facts {
    grid-template-columns: 1fr;
  }
}
</style>
