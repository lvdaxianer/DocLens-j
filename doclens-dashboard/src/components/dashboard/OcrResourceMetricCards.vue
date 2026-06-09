<script setup lang="ts">
import { computed } from 'vue'
import { Activity, CheckCircle2, Clock3, Image, ServerCrash } from '@lucide/vue'
import { NIcon } from 'naive-ui'

import type { DashboardOcrResources } from '@/types/dashboard'
import { formatNumber } from '@/utils/formatters'
import { displayNodeName } from '@/utils/ocrDisplayRules'

const props = defineProps<{
  metrics?: DashboardOcrResources | null
}>()

const cards = computed(() => [
  {
    key: 'healthy',
    label: 'OCR 健康节点',
    value: formatNumber(props.metrics?.healthy_node_count),
    note: `${formatNumber(props.metrics?.recovering_node_count)} 个恢复中`,
    icon: CheckCircle2,
    tone: 'success'
  },
  {
    key: 'down',
    label: 'OCR 异常节点',
    value: formatNumber(props.metrics?.down_node_count),
    note: '健康检查摘除',
    icon: ServerCrash,
    tone: 'danger'
  },
  {
    key: 'inflight',
    label: 'OCR 解析中图片',
    value: formatNumber(props.metrics?.global_inflight_images),
    note: busiestNodeNote.value,
    icon: Image,
    tone: 'active'
  },
  {
    key: 'request-queue',
    label: 'OCR 请求队列',
    value: formatNumber(props.metrics?.thread_pools.ocr_request?.queue_size),
    note: `${formatNumber(props.metrics?.thread_pools.ocr_request?.active_count)} 个活跃请求`,
    icon: Activity,
    tone: 'latency'
  },
  {
    key: 'health-queue',
    label: '健康检查队列',
    value: formatNumber(props.metrics?.thread_pools.ocr_health?.queue_size),
    note: `${formatNumber(props.metrics?.thread_pools.ocr_health?.active_count)} 个活跃检查`,
    icon: Clock3,
    tone: 'neutral'
  }
])

const busiestNodeNote = computed(() => {
  const busiestNode = props.metrics?.busiest_node
  if (busiestNode?.node_id) {
    // 有繁忙节点时优先展示节点名称，内部 ID 仅作为缺省值。
    return `${displayNodeName({ nodeId: busiestNode.node_id, nodeName: busiestNode.node_name })} · ${formatNumber(busiestNode.inflight_images)} 张`
  } else {
    // 没有繁忙节点时展示稳定空态。
    return '暂无繁忙节点'
  }
})
</script>

<template>
  <section class="ocr-resource-metrics" aria-label="OCR resource metrics">
    <article v-for="card in cards" :key="card.key" class="ocr-resource-card" :class="`ocr-resource-card--${card.tone}`">
      <span class="ocr-resource-card__icon">
        <NIcon :component="card.icon" />
      </span>
      <span class="ocr-resource-card__label">{{ card.label }}</span>
      <strong class="ocr-resource-card__value">{{ card.value }}</strong>
      <span class="ocr-resource-card__note">{{ card.note }}</span>
    </article>
  </section>
</template>

<style scoped>
.ocr-resource-metrics {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.ocr-resource-card {
  display: grid;
  min-width: 0;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 4px 10px;
  padding: 14px;
  border: 1px solid var(--rail-border);
  border-radius: 8px;
  background: var(--surface-raised);
  box-shadow: var(--shadow-card);
}

.ocr-resource-card__icon {
  display: grid;
  width: 34px;
  height: 34px;
  grid-row: span 3;
  place-items: center;
  border-radius: 7px;
  background: var(--signal-muted);
  color: var(--signal);
  font-size: 20px;
}

.ocr-resource-card__label,
.ocr-resource-card__note {
  overflow-wrap: anywhere;
  color: var(--ink-muted);
  font-size: 12px;
}

.ocr-resource-card__value {
  overflow-wrap: anywhere;
  color: var(--ink-strong);
  font-size: 21px;
}

.ocr-resource-card--success {
  --signal: var(--success);
  --signal-muted: var(--success-muted);
}

.ocr-resource-card--danger {
  --signal: var(--danger);
  --signal-muted: var(--danger-muted);
}

.ocr-resource-card--active {
  --signal: var(--active);
  --signal-muted: var(--active-muted);
}

.ocr-resource-card--latency {
  --signal: var(--latency);
  --signal-muted: var(--latency-muted);
}

.ocr-resource-card--neutral {
  --signal: var(--ink-soft);
  --signal-muted: var(--surface-inset);
}

@media (max-width: 1440px) {
  .ocr-resource-metrics {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .ocr-resource-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
