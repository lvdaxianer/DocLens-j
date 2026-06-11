<script setup lang="ts">
import { ref } from 'vue'
import { storeToRefs } from 'pinia'
import { NAlert, NButton, NSpin, useMessage } from 'naive-ui'

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
const message = useMessage()

const NON_DELETABLE_BATCH_MESSAGE = '批次中还有等待或处理中的任务，暂时不能删除'

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
  } catch (error) {
    showDeleteBatchError(error)
  } finally {
    deletingBatchId.value = ''
  }
}

/**
 * 展示批次删除失败提示，避免把后端英文错误直接暴露给用户。
 *
 * @param error 删除失败异常
 * @returns 提示展示完成信号
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
function showDeleteBatchError(error: unknown): void {
  if (isNonDeletableBatchError(error)) {
    message.warning(NON_DELETABLE_BATCH_MESSAGE)
  } else {
    message.error(error instanceof Error ? error.message : '批次删除失败')
  }
}

/**
 * 判断错误是否来自批次内仍有等待或处理中任务。
 *
 * @param error 删除失败异常
 * @returns 是否为不可删除批次错误
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
function isNonDeletableBatchError(error: unknown): boolean {
  const errorMessage = error instanceof Error ? error.message : String(error)
  return errorMessage.includes('non-deletable') || errorMessage.includes('status queued')
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
