<script setup lang="ts">
import { h } from 'vue'
import { RouterLink } from 'vue-router'
import type { DataTableColumns } from 'naive-ui'
import { NButton, NDataTable, NProgress } from 'naive-ui'

import StatusTag from '@/components/dashboard/StatusTag.vue'
import type { BatchRow } from '@/types/dashboard'
import { formatDateTime, formatDuration, formatPercent } from '@/utils/formatters'

defineProps<{
  batches: BatchRow[]
  loading?: boolean
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
    width: 180,
    render: (row) =>
      h(NProgress, {
        percentage: row.progress_percent,
        height: 8,
        indicatorPlacement: 'inside',
        status: row.failed_files > 0 ? 'error' : 'success'
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
    width: 90,
    render: (row) =>
      h(
        RouterLink,
        {
          to: { name: 'batch-detail', params: { batchId: row.batch_id } }
        },
        () => h(NButton, { size: 'small', quaternary: true }, () => '查看')
      )
  }
]
</script>

<template>
  <NDataTable
    class="batch-table"
    :columns="columns"
    :data="batches"
    :loading="loading"
    :pagination="{ pageSize: 10 }"
    :row-key="(row) => row.batch_id"
    size="small"
  />
</template>

<style scoped>
.batch-table {
  border: 1px solid var(--rail-border);
  border-radius: 8px;
  background: var(--surface-raised);
}

:deep(.batch-link) {
  display: inline-block;
  max-width: 160px;
  overflow: hidden;
  color: var(--active);
  font-weight: 650;
  text-decoration: none;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
