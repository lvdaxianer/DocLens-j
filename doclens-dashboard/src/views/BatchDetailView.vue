<script setup lang="ts">
import { computed, h, watch } from 'vue'
import { useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import type { DataTableColumns } from 'naive-ui'
import {
  NAlert,
  NButton,
  NDataTable,
  NProgress
} from 'naive-ui'

import BatchOcrRoutePanel from '@/components/dashboard/BatchOcrRoutePanel.vue'
import DocumentResultDrawer from '@/components/dashboard/DocumentResultDrawer.vue'
import DocumentTrackCards from '@/components/dashboard/DocumentTrackCards.vue'
import StatusTag from '@/components/dashboard/StatusTag.vue'
import { useDocumentResultDrawer } from '@/composables/useDocumentResultDrawer'
import { DEFAULT_REFRESH_INTERVAL_SECONDS, useAutoRefresh } from '@/composables/useAutoRefresh'
import { useDashboardStore } from '@/stores/dashboard'
import type { DocumentRow } from '@/types/dashboard'
import {
  fileTypeLabel,
  formatDateTime,
  formatDuration,
  formatImageProgress,
  formatPercent,
  hasImageProgressStage,
  stageLabel
} from '@/utils/formatters'

const route = useRoute()
const store = useDashboardStore()
const { selectedBatch, detailState } = storeToRefs(store)

const COMPLETED_STATUS = 'completed'
const FAILED_STATUS = 'failed'
const PROGRESS_BAR_HEIGHT = 12

const batchId = computed(() => String(route.params.batchId ?? ''))
const {
  resultDrawerOpen,
  selectedResultDocument,
  selectedDocumentResult,
  resultState,
  openDocumentResult,
  retryDocumentResult
} = useDocumentResultDrawer()

const columns: DataTableColumns<DocumentRow> = [
  {
    title: '文件',
    key: 'file_name',
    minWidth: 190,
    render: (row) => h('strong', { class: 'document-name' }, row.file_name)
  },
  {
    title: '类型',
    key: 'file_type',
    width: 100,
    render: (row) => fileTypeLabel(row.file_type)
  },
  {
    title: '状态',
    key: 'status',
    width: 110,
    render: (row) => h(StatusTag, { status: row.status })
  },
  {
    title: '阶段',
    key: 'stage',
    width: 150,
    render: (row) => h('span', { class: 'stage-label' }, stageLabel(row.stage))
  },
  {
    title: '图片进度',
    key: 'image_progress',
    width: 130,
    render: (row) => hasImageProgressStage(row.stage) ? formatImageProgress(row.current_page, row.total_pages) : '-'
  },
  {
    title: '进度',
    key: 'progress_percent',
    width: 190,
    render: (row) =>
      h(NProgress, {
        percentage: row.progress_percent,
        height: PROGRESS_BAR_HEIGHT,
        indicatorPlacement: 'outside',
        status: row.status === FAILED_STATUS ? 'error' : 'success'
      }, {
        default: () => formatPercent(row.progress_percent)
      })
  },
  {
    title: '耗时',
    key: 'duration_ms',
    width: 100,
    render: (row) => formatDuration(row.duration_ms)
  },
  {
    title: '更新时间',
    key: 'updated_at',
    width: 150,
    render: (row) => formatDateTime(row.updated_at)
  },
  {
    title: '操作',
    key: 'actions',
    width: 110,
    fixed: 'right',
    render: (row) =>
      h(NButton, {
        size: 'small',
        secondary: true,
        disabled: row.status !== COMPLETED_STATUS,
        loading: resultState.loading && selectedResultDocument.value?.document_id === row.document_id,
        onClick: () => {
          void openDocumentResult(row)
        }
      }, {
        default: () => '查看文本'
      })
  }
]

/**
 * 刷新当前批次详情。
 *
 * @returns 刷新完成信号
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
function refresh(): Promise<void> {
  if (batchId.value) {
    // 有批次 ID 时刷新当前批次详情。
    return store.loadBatchDetail(batchId.value)
  } else {
    // 路由参数为空时跳过请求。
    return Promise.resolve()
  }
}

watch(batchId, refresh)
useAutoRefresh(refresh)
</script>

<template>
  <div class="view-stack">
    <NAlert v-if="detailState.error" type="error" :title="detailState.error" />

    <section v-if="selectedBatch" class="batch-summary">
      <article class="batch-summary__item">
        <span>进度</span>
        <strong>{{ selectedBatch.batch.progress_percent }}%</strong>
      </article>
      <article class="batch-summary__item">
        <span>成功率</span>
        <strong>{{ formatPercent(selectedBatch.batch.success_rate) }}</strong>
      </article>
      <article class="batch-summary__item">
        <span>失败率</span>
        <strong>{{ formatPercent(selectedBatch.batch.failure_rate) }}</strong>
      </article>
      <article class="batch-summary__item">
        <span>平均耗时</span>
        <strong>{{ formatDuration(selectedBatch.batch.average_duration_ms) }}</strong>
      </article>
      <article class="batch-summary__item batch-summary__item--refresh">
        <span>局部刷新</span>
        <strong>{{ DEFAULT_REFRESH_INTERVAL_SECONDS }} 秒</strong>
      </article>
      <NButton size="small" :loading="detailState.loading" @click="refresh">
        刷新
      </NButton>
    </section>

    <BatchOcrRoutePanel
      v-if="selectedBatch"
      :route-policy="selectedBatch.ocr_route_policy"
      :hit-nodes="selectedBatch.ocr_hit_nodes"
    />

    <section class="panel">
      <div class="panel__header">
        <div>
          <h2 class="panel__title">文档处理轨道</h2>
          <span class="panel__hint">{{ batchId }} · 每 {{ DEFAULT_REFRESH_INTERVAL_SECONDS }} 秒自动刷新</span>
        </div>
        <StatusTag v-if="selectedBatch" :status="selectedBatch.batch.status" />
      </div>
      <DocumentTrackCards :documents="selectedBatch?.documents ?? []" />
      <NDataTable
        :columns="columns"
        :data="selectedBatch?.documents ?? []"
        :loading="detailState.loading"
        :pagination="{ pageSize: 8 }"
        :row-key="(row) => row.document_id"
        size="small"
      />
      <DocumentResultDrawer
        v-model:show="resultDrawerOpen"
        :document="selectedResultDocument"
        :result="selectedDocumentResult"
        :loading="resultState.loading"
        :error="resultState.error"
        @retry="retryDocumentResult"
      />
    </section>
  </div>
</template>

<style scoped>
.batch-summary {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr)) auto;
  gap: 12px;
  align-items: stretch;
}

.batch-summary__item {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
  padding: 12px;
  border: 1px solid var(--rail-border);
  border-radius: 8px;
  background: var(--surface-raised);
}

.batch-summary__item span {
  color: var(--ink-muted);
  font-size: 12px;
}

.batch-summary__item strong {
  overflow-wrap: anywhere;
  color: var(--ink-strong);
  font-size: 20px;
}

.batch-summary__item--refresh strong {
  color: var(--active-strong);
}

:deep(.document-name) {
  display: inline-block;
  max-width: 260px;
  overflow: hidden;
  color: var(--ink-strong);
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.stage-label) {
  display: inline-block;
  max-width: 132px;
  overflow: hidden;
  color: var(--ink-soft);
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.n-progress-custom-content) {
  min-width: 48px;
  color: var(--ink-soft);
  font-size: 13px;
  font-weight: 700;
  text-align: right;
}

@media (max-width: 900px) {
  .batch-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
