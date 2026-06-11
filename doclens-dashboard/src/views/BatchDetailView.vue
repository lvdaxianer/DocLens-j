<script setup lang="ts">
import { computed, h, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import type { DataTableColumns } from 'naive-ui'
import {
  NAlert,
  NButton,
  NDataTable,
  NPopconfirm,
  NProgress
} from 'naive-ui'

import { deleteDocument, retryDocument } from '@/api/dashboard'
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
const router = useRouter()
const store = useDashboardStore()
const { selectedBatch, detailState } = storeToRefs(store)

const COMPLETED_STATUS = 'completed'
const FAILED_STATUS = 'failed'
const STALLED_STATUS = 'stalled'
const PROGRESS_BAR_HEIGHT = 8
const DOCUMENT_TABLE_SCROLL_X = 1120

const batchId = computed(() => String(route.params.batchId ?? ''))
const retryingDocumentId = ref('')
const deletingDocumentId = ref('')
const currentRouteDocument = computed(() => {
  const documents = selectedBatch.value?.documents ?? []
  return documents.find((document) => document.status === 'processing')
    ?? documents.find((document) => document.ocr_final_hit_nodes.length > 0)
    ?? documents[0]
    ?? null
})
const {
  resultDrawerOpen,
  selectedResultDocument,
  selectedDocumentResult,
  resultState,
  openDocumentResult,
  retryDocumentResult
} = useDocumentResultDrawer()

/**
 * 判断文档当前状态是否允许显示重试操作。
 *
 * @param status 文档状态
 * @returns 是否可重试
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
function canRetryDocument(status: string): boolean {
  return status === FAILED_STATUS || status === STALLED_STATUS
}

/**
 * 判断文档当前状态是否允许显示删除操作。
 *
 * @param status 文档状态
 * @returns 是否可删除
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
function canDeleteDocument(status: string): boolean {
  return status === COMPLETED_STATUS || status === FAILED_STATUS || status === STALLED_STATUS
}

/**
 * 执行文档重试并刷新当前批次详情。
 *
 * @param documentId 文档 ID
 * @returns 重试完成信号
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
async function handleRetryDocument(documentId: string): Promise<void> {
  retryingDocumentId.value = documentId
  try {
    await retryDocument(documentId)
    await refresh()
  } finally {
    retryingDocumentId.value = ''
  }
}

/**
 * 执行文档删除并刷新当前批次详情。
 *
 * @param documentId 文档 ID
 * @returns 删除完成信号
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
async function handleDeleteDocument(documentId: string): Promise<void> {
  deletingDocumentId.value = documentId
  const isDeletingLastDocument = (selectedBatch.value?.documents.length ?? 0) <= 1
  try {
    await deleteDocument(documentId)
    if (isDeletingLastDocument) {
      // 删除最后一个文档后批次会被清理，避免刷新已不存在的详情。
      router.back()
    } else {
      // 批次仍有文档时刷新当前详情，保持列表状态同步。
      await refresh()
    }
  } finally {
    deletingDocumentId.value = ''
  }
}

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
    key: 'result_action',
    width: 110,
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
  },
  {
    title: '重试',
    key: 'retry_action',
    width: 90,
    fixed: 'right',
    render: (row) =>
      h(NButton, {
        size: 'small',
        secondary: true,
        disabled: !canRetryDocument(row.status),
        loading: retryingDocumentId.value === row.document_id,
        onClick: () => {
          void handleRetryDocument(row.document_id)
        }
      }, {
        default: () => '重试'
      })
  },
  {
    title: '删除',
    key: 'delete_action',
    width: 100,
    fixed: 'right',
    render: (row) =>
      h(NPopconfirm, {
        positiveText: '确认删除',
        negativeText: '取消',
        onPositiveClick: () => {
          if (canDeleteDocument(row.status)) {
            void handleDeleteDocument(row.document_id)
          } else {
            // 不支持删除的状态忽略确认动作。
          }
        }
      }, {
        trigger: () => h(NButton, {
          size: 'small',
          secondary: true,
          disabled: !canDeleteDocument(row.status),
          loading: deletingDocumentId.value === row.document_id
        }, {
          default: () => '删除'
        }),
        default: () => '删除后将同步清理文档结果与关联存储，是否继续？'
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
      :current-document-name="currentRouteDocument?.file_name"
      :current-document-final-hit-nodes="currentRouteDocument?.ocr_final_hit_nodes ?? []"
      :hit-nodes="selectedBatch.batch_dispatch_hit_nodes"
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
        :pagination="{ pageSize: 12 }"
        :row-key="(row) => row.document_id"
        :scroll-x="DOCUMENT_TABLE_SCROLL_X"
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
  gap: 8px;
  align-items: stretch;
}

.batch-summary__item {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
  padding: 8px 10px;
  border: 1px solid var(--rail-border);
  border-radius: 8px;
  background: var(--surface-raised);
}

.batch-summary__item span {
  color: var(--ink-muted);
  font-size: 11px;
}

.batch-summary__item strong {
  overflow-wrap: anywhere;
  color: var(--ink-strong);
  font-size: 16px;
  line-height: 1.25;
}

.batch-summary__item--refresh strong {
  color: var(--active-strong);
}

:deep(.document-name) {
  display: inline-block;
  max-width: 240px;
  overflow: hidden;
  color: var(--ink-strong);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.stage-label) {
  display: inline-block;
  max-width: 132px;
  overflow: hidden;
  color: var(--ink-soft);
  font-size: 12px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.n-progress-custom-content) {
  min-width: 42px;
  color: var(--ink-soft);
  font-size: 11px;
  font-weight: 700;
  text-align: right;
}

:deep(.n-data-table-th),
:deep(.n-data-table-td) {
  padding: 7px 10px;
  font-size: 12px;
}

@media (max-width: 900px) {
  .batch-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
