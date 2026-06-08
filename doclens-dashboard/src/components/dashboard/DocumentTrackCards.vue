<script setup lang="ts">
import { NProgress } from 'naive-ui'

import ProcessingRail from '@/components/dashboard/ProcessingRail.vue'
import StatusTag from '@/components/dashboard/StatusTag.vue'
import type { DocumentRow } from '@/types/dashboard'
import {
  fileTypeLabel,
  formatDuration,
  formatImageProgress,
  formatPercent,
  hasImageProgressStage,
  stageLabel
} from '@/utils/formatters'

/**
 * 文档处理轨道卡片列表，展示每个文件的阶段状态、图片页进度和耗时。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
defineProps<{
  documents: DocumentRow[]
}>()
</script>

<template>
  <section class="document-track-list" aria-label="文件处理步骤">
    <article v-for="document in documents" :key="document.document_id" class="document-track">
      <div class="document-track__header">
        <div class="document-track__identity">
          <strong class="document-track__name">{{ document.file_name }}</strong>
          <span class="document-track__meta">
            {{ fileTypeLabel(document.file_type) }} · {{ stageLabel(document.stage) }}
          </span>
        </div>
        <StatusTag :status="document.status" />
      </div>

      <div class="document-track__rail">
        <ProcessingRail :track="document.track" :failed="document.status === 'failed'" />
      </div>

      <div class="document-track__footer">
        <div class="document-track__progress">
          <span>整体进度</span>
          <NProgress
            :percentage="document.progress_percent"
            :height="12"
            :indicator-placement="'outside'"
            :status="document.status === 'failed' ? 'error' : 'success'"
          >
            {{ formatPercent(document.progress_percent) }}
          </NProgress>
        </div>
        <span>
          图片：{{ hasImageProgressStage(document.stage)
            ? formatImageProgress(document.current_page, document.total_pages)
            : '-' }}
        </span>
        <span>耗时：{{ formatDuration(document.duration_ms) }}</span>
      </div>
    </article>
  </section>
</template>

<style scoped>
.document-track-list {
  display: grid;
  gap: 12px;
  margin-bottom: 18px;
}

.document-track {
  display: grid;
  gap: 14px;
  padding: 16px 18px;
  border: 1px solid var(--rail-border);
  border-radius: 8px;
  background: var(--surface-raised);
}

.document-track__header {
  display: flex;
  min-width: 0;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.document-track__identity {
  display: grid;
  min-width: 0;
  gap: 4px;
}

.document-track__name {
  overflow: hidden;
  color: var(--ink-strong);
  font-size: 14px;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.document-track__meta,
.document-track__footer {
  color: var(--ink-muted);
  font-size: 12px;
}

.document-track__rail {
  overflow-x: auto;
  padding: 6px 2px 10px;
}

.document-track__footer {
  display: grid;
  grid-template-columns: minmax(240px, 1fr) auto auto;
  gap: 14px;
  align-items: center;
}

.document-track__progress {
  display: grid;
  min-width: 0;
  grid-template-columns: auto minmax(160px, 1fr);
  gap: 10px;
  align-items: center;
}

.document-track__progress span {
  color: var(--ink-soft);
  font-weight: 700;
}

:deep(.n-progress-custom-content) {
  min-width: 48px;
  color: var(--ink-soft);
  font-size: 13px;
  font-weight: 700;
  text-align: right;
}

@media (max-width: 900px) {
  .document-track__header,
  .document-track__footer,
  .document-track__progress {
    grid-template-columns: 1fr;
  }

  .document-track__header {
    display: grid;
  }
}
</style>
