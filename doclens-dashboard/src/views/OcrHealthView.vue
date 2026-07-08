<script setup lang="ts">
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { Cpu, Gauge, Timer, TriangleAlert } from '@lucide/vue'
import { NAlert, NButton, NIcon } from 'naive-ui'

import AnimatedNumber from '@/components/dashboard/AnimatedNumber.vue'
import AutoRefreshStaleAlert from '@/components/dashboard/AutoRefreshStaleAlert.vue'
import FailureList from '@/components/dashboard/FailureList.vue'
import { DEFAULT_REFRESH_INTERVAL_SECONDS, useAutoRefresh } from '@/composables/useAutoRefresh'
import { useDashboardStore } from '@/stores/dashboard'
import { formatDateTime, formatDuration, formatPercent } from '@/utils/formatters'

const store = useDashboardStore()
const { ocrHealth, ocrHealthState } = storeToRefs(store)
const unavailableNodes = computed(() => (ocrHealth.value?.ocr_resources.nodes ?? [])
  .filter((node) => node.status === 'DOWN' || node.status === 'RECOVERING'))

const cards = [
  { key: 'adapter', label: '适配器', icon: Cpu },
  { key: 'success', label: '成功率', icon: Gauge },
  { key: 'failure', label: '失败率', icon: TriangleAlert },
  { key: 'duration', label: '平均请求耗时', icon: Timer }
]

/**
 * 获取健康卡片展示值。
 *
 * @param key - 卡片键
 * @returns 卡片展示文本
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
function cardValue(key: string): string {
  if (key === 'adapter') {
    return ocrHealth.value?.adapter_key ?? 'paddle_ocr'
  } else if (key === 'success') {
    return formatPercent(ocrHealth.value?.success_rate)
  } else if (key === 'failure') {
    return formatPercent(ocrHealth.value?.failure_rate)
  } else {
    return formatDuration(ocrHealth.value?.average_duration_ms)
  }
}

/**
 * 刷新 OCR 健康摘要。
 *
 * @returns 刷新完成信号
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
function refresh(): Promise<void> {
  return store.loadOcrHealth()
}

const autoRefresh = useAutoRefresh(refresh, {
  failureMessage: () => ocrHealthState.value.error
})
</script>

<template>
  <div class="view-stack">
    <NAlert v-if="ocrHealthState.error" type="error" :title="ocrHealthState.error" />
    <AutoRefreshStaleAlert
      :show="autoRefresh.isStale.value"
      subject="OCR 健康数据"
      :error-message="autoRefresh.lastErrorMessage.value"
    />
    <NAlert
      v-if="unavailableNodes.length > 0"
      type="warning"
      title="存在不可用或恢复中的 OCR 节点"
    >
      <span v-for="node in unavailableNodes" :key="node.node_id" class="ocr-health-node">
        {{ node.node_name || node.node_id }}：{{ node.status }}
        <template v-if="node.last_error">（{{ node.last_error }}）</template>
      </span>
    </NAlert>

    <section class="health-toolbar">
      <span>最后刷新：{{ formatDateTime(ocrHealthState.lastUpdated) }}</span>
      <span>每 {{ DEFAULT_REFRESH_INTERVAL_SECONDS }} 秒自动刷新</span>
      <NButton size="small" :loading="ocrHealthState.loading" @click="autoRefresh.refreshNow">
        刷新
      </NButton>
    </section>

    <section class="health-grid">
      <article v-for="card in cards" :key="card.key" class="health-card">
        <NIcon class="health-card__icon" :component="card.icon" />
        <span class="health-card__label">{{ card.label }}</span>
        <strong class="health-card__value"><AnimatedNumber :value="cardValue(card.key)" /></strong>
      </article>
    </section>

    <FailureList :failures="ocrHealth?.recent_failures ?? []" />
  </div>
</template>

<style scoped>
.health-toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  color: var(--ink-muted);
  font-size: 12px;
}

.health-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.ocr-health-node {
  display: block;
}

.health-card {
  display: grid;
  min-width: 0;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 6px 10px;
  padding: 16px;
  border: 1px solid var(--rail-border);
  border-radius: 8px;
  background: var(--surface-raised);
}

.health-card__icon {
  grid-row: span 2;
  align-self: center;
  color: var(--active);
  font-size: 22px;
}

.health-card__label {
  color: var(--ink-muted);
  font-size: 12px;
}

.health-card__value {
  overflow-wrap: anywhere;
  color: var(--ink-strong);
  font-size: 20px;
}

@media (max-width: 1352px) {
  .health-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .health-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
