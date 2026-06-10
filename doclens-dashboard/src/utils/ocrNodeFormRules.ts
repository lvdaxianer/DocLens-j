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
export const DASHSCOPE_CHANNEL_KEY: OcrOnlineChannelKey = 'aliyun_bailian_dashscope'

export interface OcrNodeFormState {
  modelKey: string
  deploymentType: OcrNodeDeploymentType
  name: string
  host: string
  port: number
  channelKey: OcrOnlineChannelKey
  providerModel: string
  apiKey: string
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
    apiKey: '',
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
    apiKey: '',
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
    return form.host.trim() !== ''
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
    && (form.credentialConfigured || form.apiKey.trim() !== '')
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
  return withScheduling(form, {
    deployment_type: 'OFFLINE',
    name: form.name.trim(),
    host: form.host.trim(),
    port: form.port
  })
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
  const node = withScheduling(form, {
    deployment_type: 'ONLINE',
    name: form.name.trim(),
    channel_key: form.channelKey,
    provider_model: form.providerModel.trim()
  })
  if (form.apiKey.trim()) {
    node.api_key = form.apiKey.trim()
  } else {
    // 编辑在线节点且未重新填写 API Key 时，后端沿用已保存密钥。
  }
  return node
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
