<script setup lang="ts">
import { computed } from 'vue'
import {
  Activity,
  CircleCheck,
  CircleX,
  Clock3,
  Files,
  Gauge
} from '@lucide/vue'
import { NIcon } from 'naive-ui'

import AnimatedNumber from '@/components/dashboard/AnimatedNumber.vue'
import type { DashboardOverview } from '@/types/dashboard'
import { formatDuration, formatNumber, formatPercent } from '@/utils/formatters'

const props = defineProps<{
  overview: DashboardOverview | null
}>()

const metrics = computed(() => {
  const overview = props.overview
  return [
    {
      key: 'documents',
      label: '上传文件',
      value: formatNumber(overview?.document_count),
      note: `${formatNumber(overview?.batch_count)} 个批次`,
      icon: Files,
      tone: 'neutral'
    },
    {
      key: 'success',
      label: '成功率',
      value: formatPercent(overview?.success_rate),
      note: `${formatNumber(overview?.completed_documents)} 个完成`,
      icon: CircleCheck,
      tone: 'success'
    },
    {
      key: 'failure',
      label: '失败率',
      value: formatPercent(overview?.failure_rate),
      note: `${formatNumber(overview?.failed_documents)} 个失败`,
      icon: CircleX,
      tone: 'danger'
    },
    {
      key: 'processing',
      label: '处理中',
      value: formatNumber(overview?.processing_documents),
      note: '队列与解析中',
      icon: Activity,
      tone: 'active'
    },
    {
      key: 'duration',
      label: '平均耗时',
      value: formatDuration(overview?.average_duration_ms),
      note: '从创建到最近更新',
      icon: Clock3,
      tone: 'latency'
    },
    {
      key: 'health',
      label: 'OCR 负载',
      value: overview?.processing_documents ? '忙碌' : '平稳',
      note: 'PaddleOCR 适配器',
      icon: Gauge,
      tone: overview?.processing_documents ? 'active' : 'success'
    }
  ]
})
</script>

<template>
  <section class="metric-strip" aria-label="Dashboard metrics">
    <article v-for="metric in metrics" :key="metric.key" class="metric-card" :class="`metric-card--${metric.tone}`">
      <div class="metric-card__icon">
        <NIcon :component="metric.icon" />
      </div>
      <div class="metric-card__body">
        <span class="metric-card__label">{{ metric.label }}</span>
        <strong class="metric-card__value"><AnimatedNumber :value="metric.value" /></strong>
        <span class="metric-card__note">{{ metric.note }}</span>
      </div>
    </article>
  </section>
</template>

<style scoped>
.metric-strip {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
}

.metric-card {
  display: flex;
  min-width: 0;
  gap: 10px;
  padding: 16px;
  border: 1px solid var(--rail-border);
  border-radius: 8px;
  background: var(--surface-raised);
  box-shadow: var(--shadow-card);
  transition: border-color 180ms ease, box-shadow 180ms ease, transform 180ms ease;
  animation: fadeInUp 300ms ease backwards;
}

.metric-card:nth-child(1) { animation-delay: 0ms; }
.metric-card:nth-child(2) { animation-delay: 40ms; }
.metric-card:nth-child(3) { animation-delay: 80ms; }
.metric-card:nth-child(4) { animation-delay: 120ms; }
.metric-card:nth-child(5) { animation-delay: 160ms; }
.metric-card:nth-child(6) { animation-delay: 200ms; }

.metric-card:hover {
  border-color: var(--rail-border-strong);
  box-shadow: var(--shadow-soft);
  transform: translateY(-1px);
}

.metric-card__icon {
  display: grid;
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 7px;
  background: var(--signal-muted);
  color: var(--signal);
  font-size: 19px;
}

.metric-card__body {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
}

.metric-card__label {
  color: var(--ink-soft);
  font-size: 12px;
}

.metric-card__value {
  overflow-wrap: anywhere;
  color: var(--ink-strong);
  font-size: 22px;
  font-weight: 700;
  line-height: 1.1;
}

.metric-card__note {
  overflow-wrap: anywhere;
  color: var(--ink-muted);
  font-size: 12px;
}

.metric-card--success {
  --signal: var(--success);
  --signal-muted: var(--success-muted);
}

.metric-card--danger {
  --signal: var(--danger);
  --signal-muted: var(--danger-muted);
}

.metric-card--active {
  --signal: var(--active);
  --signal-muted: var(--active-muted);
}

.metric-card--latency {
  --signal: var(--latency);
  --signal-muted: var(--latency-muted);
}

.metric-card--neutral {
  --signal: var(--ink-soft);
  --signal-muted: var(--surface-inset);
}

@media (max-width: 1352px) {
  .metric-strip {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .metric-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
