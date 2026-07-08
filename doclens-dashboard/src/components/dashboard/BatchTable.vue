<script setup lang="ts">
import { h } from 'vue'
import { RouterLink } from 'vue-router'
import type { DataTableColumns } from 'naive-ui'
import { NButton, NDataTable, NPopconfirm, NProgress, NSpace } from 'naive-ui'

import StatusTag from '@/components/dashboard/StatusTag.vue'
import type { BatchRow } from '@/types/dashboard'
import { batchDeleteConfirmMessage } from '@/utils/destructiveConfirmMessages'
import { formatDateTime, formatDuration, formatPercent } from '@/utils/formatters'

const props = defineProps<{
  batches: BatchRow[]
  loading?: boolean
  deletingBatchId?: string
}>()

const emit = defineEmits<{
  'delete-batch': [batchId: string]
}>()

const columns: DataTableColumns<BatchRow> = [
  {
    title: '批次',
    key: 'batch_id',
    width: 180,
    render: (row) =>
      h(
        RouterLink,
        {
          class: 'batch-link',
          to: { name: 'batch-detail', params: { batchId: row.batch_id } }
        },
        () => row.batch_id
      )
  },
  {
    title: '状态',
    key: 'status',
    width: 110,
    render: (row) => h(StatusTag, { status: row.status })
  },
  {
    title: '进度',
    key: 'progress_percent',
    width: 230,
    render: (row) =>
      h(NProgress, {
        percentage: row.progress_percent,
        height: 8,
        indicatorPlacement: 'outside',
        status: row.failed_files > 0 ? 'error' : 'success'
      }, {
        default: () => formatPercent(row.progress_percent)
      })
  },
  {
    title: '文件',
    key: 'total_files',
    width: 120,
    render: (row) => `${row.completed_files}/${row.total_files}`
  },
  {
    title: '成功率',
    key: 'success_rate',
    width: 100,
    render: (row) => formatPercent(row.success_rate)
  },
  {
    title: '失败率',
    key: 'failure_rate',
    width: 100,
    render: (row) => formatPercent(row.failure_rate)
  },
  {
    title: '平均耗时',
    key: 'average_duration_ms',
    width: 120,
    render: (row) => formatDuration(row.average_duration_ms)
  },
  {
    title: '更新时间',
    key: 'updated_at',
    width: 150,
    render: (row) => formatDateTime(row.updated_at)
  },
  {
    title: '',
    key: 'actions',
    width: 150,
    render: (row) =>
      h(NSpace, { size: 6, wrapItem: false }, {
        default: () => [
          h(
            RouterLink,
            {
              to: { name: 'batch-detail', params: { batchId: row.batch_id } }
            },
            () => h(NButton, { size: 'small', quaternary: true }, () => '查看')
          ),
          h(NPopconfirm, {
            positiveText: '确认删除',
            negativeText: '取消',
            onPositiveClick: () => emit('delete-batch', row.batch_id)
          }, {
            trigger: () => h(NButton, {
              size: 'small',
              quaternary: true,
              type: 'error',
              loading: row.batch_id === props.deletingBatchId
            }, () => '删除'),
            default: () => batchDeleteConfirmMessage(row.batch_id)
          })
        ]
      })
  }
]
</script>

<template>
  <div class="batch-table-shell">
    <NDataTable
      class="batch-table"
      :columns="columns"
      :data="batches"
      :loading="loading"
      :pagination="{ pageSize: 10 }"
      :row-key="(row) => row.batch_id"
      size="small"
    />
  </div>
</template>

<style scoped>
.batch-table-shell {
  overflow-x: auto;
  padding: 10px 12px 8px;
  border: 1px solid var(--rail-border);
  border-radius: 8px;
  background: var(--surface-raised);
  box-shadow: var(--shadow-card);
}

.batch-table {
  min-width: 1040px;
}

:deep(.n-data-table-th),
:deep(.n-data-table-td) {
  padding: 8px 12px;
  font-size: 12px;
}

:deep(.n-data-table-th) {
  color: var(--ink-soft);
  font-size: 11px;
  font-weight: 700;
}

:deep(.n-progress-custom-content) {
  min-width: 42px;
  color: var(--ink-soft);
  font-size: 11px;
  font-weight: 700;
  text-align: right;
}

:deep(.n-data-table-tr .n-data-table-td) {
  transition: background-color 120ms ease;
}

:deep(.n-data-table-tr:hover .n-data-table-td) {
  background: var(--surface-hover);
}

:deep(.batch-link) {
  display: inline-block;
  max-width: 120px;
  overflow: hidden;
  color: var(--active);
  font-weight: 650;
  text-decoration: none;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.batch-link:hover) {
  color: var(--active-strong);
  text-decoration: underline;
}
</style>
