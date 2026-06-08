<script setup lang="ts">
import { onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { Cpu, Gauge, Timer, TriangleAlert } from '@lucide/vue'
import { NAlert, NButton, NIcon } from 'naive-ui'

import FailureList from '@/components/dashboard/FailureList.vue'
import { useDashboardStore } from '@/stores/dashboard'
import { formatDateTime, formatDuration, formatPercent } from '@/utils/formatters'

const store = useDashboardStore()
const { ocrHealth, ocrHealthState } = storeToRefs(store)

const cards = [
  { key: 'adapter', label: '适配器', icon: Cpu },
  { key: 'success', label: '成功率', icon: Gauge },
  { key: 'failure', label: '失败率', icon: TriangleAlert },
  { key: 'duration', label: '平均请求耗时', icon: Timer }
]

function cardValue(key: string): string {
  if (key === 'adapter') {
    return ocrHealth.value?.adapter_key ?? 'paddle_ocr'
  }
  if (key === 'success') {
    return formatPercent(ocrHealth.value?.success_rate)
  }
  if (key === 'failure') {
    return formatPercent(ocrHealth.value?.failure_rate)
  }
  return formatDuration(ocrHealth.value?.average_duration_ms)
}

function refresh(): void {
  void store.loadOcrHealth()
}

onMounted(refresh)
</script>

<template>
  <div class="view-stack">
    <NAlert v-if="ocrHealthState.error" type="error" :title="ocrHealthState.error" />

    <section class="health-toolbar">
      <span>最后刷新：{{ formatDateTime(ocrHealthState.lastUpdated) }}</span>
      <NButton size="small" :loading="ocrHealthState.loading" @click="refresh">
        刷新
      </NButton>
    </section>

    <section class="health-grid">
      <article v-for="card in cards" :key="card.key" class="health-card">
        <NIcon class="health-card__icon" :component="card.icon" />
        <span class="health-card__label">{{ card.label }}</span>
        <strong class="health-card__value">{{ cardValue(card.key) }}</strong>
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

@media (max-width: 900px) {
  .health-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
