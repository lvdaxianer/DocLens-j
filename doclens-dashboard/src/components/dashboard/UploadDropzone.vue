<script setup lang="ts">
import { computed, reactive, shallowRef, useTemplateRef } from 'vue'
import { NButton, useMessage } from 'naive-ui'

import UploadAdvancedOptions from '@/components/upload/UploadAdvancedOptions.vue'
import OcrRoutingSelector from '@/components/upload/OcrRoutingSelector.vue'
import UploadFileList from '@/components/dashboard/UploadFileList.vue'
import UploadFilePicker from '@/components/dashboard/UploadFilePicker.vue'
import type { UploadAdvancedOptionsValue, UploadBatchOptions, UploadOcrRoutingOptions } from '@/types/upload'
import {
  createDefaultUploadAdvancedOptions,
  createDefaultUploadOcrRouting,
  validateMetadataJson
} from '@/utils/uploadFormRules'

// UploadDropzone 是上传表单的编排组件。
// 维护边界：
// - 文件选择 UI 放在 UploadFilePicker。
// - 文件列表 UI 放在 UploadFileList。
// - 高级参数 UI 放在 UploadAdvancedOptions。
// - OCR 路由 UI 放在 OcrRoutingSelector。
// - 本组件只保存提交所需的表单状态。
// - 本组件只在 submitUpload 中做最终提交校验。
// - 本组件不直接调用上传 API，上传动作交给父级 store。
// - resetForm 是给父级 UploadView 调用的唯一暴露方法。
// - 文件 picker 的原生 input reset 由子组件完成。
// - OCR 路由 selector 的 reset 由子组件公开方法完成。
// - metadata 校验留在这里，因为它影响是否发出 submit。
// - OCR 路由校验留在 selector，因为字段依赖和展示都在子组件内。
// - selectedFiles 使用 shallowRef，避免深度代理浏览器 File 对象。
// - form 使用 reactive，方便 v-model 传给高级参数组件。
// - ocrRouting 使用 reactive，方便按字段同步选择器输出。
// - EMPTY_UPLOAD_OPTIONS 统一承接默认值，避免 reset 和初始化分叉。
// - DEFAULT_ADVANCED_OPTIONS 来自规则工具，保持默认值可测试。
// - DEFAULT_OCR_ROUTING 来自规则工具，保持上传路由默认值一致。
// - loading 是父级传入状态，只控制按钮交互。
// - hasFiles 派生上传按钮可用状态，不额外维护布尔状态。
// - removeFile 按文件名移除，延续原有交互行为。
// - updateFiles 接受 FileList|null，兼容 input 和 drop 两种来源。
// - submit 事件只传 UploadBatchOptions，不泄漏内部组件状态。
// - 用户提示通过 Naive UI message，保持和页面其它提示一致。
// - 不在这里展示上传结果，上传结果属于 UploadView 侧栏。
// - 不在这里展示解析进度，解析进度属于批次详情页。
// - 不在这里做文件类型二次校验，accept 已由 picker 声明。
// - 不在这里做文件大小限制，当前产品需求尚未要求。
// - 不在这里处理拖拽 hover 态，视觉反馈属于 picker。
// - 不在这里计算文件总大小，文件列表组件负责展示。
// - 不在这里拼接文件行 key，文件列表组件负责渲染。
// - 不在这里缓存 submit payload，提交时即时组装避免陈旧值。
// - 不在这里捕获父级上传异常，store 会写入 uploadState.error。
// - 不在这里硬编码 OCR 模型选项，路由选择器自己加载规则。
// - 不在这里读取路由，上传表单应可独立挂载。
// - 不在这里发起自动刷新，上传页没有轮询需求。
// - 不在这里引入大图标，视觉资源都由子组件按需引入。
// - 不在这里定义文件列表样式，避免父组件再次膨胀。
// - 不在这里定义文件选择区样式，避免布局职责混淆。
// - 新增上传字段时，应先判断属于高级参数还是 OCR 路由。
// - 新增提交前校验时，应优先放在对应子组件展示错误。
// - 只有跨多个子组件的最终校验才放回 submitUpload。
// - 这个组件的目标是“组装上传请求”，不是“画完整上传 UI”。
const DEFAULT_ADVANCED_OPTIONS = createDefaultUploadAdvancedOptions()
const DEFAULT_OCR_ROUTING = createDefaultUploadOcrRouting()
const EMPTY_UPLOAD_OPTIONS: UploadBatchOptions = {
  files: [],
  metadata: DEFAULT_ADVANCED_OPTIONS.metadata,
  callbackUrl: DEFAULT_ADVANCED_OPTIONS.callbackUrl,
  idempotencyKey: DEFAULT_ADVANCED_OPTIONS.idempotencyKey,
  ocrRoutingMode: DEFAULT_OCR_ROUTING.ocrRoutingMode,
  ocrModelKey: DEFAULT_OCR_ROUTING.ocrModelKey,
  ocrNodeId: DEFAULT_OCR_ROUTING.ocrNodeId,
  ocrLoadBalanceStrategy: DEFAULT_OCR_ROUTING.ocrLoadBalanceStrategy
}

const emit = defineEmits<{
  submit: [options: UploadBatchOptions]
}>()

defineProps<{
  loading?: boolean
}>()

const filePicker = useTemplateRef<InstanceType<typeof UploadFilePicker>>('filePicker')
const ocrRoutingSelector = useTemplateRef<InstanceType<typeof OcrRoutingSelector>>('ocrRoutingSelector')
const message = useMessage()
const selectedFiles = shallowRef<File[]>([])
let form = reactive<UploadAdvancedOptionsValue>({
  metadata: EMPTY_UPLOAD_OPTIONS.metadata,
  callbackUrl: EMPTY_UPLOAD_OPTIONS.callbackUrl,
  idempotencyKey: EMPTY_UPLOAD_OPTIONS.idempotencyKey
})

