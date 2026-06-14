<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, useTemplateRef, watch } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart, PieChart } from 'echarts/charts'
import {
  GridComponent,
  LegendComponent,
  TooltipComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { EChartsCoreOption } from 'echarts/core'

import type { BatchRow } from '@/types/dashboard'
import { DASHBOARD_THEME } from '@/theme/dashboardTheme'
import { formatDuration } from '@/utils/formatters'

echarts.use([BarChart, PieChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const props = defineProps<{
  batches: BatchRow[]
}>()

const chartElement = useTemplateRef<HTMLDivElement>('chartElement')
let chart: echarts.ECharts | null = null

const option = computed<EChartsCoreOption>(() => {
  const batches = props.batches.slice(0, 8).reverse()
  return {
    color: [...DASHBOARD_THEME.chartAccentColors],
    grid: {
      left: 58,
      right: 28,
      top: 36,
      bottom: 56
    },
    tooltip: {
      trigger: 'axis',
      valueFormatter: (value: string | number) => formatDuration(Number(value))
    },
    xAxis: {
      type: 'category',
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#d8e0e4' } },
      axisLabel: {
        color: '#5f6f77',
        interval: 0,
        formatter: (value: string) => value.slice(0, 6)
      },
      data: batches.map((batch) => batch.batch_id)
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        color: '#5f6f77',
        formatter: (value: number) => formatDuration(value)
      },
      splitLine: { lineStyle: { color: '#edf2f4' } }
    },
    series: [
      {
        name: '平均耗时',
        type: 'bar',
        barWidth: 18,
        itemStyle: {
          borderRadius: [4, 4, 0, 0]
        },
        data: batches.map((batch) => batch.average_duration_ms)
      }
    ]
  }
})

function renderChart(): void {
  if (!chartElement.value) {
    return
  }
  chart ??= echarts.init(chartElement.value)
  chart.setOption(option.value, true)
}

function resizeChart(): void {
  chart?.resize()
}

watch(option, renderChart)

onMounted(() => {
  renderChart()
  window.addEventListener('resize', resizeChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  chart?.dispose()
  chart = null
})
</script>

<template>
  <div class="chart-panel">
    <div class="chart-panel__header">
      <h2 class="chart-panel__title">批次耗时</h2>
      <span class="chart-panel__hint">最近 8 个批次</span>
    </div>
    <div ref="chartElement" class="chart-panel__canvas" />
  </div>
</template>

<style scoped>
.chart-panel {
  min-width: 0;
  padding: 18px 18px 12px;
  border: 1px solid var(--rail-border);
  border-radius: 8px;
  background: var(--surface-raised);
  box-shadow: var(--shadow-card);
}

.chart-panel__header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  padding: 0 0 10px;
}

.chart-panel__title {
  margin: 0;
  color: var(--ink-strong);
  font-size: 16px;
  font-weight: 700;
}

.chart-panel__hint {
  color: var(--ink-muted);
  font-size: 12px;
}

.chart-panel__canvas {
  width: 100%;
  height: 280px;
}
</style>
