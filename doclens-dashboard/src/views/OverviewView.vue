<script setup lang="ts">
import { ref } from 'vue'
import { storeToRefs } from 'pinia'
import { NAlert, NButton, NSpin } from 'naive-ui'

import { deleteBatch } from '@/api/dashboard'
import BatchTable from '@/components/dashboard/BatchTable.vue'
import FailureList from '@/components/dashboard/FailureList.vue'
import LatencyChart from '@/components/dashboard/LatencyChart.vue'
import MetricStrip from '@/components/dashboard/MetricStrip.vue'
import OcrResourceMetricCards from '@/components/dashboard/OcrResourceMetricCards.vue'
import StageStatusBoard from '@/components/dashboard/StageStatusBoard.vue'
import { DEFAULT_REFRESH_INTERVAL_SECONDS, useAutoRefresh } from '@/composables/useAutoRefresh'
import { useDashboardStore } from '@/stores/dashboard'
import { formatDateTime } from '@/utils/formatters'

const store = useDashboardStore()
const { summary, summaryState } = storeToRefs(store)
const deletingBatchId = ref('')

/**
 * 刷新运行总览数据。
 *
 * @returns 刷新完成信号
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
function refresh(): Promise<void> {
  return store.loadSummary()
}

/**
 * 删除总览中的批次并刷新运行总览。
 *
 * @param batchId 批次 ID
 * @returns 删除完成信号
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
async function handleDeleteBatch(batchId: string): Promise<void> {
  deletingBatchId.value = batchId
  try {
    await deleteBatch(batchId)
    await refresh()
  } finally {
    deletingBatchId.value = ''
  }
}

useAutoRefresh(refresh)
</script>

<template>
  <div class="view-stack">
    <NAlert v-if="summaryState.error" type="error" :title="summaryState.error" />

    <section class="overview-toolbar">
      <span class="overview-toolbar__time">
        最后刷新：{{ formatDateTime(summaryState.lastUpdated) }}
      </span>
      <span class="overview-toolbar__auto">每 {{ DEFAULT_REFRESH_INTERVAL_SECONDS }} 秒自动刷新</span>
      <NButton size="small" :loading="summaryState.loading" @click="refresh">
        刷新
      </NButton>
    </section>

    <NSpin :show="summaryState.loading && !summary">
      <MetricStrip :overview="summary?.overview ?? null" />
    </NSpin>

    <StageStatusBoard
      :stages="summary?.stage_status_counts ?? []"
      :image-progress="summary?.image_progress ?? null"
    />

    <OcrResourceMetricCards :metrics="summary?.ocr_resources ?? null" />

    <section class="overview-grid">
      <LatencyChart :batches="summary?.recent_batches ?? []" />
      <FailureList :failures="summary?.recent_failures ?? []" />
    </section>

    <section class="panel">
      <div class="panel__header">
        <h2 class="panel__title">最近批次</h2>
        <span class="panel__hint">按更新时间倒序</span>
      </div>
      <BatchTable
        :batches="summary?.recent_batches ?? []"
        :loading="summaryState.loading"
        :deleting-batch-id="deletingBatchId"
        @delete-batch="handleDeleteBatch"
      />
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

.overview-toolbar__auto {
  color: var(--ink-soft);
  font-size: 12px;
  font-weight: 650;
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
