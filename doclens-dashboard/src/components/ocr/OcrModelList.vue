<script setup lang="ts">
import { computed } from 'vue'
import { Boxes, CheckCircle2, CircleAlert, Server } from '@lucide/vue'
import { NButton, NIcon, NTag } from 'naive-ui'

import type { OcrModel } from '@/types/ocrResources'
import { formatNumber } from '@/utils/formatters'

const props = defineProps<{
  models: OcrModel[]
  selectedModelKey: string
}>()

const emit = defineEmits<{
  select: [modelKey: string]
}>()

const sortedModels = computed(() => [...props.models].sort((left, right) => modelDisplayName(left).localeCompare(modelDisplayName(right))))

/**
 * 获取 OCR 模型展示名称。
 *
 * @param model - OCR 模型
 * @returns 展示名称
 * @author lvdaxianerplus
 * @date 2026-06-13
 */
function modelDisplayName(model: OcrModel): string {
  return model.name ?? model.model_key
}

/**
 * 计算异常节点数量。
 *
 * @param model - OCR 模型
 * @returns 异常节点数量
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function unhealthyNodeCount(model: OcrModel): number {
  return Math.max(0, model.node_count - model.healthy_node_count)
}

/**
 * 选择 OCR 模型。
 *
 * @param modelKey - OCR 模型标识
 * @returns 选择完成信号
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function selectModel(modelKey: string): void {
  emit('select', modelKey)
}
</script>

<template>
  <section class="ocr-model-list" aria-label="OCR 模型列表">
    <article
      v-for="model in sortedModels"
      :key="model.model_key"
      class="ocr-model"
      :class="{ 'ocr-model--active': model.model_key === selectedModelKey }"
    >
      <div class="ocr-model__header">
        <span class="ocr-model__icon">
          <NIcon :component="Server" />
        </span>
        <div class="ocr-model__title">
          <strong>{{ modelDisplayName(model) }}</strong>
          <span>{{ model.model_key }}</span>
        </div>
        <NTag size="small" :type="model.healthy_node_count > 0 ? 'success' : 'warning'">
          {{ model.healthy_node_count > 0 ? '可调度' : '待恢复' }}
        </NTag>
      </div>

      <p class="ocr-model__description">{{ model.description ?? '暂无模型说明' }}</p>

      <div class="ocr-model__stats">
        <span>
          <NIcon :component="Boxes" />
          {{ formatNumber(model.node_count) }} 节点
        </span>
        <span>
          <NIcon :component="CheckCircle2" />
          {{ formatNumber(model.healthy_node_count) }} 健康
        </span>
        <span>
          <NIcon :component="CircleAlert" />
          {{ formatNumber(unhealthyNodeCount(model)) }} 异常
        </span>
      </div>

      <div class="ocr-model__footer">
        <span>{{ model.ocr_path }} · {{ model.health_path }}</span>
        <NButton size="small" :type="model.model_key === selectedModelKey ? 'primary' : 'default'" @click="selectModel(model.model_key)">
          查看节点
        </NButton>
      </div>
    </article>
  </section>
</template>

<style scoped>
.ocr-model-list {
  display: grid;
  min-width: 0;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 12px;
}

.ocr-model {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  border: 1px solid var(--rail-border);
  border-radius: 8px;
  background: var(--surface-raised);
  box-shadow: var(--shadow-card);
}

.ocr-model--active {
  border-color: rgba(249, 115, 22, 0.46);
  box-shadow: var(--focus-ring);
}

.ocr-model__header,
.ocr-model__footer,
.ocr-model__stats,
.ocr-model__stats span {
  display: flex;
  align-items: center;
}

.ocr-model__header {
  gap: 10px;
}

.ocr-model__icon {
  display: grid;
  width: 36px;
  height: 36px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 8px;
  background: var(--active-muted);
  color: var(--active);
  font-size: 21px;
}

.ocr-model__title {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
}

.ocr-model__title strong,
.ocr-model__title span,
.ocr-model__description,
.ocr-model__footer span {
  overflow-wrap: anywhere;
}

.ocr-model__title strong {
  color: var(--ink-strong);
  font-size: 15px;
}

.ocr-model__title span,
.ocr-model__description,
.ocr-model__footer span {
  color: var(--ink-muted);
  font-size: 12px;
}

.ocr-model__description {
  min-height: 36px;
  margin: 0;
}

.ocr-model__stats {
  flex-wrap: wrap;
  gap: 8px 12px;
}

.ocr-model__stats span {
  gap: 5px;
  color: var(--ink-soft);
  font-size: 12px;
  font-weight: 650;
}

.ocr-model__footer {
  justify-content: space-between;
  gap: 10px;
}
</style>
