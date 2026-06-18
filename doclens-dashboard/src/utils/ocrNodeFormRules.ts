import type {
  OcrNode,
  OcrNodeDeploymentType,
  OcrNodePayload,
  OcrNodeSubmitPayload,
  OcrOnlineChannelKey
} from '@/types/ocrResources'

export { createOcrNodeFormRules } from './ocrNodeFormValidationRules.ts'

const DEFAULT_PORT = 8080
const DEFAULT_WEIGHT = 50
const DEFAULT_MAX_CONCURRENCY = 10
const COMPATIBLE_ONLINE_MODEL_KEY = 'paddle_ocr'
const ONLINE_OCR_MODEL_KEY = 'online_ocr'
export const OLLAMA_MODEL_FAMILY_PREFIX = 'ollama'
export const OLLAMA_CHANNEL_KEY = 'ollama'
export const DASHSCOPE_CHANNEL_KEY: OcrOnlineChannelKey = 'aliyun_bailian_dashscope'

export interface OcrNodeFormState {
  modelKey: string
  deploymentType: OcrNodeDeploymentType
  name: string
  host: string
  port: number
  channelKey: OcrOnlineChannelKey
  providerModel: string
  credentialEnvVar: string
  credentialConfigured: boolean
  enabled: boolean
  participateGlobal: boolean
  weight: number
  maxConcurrency: number
}

/**
 * 创建 OCR 节点表单默认值。
 *
 * @param modelKey - 默认 OCR 模型标识
 * @returns OCR 节点表单状态
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function createDefaultOcrNodeForm(modelKey: string): OcrNodeFormState {
  return {
    modelKey,
    deploymentType: 'OFFLINE',
    name: '',
    host: '',
    port: DEFAULT_PORT,
    channelKey: DASHSCOPE_CHANNEL_KEY,
    providerModel: '',
    credentialEnvVar: '',
    credentialConfigured: false,
    enabled: true,
    participateGlobal: true,
    weight: DEFAULT_WEIGHT,
    maxConcurrency: DEFAULT_MAX_CONCURRENCY
  }
}

/**
 * 将已有节点转换为编辑表单状态。
 *
 * @param node - OCR 节点
 * @returns OCR 节点表单状态
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function fillOcrNodeFormFromNode(node: OcrNode): OcrNodeFormState {
  return {
    modelKey: node.model_key,
    deploymentType: node.deployment_type,
    name: node.name,
    host: node.host,
    port: node.port || DEFAULT_PORT,
    channelKey: onlineChannel(node.channel_key),
    providerModel: node.provider_model,
    credentialEnvVar: node.credential_env_var ?? '',
    credentialConfigured: node.credential_configured,
    enabled: node.enabled,
    participateGlobal: node.participate_global,
    weight: node.weight,
    maxConcurrency: node.max_concurrency
  }
}

/**
 * 判断 OCR 节点表单是否可提交。
 *
 * @param form - OCR 节点表单状态
 * @returns 是否可提交
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function isOcrNodeFormSubmittable(form: OcrNodeFormState): boolean {
  if (!hasBaseFields(form)) {
    return false
  } else if (form.deploymentType === 'OFFLINE') {
    return hasOfflineFields(form)
  } else {
    return hasOnlineFields(form)
  }
}

/**
 * 判断节点表单是否需要展示 OCR 模型选择。
 *
 * @param deploymentType - 节点部署类型
 * @returns 是否展示 OCR 模型选择
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function shouldShowOcrModelSelect(deploymentType: OcrNodeDeploymentType): boolean {
  return deploymentType === 'OFFLINE'
}

/**
 * 校验在线 OCR API Key 环境变量名。
 *
 * @param value - 环境变量名
 * @param modelKey - OCR 模型标识
 * @returns 校验错误消息，空字符串表示通过
 * @author lvdaxianerplus
 * @date 2026-06-13
 */
export function validateOcrNodeCredentialEnvVar(value: string, modelKey: string): string {
  if (!isOnlineOcrModel(modelKey)) {
    return ''
  } else if (!value.trim()) {
    return '请输入 API Key 环境变量名'
  } else if (!/^[A-Z_][A-Z0-9_]*$/.test(value.trim())) {
    return '环境变量名只能包含大写字母、数字和下划线，且不能以数字开头'
  } else {
    return ''
  }
}

