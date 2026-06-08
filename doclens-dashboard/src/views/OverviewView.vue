<script setup lang="ts">
import { onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { NAlert, NButton, NSpin } from 'naive-ui'

import BatchTable from '@/components/dashboard/BatchTable.vue'
import FailureList from '@/components/dashboard/FailureList.vue'
import LatencyChart from '@/components/dashboard/LatencyChart.vue'
import MetricStrip from '@/components/dashboard/MetricStrip.vue'
import { useDashboardStore } from '@/stores/dashboard'
import { formatDateTime } from '@/utils/formatters'

const store = useDashboardStore()
const { summary, summaryState } = storeToRefs(store)

function refresh(): void {
  void store.loadSummary()
}

onMounted(refresh)
</script>

<template>
  <div class="view-stack">
    <NAlert v-if="summaryState.error" type="error" :title="summaryState.error" />

    <section class="overview-toolbar">
      <span class="overview-toolbar__time">
        最后刷新：{{ formatDateTime(summaryState.lastUpdated) }}
      </span>
      <NButton size="small" :loading="summaryState.loading" @click="refresh">
        刷新
      </NButton>
    </section>

    <NSpin :show="summaryState.loading && !summary">
      <MetricStrip :overview="summary?.overview ?? null" />
    </NSpin>

    <section class="overview-grid">
      <LatencyChart :batches="summary?.recent_batches ?? []" />
      <FailureList :failures="summary?.recent_failures ?? []" />
    </section>

    <section class="panel">
      <div class="panel__header">
        <h2 class="panel__title">最近批次</h2>
        <span class="panel__hint">按更新时间倒序</span>
      </div>
      <BatchTable :batches="summary?.recent_batches ?? []" :loading="summaryState.loading" />
    </section>
  </div>
</template>

<style scoped>
.overview-toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

.overview-toolbar__time {
  color: var(--ink-muted);
  font-size: 12px;
}

.overview-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(280px, 0.6fr);
  gap: 18px;
}

@media (max-width: 980px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
