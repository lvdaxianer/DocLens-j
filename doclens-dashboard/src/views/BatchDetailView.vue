<script setup lang="ts">
import { computed, h, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import type { DataTableColumns } from 'naive-ui'
import {
  NAlert,
  NButton,
  NDataTable,
  NProgress
} from 'naive-ui'

import ProcessingRail from '@/components/dashboard/ProcessingRail.vue'
import StatusTag from '@/components/dashboard/StatusTag.vue'
import { useDashboardStore } from '@/stores/dashboard'
import type { DocumentRow } from '@/types/dashboard'
import {
  fileTypeLabel,
  formatDateTime,
  formatDuration,
  formatPercent
} from '@/utils/formatters'

const route = useRoute()
const store = useDashboardStore()
const { selectedBatch, detailState } = storeToRefs(store)

const batchId = computed(() => String(route.params.batchId ?? ''))

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
    title: '处理轨道',
    key: 'track',
    minWidth: 410,
    render: (row) => h(ProcessingRail, { track: row.track, failed: row.status === 'failed' })
  },
  {
    title: '进度',
    key: 'progress_percent',
    width: 150,
    render: (row) =>
      h(NProgress, {
        percentage: row.progress_percent,
        height: 8,
        status: row.status === 'failed' ? 'error' : 'success'
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
  }
]

function refresh(): void {
  if (batchId.value) {
    void store.loadBatchDetail(batchId.value)
  }
}

onMounted(refresh)
watch(batchId, refresh)
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
      <NButton size="small" :loading="detailState.loading" @click="refresh">
        刷新
      </NButton>
    </section>

    <section class="panel">
      <div class="panel__header">
        <div>
          <h2 class="panel__title">文档处理轨道</h2>
          <span class="panel__hint">{{ batchId }}</span>
        </div>
        <StatusTag v-if="selectedBatch" :status="selectedBatch.batch.status" />
      </div>
      <NDataTable
        :columns="columns"
        :data="selectedBatch?.documents ?? []"
        :loading="detailState.loading"
        :pagination="{ pageSize: 8 }"
        :row-key="(row) => row.document_id"
        size="small"
      />
    </section>
  </div>
</template>

<style scoped>
.batch-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr)) auto;
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

:deep(.document-name) {
  display: inline-block;
  max-width: 260px;
  overflow: hidden;
  color: var(--ink-strong);
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 900px) {
  .batch-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
