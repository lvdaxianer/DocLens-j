<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import {
  NButton,
  NDrawer,
  NDrawerContent,
  NForm,
  NFormItem,
  NInput,
  NInputNumber,
  NSelect,
  NSwitch
} from 'naive-ui'

import type { OcrModel, OcrNode, OcrNodePayload, OcrNodeSubmitPayload } from '@/types/ocrResources'

const DEFAULT_PORT = 8080
const DEFAULT_WEIGHT = 100
const DEFAULT_MAX_CONCURRENCY = 4

const props = defineProps<{
  visible: boolean
  models: OcrModel[]
  selectedModelKey: string
  node?: OcrNode | null
  loading?: boolean
}>()

const emit = defineEmits<{
  close: []
  submit: [payload: OcrNodeSubmitPayload]
}>()

const form = reactive({
  modelKey: '',
  name: '',
  host: '',
  port: DEFAULT_PORT,
  enabled: true,
  participateGlobal: true,
  weight: DEFAULT_WEIGHT,
  maxConcurrency: DEFAULT_MAX_CONCURRENCY
})

const title = computed(() => (props.node ? '编辑 OCR 节点' : '新增 OCR 节点'))
const isEditing = computed(() => Boolean(props.node))
const modelOptions = computed(() => props.models.map((model) => ({ label: `${model.name} · ${model.model_key}`, value: model.model_key })))
const canSubmit = computed(() => form.modelKey.trim() !== '' && form.name.trim() !== '' && form.host.trim() !== '')

/**
 * 将节点配置写入本地表单。
 *
 * @returns 写入完成信号
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function syncForm(): void {
  if (props.node) {
    fillFromNode(props.node)
  } else {
    resetForCreate()
  }
}

/**
 * 使用已有节点填充表单。
 *
 * @param node - OCR 节点
 * @returns 填充完成信号
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function fillFromNode(node: OcrNode): void {
  form.modelKey = node.model_key
  form.name = node.name
  form.host = node.host
  form.port = node.port
  form.enabled = node.enabled
  form.participateGlobal = node.participate_global
  form.weight = node.weight
  form.maxConcurrency = node.max_concurrency
}

/**
 * 重置为新增节点默认值。
 *
 * @returns 重置完成信号
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function resetForCreate(): void {
  form.modelKey = props.selectedModelKey || props.models[0]?.model_key || ''
  form.name = ''
  form.host = ''
  form.port = DEFAULT_PORT
  form.enabled = true
  form.participateGlobal = true
  form.weight = DEFAULT_WEIGHT
  form.maxConcurrency = DEFAULT_MAX_CONCURRENCY
}

/**
 * 创建节点提交载荷。
 *
 * @returns 节点提交载荷
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function createPayload(): OcrNodeSubmitPayload {
  const node: OcrNodePayload = {
    name: form.name.trim(),
    host: form.host.trim(),
    port: form.port,
    enabled: form.enabled,
    participate_global: form.participateGlobal,
    weight: form.weight,
    max_concurrency: form.maxConcurrency
  }
  return { modelKey: form.modelKey, node }
}

/**
 * 提交节点表单。
 *
 * @returns 提交完成信号
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function submitForm(): void {
  if (canSubmit.value) {
    emit('submit', createPayload())
  } else {
    // 必填字段不完整时保持抽屉打开，等待用户补全。
  }
}

watch(() => [props.visible, props.node, props.selectedModelKey, props.models.length], syncForm, { immediate: true })
</script>

<template>
  <NDrawer :show="visible" :width="460" @update:show="emit('close')">
    <NDrawerContent :title="title" closable>
      <NForm class="ocr-node-form" label-placement="top">
        <NFormItem label="OCR 模型">
          <NSelect v-model:value="form.modelKey" :disabled="isEditing" :options="modelOptions" />
        </NFormItem>
        <NFormItem label="节点名称">
          <NInput v-model:value="form.name" placeholder="例如 paddle-215" />
        </NFormItem>
        <NFormItem label="Host">
          <NInput v-model:value="form.host" placeholder="只填写主机或 IP" />
        </NFormItem>
        <NFormItem label="Port">
          <NInputNumber v-model:value="form.port" class="ocr-node-form__number" :min="1" :max="65535" />
        </NFormItem>
        <div class="ocr-node-form__switches">
          <NFormItem label="启用节点">
            <NSwitch v-model:value="form.enabled" />
          </NFormItem>
          <NFormItem label="参与全局负载均衡">
            <NSwitch v-model:value="form.participateGlobal" />
          </NFormItem>
        </div>
        <div class="ocr-node-form__grid">
          <NFormItem label="权重">
            <NInputNumber v-model:value="form.weight" class="ocr-node-form__number" :min="1" :max="10000" />
          </NFormItem>
          <NFormItem label="最大并发">
            <NInputNumber v-model:value="form.maxConcurrency" class="ocr-node-form__number" :min="1" :max="1000" />
          </NFormItem>
        </div>
      </NForm>

      <template #footer>
        <div class="ocr-node-form__actions">
          <NButton :disabled="loading" @click="emit('close')">
            取消
          </NButton>
          <NButton type="primary" :loading="loading" :disabled="!canSubmit" @click="submitForm">
            保存
          </NButton>
        </div>
      </template>
    </NDrawerContent>
  </NDrawer>
</template>

<style scoped>
.ocr-node-form {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.ocr-node-form__switches,
.ocr-node-form__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.ocr-node-form__number {
  width: 100%;
}

.ocr-node-form__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