const hasFiles = computed(() => selectedFiles.value.length > 0)
const ocrRouting = reactive<UploadOcrRoutingOptions>({
  ocrRoutingMode: EMPTY_UPLOAD_OPTIONS.ocrRoutingMode,
  ocrModelKey: EMPTY_UPLOAD_OPTIONS.ocrModelKey,
  ocrNodeId: EMPTY_UPLOAD_OPTIONS.ocrNodeId,
  ocrLoadBalanceStrategy: EMPTY_UPLOAD_OPTIONS.ocrLoadBalanceStrategy
})

/**
 * 更新待上传文件列表。
 *
 * @param files - 浏览器文件列表
 * @returns 更新完成信号
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function updateFiles(files: FileList | null): void {
  if (files) {
    // 有文件列表时转换为稳定数组。
    // Array.from 会快照当前选择，避免后续 FileList 被浏览器重用。
    selectedFiles.value = Array.from(files)
  } else {
    // 无文件列表时清空待上传文件。
    selectedFiles.value = []
  }
}

/**
 * 从待上传列表移除文件。
 *
 * @param fileName - 文件名称
 * @returns 移除完成信号
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function removeFile(fileName: string): void {
  // 只在内存数组中移除，原生 input 值由整体 reset 时清空。
  selectedFiles.value = selectedFiles.value.filter((file) => file.name !== fileName)
}

/**
 * 重置上传表单。
 *
 * @returns 重置完成信号
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function resetForm(): void {
  // 清空文件数组，确保列表组件立即回到空态。
  selectedFiles.value = []
  // 高级参数恢复默认 metadata。
  form.metadata = EMPTY_UPLOAD_OPTIONS.metadata
  // 高级参数恢复默认回调地址。
  form.callbackUrl = EMPTY_UPLOAD_OPTIONS.callbackUrl
  // 高级参数恢复默认幂等键。
  form.idempotencyKey = EMPTY_UPLOAD_OPTIONS.idempotencyKey
  // OCR 路由选择器拥有自己的内部校验状态，需要委托子组件重置。
  ocrRoutingSelector.value?.reset()
  // 原生文件输入框的 value 只能由 picker 组件自己清理。
  filePicker.value?.reset()
}

/**
 * 同步 OCR 路由选择器值。
 *
 * @param value - OCR 路由参数
 * @returns 同步完成信号
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function updateOcrRouting(value: UploadOcrRoutingOptions): void {
  // 路由模式决定后续 node/model 字段是否必填。
  ocrRouting.ocrRoutingMode = value.ocrRoutingMode
  // 模型 key 由选择器保证来自可选项。
  ocrRouting.ocrModelKey = value.ocrModelKey
  // 节点 ID 只在指定节点模式下有业务含义。
  ocrRouting.ocrNodeId = value.ocrNodeId
  // 负载均衡策略只在全局调度模式下生效。
  ocrRouting.ocrLoadBalanceStrategy = value.ocrLoadBalanceStrategy
}

/**
 * 提交上传表单。
 *
 * @returns 提交完成信号
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function submitUpload(): void {
  // OCR 路由校验先执行，让选择器有机会更新自己的错误展示。
  const validationMessage = ocrRoutingSelector.value?.validateRouting() ?? ''
  // metadata 是文本域输入，必须在提交前单独验证 JSON。
  const metadataValidationMessage = validateMetadataJson(form.metadata)
  if (metadataValidationMessage) {
    // 元数据 JSON 不合法时阻止提交并提示用户。
    message.warning(metadataValidationMessage)
  } else if (validationMessage) {
    // OCR 路由字段不完整时保持表单不提交，由选择器展示校验信息。
  } else {
    // 所有上传字段合法时向父组件发出提交事件。
    // payload 使用当前响应式状态即时组装，避免缓存旧值。
    emit('submit', {
      files: selectedFiles.value,
      metadata: form.metadata,
      callbackUrl: form.callbackUrl,
      idempotencyKey: form.idempotencyKey,
      ocrRoutingMode: ocrRouting.ocrRoutingMode,
      ocrModelKey: ocrRouting.ocrModelKey,
      ocrNodeId: ocrRouting.ocrNodeId,
      ocrLoadBalanceStrategy: ocrRouting.ocrLoadBalanceStrategy
    })
  }
}

defineExpose({ resetForm })
</script>

<template>
  <!-- 上传表单保持纵向编排，具体 UI 段落由子组件负责。 -->
  <section class="upload-dropzone">
    <!-- 文件选择器负责 input/drop 事件，并把 FileList 上抛给父组件。 -->
    <UploadFilePicker ref="filePicker" @change="updateFiles" />

    <!-- 文件列表只展示当前内存文件数组，并上抛移除意图。 -->
    <UploadFileList :files="selectedFiles" @remove="removeFile" />

    <!-- 高级参数通过 v-model 直接同步到 form。 -->
    <UploadAdvancedOptions v-model="form" />

    <!-- OCR 路由选择器负责展示和校验路由相关字段。 -->
    <OcrRoutingSelector ref="ocrRoutingSelector" @change="updateOcrRouting" />

    <!-- 底部动作区由父组件 loading 控制，避免重复提交。 -->
    <div class="upload-dropzone__actions">
      <NButton quaternary :disabled="loading" @click="resetForm">
        清空
      </NButton>
      <NButton type="primary" :loading="loading" :disabled="!hasFiles" @click="submitUpload">
        上传并解析
      </NButton>
    </div>
  </section>
</template>

<style scoped>
/* 父组件只保留整体纵向布局，具体区块样式下沉到子组件。 */
.upload-dropzone {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 16px;
}

.upload-dropzone__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
