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
  NRadioButton,
  NRadioGroup,
  NSelect,
  NSwitch
} from 'naive-ui'

import type { OcrModel, OcrNode, OcrNodeSubmitPayload } from '@/types/ocrResources'
import {
  createDefaultOcrNodeForm,
  createOcrNodePayload,
  DASHSCOPE_CHANNEL_KEY,
  fillOcrNodeFormFromNode,
  isOcrNodeFormSubmittable
} from '@/utils/ocrNodeFormRules'

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

let form = reactive(createDefaultOcrNodeForm(''))

const title = computed(() => (props.node ? '编辑 OCR 节点' : '新增 OCR 节点'))
const isEditing = computed(() => Boolean(props.node))
const modelOptions = computed(() => props.models.map((model) => ({ label: `${model.name} · ${model.model_key}`, value: model.model_key })))
const channelOptions = [
  {
    label: '阿里百炼 DashScope',
    value: DASHSCOPE_CHANNEL_KEY
  }
]
const canSubmit = computed(() => isOcrNodeFormSubmittable(form))

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
  Object.assign(form, fillOcrNodeFormFromNode(node))
}

/**
 * 重置为新增节点默认值。
 *
 * @returns 重置完成信号
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function resetForCreate(): void {
  Object.assign(form, createDefaultOcrNodeForm(props.selectedModelKey || props.models[0]?.model_key || ''))
}

/**
 * 创建节点提交载荷。
 *
 * @returns 节点提交载荷
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function createPayload(): OcrNodeSubmitPayload {
  return createOcrNodePayload(form)
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
  <NDrawer :show="visible" :width="520" @update:show="emit('close')">
    <NDrawerContent :title="title" closable>
      <NForm class="ocr-node-form" label-placement="top">
        <NFormItem label="部署类型">
          <NRadioGroup v-model:value="form.deploymentType" :disabled="isEditing">
            <NRadioButton value="OFFLINE">
              离线节点
            </NRadioButton>
            <NRadioButton value="ONLINE">
              在线节点
            </NRadioButton>
          </NRadioGroup>
        </NFormItem>
        <NFormItem label="OCR 模型">
          <NSelect v-model:value="form.modelKey" :disabled="isEditing" :options="modelOptions" />
        </NFormItem>
        <NFormItem label="节点名称">
          <NInput v-model:value="form.name" placeholder="例如 paddle-215" />
        </NFormItem>
        <template v-if="form.deploymentType === 'OFFLINE'">
          <NFormItem label="Host">
            <NInput v-model:value="form.host" placeholder="只填写主机或 IP" />
          </NFormItem>
          <NFormItem label="Port">
            <NInputNumber v-model:value="form.port" class="ocr-node-form__number" :min="1" :max="65535" />
          </NFormItem>
        </template>
        <template v-else>
          <NFormItem label="在线渠道">
            <NSelect v-model:value="form.channelKey" :options="channelOptions" />
          </NFormItem>
          <NFormItem label="模型名称">
            <NInput v-model:value="form.providerModel" placeholder="qwen-vl-ocr-2025-11-20" />
          </NFormItem>
          <NFormItem label="API Key">
            <NInput
              v-model:value="form.apiKey"
              type="password"
              :placeholder="form.credentialConfigured ? '已配置，留空则沿用旧密钥' : '保存后不再回显'"
            />
          </NFormItem>
        </template>
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
