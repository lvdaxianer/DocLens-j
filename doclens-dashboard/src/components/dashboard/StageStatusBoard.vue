<script setup lang="ts">
import { computed } from 'vue'
import { NProgress } from 'naive-ui'
import { Layers3 } from '@lucide/vue'

import AnimatedNumber from '@/components/dashboard/AnimatedNumber.vue'
import EmptyState from '@/components/dashboard/EmptyState.vue'
import type { ImageProgressSummary, StageStatusCount } from '@/types/dashboard'
import { formatImageProgress, formatNumber, formatPercent, stageLabel } from '@/utils/formatters'

const props = defineProps<{
  stages: StageStatusCount[]
  imageProgress: ImageProgressSummary | null
}>()

const visibleStages = computed(() =>
  props.stages.filter((stage) => stage.document_count > 0 || stage.completed_images > 0 || stage.total_images > 0)
)

const imagePercent = computed(() => props.imageProgress?.progress_percent ?? 0)
</script>

<template>
  <section class="stage-board panel">
    <div class="panel__header">
      <div>
        <h2 class="panel__title">文件状态</h2>
        <span class="panel__hint">按处理阶段聚合</span>
      </div>
      <strong class="stage-board__summary">
        {{ formatImageProgress(imageProgress?.completed_images, imageProgress?.total_images) }}
      </strong>
    </div>

    <div class="stage-board__progress">
      <NProgress
        :percentage="imagePercent"
        :height="12"
        indicator-placement="outside"
        status="success"
      >
        {{ formatPercent(imagePercent) }}
      </NProgress>
    </div>

    <EmptyState v-if="visibleStages.length === 0" description="暂无阶段数据" :icon="Layers3" />
    <div v-else class="stage-board__grid">
      <article v-for="stage in visibleStages" :key="stage.stage" class="stage-card">
        <span class="stage-card__label">{{ stage.label || stageLabel(stage.stage) }}</span>
        <strong class="stage-card__value"><AnimatedNumber :value="formatNumber(stage.document_count)" /></strong>
        <span class="stage-card__note">
          {{ formatImageProgress(stage.completed_images, stage.total_images) }}
        </span>
      </article>
    </div>
  </section>
</template>

<style scoped>
.stage-board {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.stage-board__summary {
  color: var(--ink-soft);
  font-size: 13px;
}

.stage-board__progress {
  padding: 0 2px;
}

.stage-board__grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
}

.stage-card {
  min-width: 0;
  padding: 12px;
  border: 1px solid var(--rail-border);
  border-radius: 8px;
  background: var(--surface-inset);
  animation: fadeInUp 300ms ease backwards;
}

.stage-card:nth-child(1) { animation-delay: 0ms; }
.stage-card:nth-child(2) { animation-delay: 40ms; }
.stage-card:nth-child(3) { animation-delay: 80ms; }
.stage-card:nth-child(4) { animation-delay: 120ms; }
.stage-card:nth-child(5) { animation-delay: 160ms; }
.stage-card:nth-child(6) { animation-delay: 200ms; }

.stage-card__label,
.stage-card__note {
  display: block;
  overflow: hidden;
  color: var(--ink-muted);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stage-card__label {
  color: var(--ink-soft);
  font-weight: 650;
}

.stage-card__value {
  display: block;
  margin: 4px 0 2px;
  color: var(--ink-strong);
  font-size: 22px;
  line-height: 1.1;
}

@media (max-width: 1352px) {
  .stage-board__grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .stage-board__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
