<script setup lang="ts">
import { AlertTriangle } from '@lucide/vue'
import { NIcon } from 'naive-ui'

import EmptyState from '@/components/dashboard/EmptyState.vue'
import StatusTag from '@/components/dashboard/StatusTag.vue'
import type { DocumentRow } from '@/types/dashboard'
import { fileTypeLabel, formatDateTime } from '@/utils/formatters'

defineProps<{
  failures: DocumentRow[]
}>()
</script>

<template>
  <section class="failure-list">
    <div class="failure-list__header">
      <h2 class="failure-list__title">失败队列</h2>
      <NIcon class="failure-list__icon" :component="AlertTriangle" />
    </div>
    <EmptyState v-if="failures.length === 0" description="暂无失败任务" :icon="AlertTriangle" />
    <div v-else class="failure-list__items">
      <article v-for="failure in failures" :key="failure.document_id" class="failure-item">
        <div class="failure-item__main">
          <strong class="failure-item__name">{{ failure.file_name }}</strong>
          <span class="failure-item__meta">
            {{ fileTypeLabel(failure.file_type) }} · {{ formatDateTime(failure.updated_at) }}
          </span>
        </div>
        <StatusTag :status="failure.status" />
        <p class="failure-item__message">
          {{ failure.error_code || 'UNKNOWN' }} · {{ failure.error_message || '无错误详情' }}
        </p>
      </article>
    </div>
  </section>
</template>

<style scoped>
.failure-list {
  min-width: 0;
  padding: 16px;
  border: 1px solid var(--rail-border);
  border-radius: 8px;
  background: var(--surface-raised);
}

.failure-list__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.failure-list__title {
  margin: 0;
  color: var(--ink-strong);
  font-size: 16px;
  font-weight: 700;
}

.failure-list__icon {
  color: var(--danger);
  font-size: 18px;
}

.failure-list__items {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.failure-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 6px 10px;
  padding: 10px;
  border-radius: 7px;
  background: var(--surface-inset);
}

.failure-item__main {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.failure-item__name,
.failure-item__meta,
.failure-item__message {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.failure-item__name {
  color: var(--ink-strong);
  font-size: 13px;
}

.failure-item__meta,
.failure-item__message {
  color: var(--ink-muted);
  font-size: 12px;
}

.failure-item__message {
  grid-column: 1 / -1;
  margin: 0;
}
</style>