/**
 * 创建 OCR 节点提交载荷。
 *
 * @param form - OCR 节点表单状态
 * @returns OCR 节点提交载荷
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function createOcrNodePayload(form: OcrNodeFormState): OcrNodeSubmitPayload {
  const node = form.deploymentType === 'OFFLINE' ? offlinePayload(form) : onlinePayload(form)
  return { modelKey: effectiveModelKey(form), node }
}

/**
 * 判断基础字段是否完整。
 *
 * @param form - OCR 节点表单状态
 * @returns 是否完整
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function hasBaseFields(form: OcrNodeFormState): boolean {
  if (form.deploymentType === 'OFFLINE') {
    return form.modelKey.trim() !== '' && form.name.trim() !== ''
  } else {
    return form.name.trim() !== ''
  }
}

/**
 * 获取提交给后端路由的 OCR 模型标识。
 *
 * @param form - OCR 节点表单状态
 * @returns OCR 模型标识
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function effectiveModelKey(form: OcrNodeFormState): string {
  if (form.deploymentType === 'OFFLINE') {
    return form.modelKey
  } else {
    return form.modelKey.trim() || COMPATIBLE_ONLINE_MODEL_KEY
  }
}

/**
 * 判断在线节点字段是否完整。
 *
 * @param form - OCR 节点表单状态
 * @returns 是否完整
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function hasOnlineFields(form: OcrNodeFormState): boolean {
  return form.channelKey.trim() !== ''
    && form.providerModel.trim() !== ''
    && validateOcrNodeCredentialEnvVar(form.credentialEnvVar, 'online_ocr') === ''
}

/**
 * 判断离线节点字段是否完整。
 *
 * @param form - OCR 节点表单状态
 * @returns 是否完整
 * @author lvdaxianerplus
 * @date 2026-06-13
 */
function hasOfflineFields(form: OcrNodeFormState): boolean {
  if (isOllamaModel(form.modelKey)) {
    return form.host.trim() !== '' && form.providerModel.trim() !== ''
  } else {
    return form.host.trim() !== ''
  }
}

/**
 * 创建离线节点载荷。
 *
 * @param form - OCR 节点表单状态
 * @returns OCR 节点载荷
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function offlinePayload(form: OcrNodeFormState): OcrNodePayload {
  const node = withScheduling(form, {
    deployment_type: 'OFFLINE',
    name: form.name.trim(),
    host: form.host.trim(),
    port: form.port
  })
  if (isOllamaModel(form.modelKey)) {
    // Ollama 统一走 /api/generate，真实 OCR 模型名由用户输入。
    node.channel_key = OLLAMA_CHANNEL_KEY
    node.provider_model = form.providerModel.trim()
  } else {
    // PaddleOCR 离线节点只需要主机和端口。
  }
  return node
}

/**
 * 创建在线节点载荷。
 *
 * @param form - OCR 节点表单状态
 * @returns OCR 节点载荷
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function onlinePayload(form: OcrNodeFormState): OcrNodePayload {
  return withScheduling(form, {
    deployment_type: 'ONLINE',
    name: form.name.trim(),
    channel_key: form.channelKey,
    provider_model: form.providerModel.trim(),
    credential_env_var: form.credentialEnvVar.trim()
  })
}

/**
 * 合并调度字段。
 *
 * @param form - OCR 节点表单状态
 * @param node - 节点载荷
 * @returns OCR 节点载荷
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function withScheduling(form: OcrNodeFormState, node: Omit<OcrNodePayload, 'enabled' | 'participate_global' | 'weight' | 'max_concurrency'>): OcrNodePayload {
  return {
    ...node,
    enabled: form.enabled,
    participate_global: form.participateGlobal,
    weight: form.weight,
    max_concurrency: form.maxConcurrency
  }
}

/**
 * 标准化在线渠道。
 *
 * @param channelKey - 接口返回渠道标识
 * @returns 在线渠道标识
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function onlineChannel(channelKey: string): OcrOnlineChannelKey {
  return channelKey === DASHSCOPE_CHANNEL_KEY ? DASHSCOPE_CHANNEL_KEY : DASHSCOPE_CHANNEL_KEY
}

/**
 * 判断是否为 Ollama OCR 类型。
 *
 * @param modelKey - OCR 模型标识
 * @returns 是否为 Ollama
 * @author lvdaxianerplus
 * @date 2026-06-13
 */
export function isOllamaModel(modelKey: string): boolean {
  return modelKey.trim().startsWith(OLLAMA_MODEL_FAMILY_PREFIX)
}

/**
 * 判断是否为在线 OCR 表单校验目标。
 *
 * @param modelKey - OCR 模型标识
 * @returns 是否需要在线 OCR 凭证校验
 * @author lvdaxianerplus
 * @date 2026-06-13
 */
function isOnlineOcrModel(modelKey: string): boolean {
  return modelKey.trim() === ONLINE_OCR_MODEL_KEY
}
