<script setup lang="ts">
import { computed, onMounted, reactive, shallowRef, watch } from 'vue'
import { NAlert, NFormItem, NRadio, NRadioGroup, NSelect } from 'naive-ui'

import { fetchOcrModels, fetchOcrNodes } from '@/api/ocrResources'
import type { OcrModel, OcrNode } from '@/types/ocrResources'
import type { OcrRoutingMode, UploadOcrRoutingOptions } from '@/types/upload'
import {
  DEFAULT_UPLOAD_LOAD_BALANCE_STRATEGY,
  createDefaultUploadOcrRouting
} from '@/utils/uploadFormRules'

const emit = defineEmits<{
  change: [value: UploadOcrRoutingOptions]
}>()

const models = shallowRef<OcrModel[]>([])
const nodes = shallowRef<OcrNode[]>([])
const loadingModels = shallowRef(false)
const loadingNodes = shallowRef(false)
const errorMessage = shallowRef('')
let form = reactive<UploadOcrRoutingOptions>(createDefaultUploadOcrRouting())

const modelOptions = computed(() => models.value.map((model) => ({
  label: model.name,
  value: model.model_key
})))
const nodeOptions = computed(() => nodes.value.map((node) => ({
  label: `${node.name} · ${node.host}:${node.port}${node.enabled ? '' : ' · 已停用'}`,
  value: node.id,
  disabled: !node.enabled
})))
const hasModelSelect = computed(() => form.ocrRoutingMode === 'MODEL_LOAD_BALANCE' || form.ocrRoutingMode === 'SPECIFIC_NODE')
const hasNodeSelect = computed(() => form.ocrRoutingMode === 'SPECIFIC_NODE')
const hasStrategySelect = computed(() => form.ocrRoutingMode === 'GLOBAL_LOAD_BALANCE' || form.ocrRoutingMode === 'MODEL_LOAD_BALANCE')
const validationMessage = computed(() => validateRouting())

const strategyOptions = [
  { label: '加权空闲优先', value: DEFAULT_UPLOAD_LOAD_BALANCE_STRATEGY }
]

/**
 * 将未知异常转换为错误消息。
 *
 * @param error - 捕获到的异常
 * @returns 错误消息
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function toErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error)
}

/**
 * 加载 OCR 模型列表。
 *
 * @returns 加载完成信号
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
async function loadModels(): Promise<void> {
  loadingModels.value = true
  errorMessage.value = ''
  try {
    const response = await fetchOcrModels()
    models.value = response.items
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loadingModels.value = false
  }
}

/**
 * 加载指定模型下的 OCR 节点。
 *
 * @param modelKey - OCR 模型标识
 * @returns 加载完成信号
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
async function loadNodes(modelKey: string): Promise<void> {
  if (!modelKey) {
    nodes.value = []
    return
  }
  loadingNodes.value = true
  errorMessage.value = ''
  try {
    const response = await fetchOcrNodes(modelKey)
    nodes.value = response.items
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loadingNodes.value = false
  }
}

/**
 * 校验 OCR 路由选择。
 *
 * @returns 校验消息，空字符串表示通过
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function validateRouting(): string {
  if (form.ocrRoutingMode === 'MODEL_LOAD_BALANCE' && !form.ocrModelKey) {
    return '指定 OCR 时需要选择模型'
  } else if (form.ocrRoutingMode === 'SPECIFIC_NODE' && !form.ocrModelKey) {
    return '指定节点时需要先选择模型'
  } else if (form.ocrRoutingMode === 'SPECIFIC_NODE' && !form.ocrNodeId) {
    return '指定节点时需要选择健康节点'
  } else {
    return ''
  }
}

/**
 * 创建可提交 OCR 路由参数。
 *
 * @returns OCR 路由参数
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function createValue(): UploadOcrRoutingOptions {
  return {
    ocrRoutingMode: form.ocrRoutingMode,
    ocrModelKey: hasModelSelect.value ? form.ocrModelKey : '',
    ocrNodeId: hasNodeSelect.value ? form.ocrNodeId : '',
    ocrLoadBalanceStrategy: hasStrategySelect.value ? form.ocrLoadBalanceStrategy : ''
  }
}

/**
 * 切换 OCR 路由模式。
 *
 * @param mode - OCR 路由模式
 * @returns 切换完成信号
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function updateMode(mode: OcrRoutingMode): void {
  form.ocrRoutingMode = mode
  if (hasStrategySelect.value) {
    form.ocrLoadBalanceStrategy = DEFAULT_UPLOAD_LOAD_BALANCE_STRATEGY
  } else {
    form.ocrLoadBalanceStrategy = ''
  }
  if (!hasModelSelect.value) {
    form.ocrModelKey = ''
  } else {
    // 需要模型时保留当前选择，方便用户在模式之间切换。
  }
  if (!hasNodeSelect.value) {
    form.ocrNodeId = ''
  } else {
    // 指定节点模式下等待节点列表加载后选择。
  }
}

/**
 * 重置 OCR 路由选择。
 *
 * @returns 重置完成信号
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function reset(): void {
  Object.assign(form, createDefaultUploadOcrRouting())
  nodes.value = []
  emit('change', createValue())
}

watch(() => form.ocrModelKey, async (modelKey) => {
  form.ocrNodeId = ''
  await loadNodes(modelKey)
})

watch(form, () => {
  emit('change', createValue())
})

onMounted(loadModels)
defineExpose({ reset, validateRouting })
</script>

<template>
  <section class="ocr-routing-selector">
    <NAlert v-if="errorMessage" type="error" :title="errorMessage" />
    <NAlert v-if="validationMessage" type="warning" :title="validationMessage" />

    <NFormItem label="OCR 路由方式">
      <NRadioGroup :value="form.ocrRoutingMode" class="ocr-routing-selector__modes" @update:value="updateMode">
        <NRadio value="GLOBAL_LOAD_BALANCE">全局负载均衡</NRadio>
        <NRadio value="MODEL_LOAD_BALANCE">指定 OCR</NRadio>
        <NRadio value="SPECIFIC_NODE">指定节点</NRadio>
      </NRadioGroup>
    </NFormItem>

    <div class="ocr-routing-selector__grid">
      <NFormItem v-if="hasModelSelect" label="OCR 模型">
        <NSelect
          v-model:value="form.ocrModelKey"
          :loading="loadingModels"
          :options="modelOptions"
          placeholder="选择系统支持的 OCR"
        />
      </NFormItem>
      <NFormItem v-if="hasNodeSelect" label="OCR 节点">
        <NSelect
          v-model:value="form.ocrNodeId"
          :loading="loadingNodes"
          :options="nodeOptions"
          placeholder="选择该 OCR 下的节点"
        />
      </NFormItem>
      <NFormItem v-if="hasStrategySelect" label="负载均衡策略">
        <NSelect v-model:value="form.ocrLoadBalanceStrategy" :options="strategyOptions" />
      </NFormItem>
    </div>
  </section>
</template>

<style scoped>
.ocr-routing-selector {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 8px;
  padding: 14px;
  border: 1px solid var(--rail-border);
  border-radius: 8px;
  background: var(--surface-inset);
}

.ocr-routing-selector__modes {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 14px;
}

.ocr-routing-selector__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

@media (max-width: 900px) {
  .ocr-routing-selector__grid {
    grid-template-columns: 1fr;
  }
}
</style>
