import type { LlmMarkdownApiType, LlmMarkdownConfigPayload, LlmMarkdownConfigResponse } from '@/types/llmMarkdownConfig'

export interface LlmMarkdownConfigFormState {
  id: string
  name: string
  apiType: LlmMarkdownApiType
  url: string
  model: string
  apiKey: string
  credentialConfigured: boolean
  usageType: string
  priority: number
  defaultConfig: boolean
  enabled: boolean
  healthy: boolean
  healthMessage: string
  lastHealthAt: string
}

export interface LlmMarkdownConfigRow {
  id: string
  name: string
  apiType: LlmMarkdownApiType
  url: string
  model: string
  credentialConfigured: boolean
  usageType: string
  priority: number
  defaultConfig: boolean
  enabled: boolean
  healthy: boolean
  healthMessage: string
  lastHealthAt: string
  statusLabel: string
}

export interface LlmConfigCapabilityHints {
  urlPlaceholder: string
  canTest: boolean
}

const HTTP_PROTOCOL = 'http:'
const HTTPS_PROTOCOL = 'https:'
const MASKED_SECRET = '***'
const URL_PLACEHOLDER = '请输入完整接口地址'
const API_KEY_VALUE_PATTERN = /(api_key\s*[:=]\s*["']?)([^"',\s]+)/gi
const BEARER_VALUE_PATTERN = /(Bearer\s+)([A-Za-z0-9._~+/=-]+)/gi
const DEFAULT_USAGE_TYPE = 'MARKDOWN_POST_PROCESSING'
const DEFAULT_PRIORITY = 100
const EMPTY_CONFIG_STATUS = '暂无 LLM 配置，OCR 可正常运行。'

/**
 * 创建 LLM Markdown 配置表单默认值。
 *
 * @returns LLM Markdown 配置表单状态
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function createDefaultLlmMarkdownConfigForm(): LlmMarkdownConfigFormState {
  return {
    id: '',
    name: '',
    apiType: 'openai',
    url: '',
    model: '',
    apiKey: '',
    credentialConfigured: false,
    usageType: DEFAULT_USAGE_TYPE,
    priority: DEFAULT_PRIORITY,
    defaultConfig: true,
    enabled: true,
    healthy: false,
    healthMessage: '',
    lastHealthAt: ''
  }
}

/**
 * 将接口响应转换为编辑表单状态。
 *
 * @param response - LLM Markdown 配置响应
 * @returns LLM Markdown 配置表单状态
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function fillLlmMarkdownConfigFormFromResponse(response: LlmMarkdownConfigResponse): LlmMarkdownConfigFormState {
  return {
    id: response.id ?? '',
    name: response.name ?? '',
    apiType: response.api_type ?? 'openai',
    url: response.url,
    model: response.model,
    apiKey: '',
    credentialConfigured: response.credential_configured,
    usageType: response.usage_type ?? DEFAULT_USAGE_TYPE,
    priority: response.priority ?? DEFAULT_PRIORITY,
    defaultConfig: response.is_default ?? false,
    enabled: response.enabled ?? true,
    healthy: response.healthy,
    healthMessage: response.health_message ?? '',
    lastHealthAt: response.last_health_at ?? ''
  }
}

/**
 * 判断 LLM Markdown 配置表单是否可提交。
 *
 * @param form - LLM Markdown 配置表单状态
 * @returns 是否可提交
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function isLlmMarkdownConfigFormSubmittable(form: LlmMarkdownConfigFormState): boolean {
  const hasUrl = form.url.trim() !== ''
  const hasModel = form.model.trim() !== ''
  if (!hasUrl && !hasModel) {
    // URL 和模型都为空时表示关闭可选后处理。
    return true
  } else {
    // 只要填写任一核心字段，就必须形成可调用的完整配置。
    return hasUrl && hasModel && isHttpUrl(form.url)
  }
}

/**
 * 创建 LLM Markdown 配置提交载荷。
 *
 * @param form - LLM Markdown 配置表单状态
 * @returns LLM Markdown 配置提交载荷
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function createLlmMarkdownConfigPayload(form: LlmMarkdownConfigFormState): LlmMarkdownConfigPayload {
  const payload: LlmMarkdownConfigPayload = {
    api_type: form.apiType,
    url: form.url.trim(),
    model: form.model.trim(),
    enabled: form.enabled,
    usage_type: form.usageType,
    priority: form.priority,
    is_default: form.defaultConfig
  }
  const trimmedName = trimmedValueOrUndefined(form.name)
  if (trimmedName) {
    // 非空名称才提交，空名称交由后端使用默认命名。
    payload.name = trimmedName
  } else {
    // 空名称不提交，避免出现 name: undefined。
  }
  if (form.apiKey.trim()) {
    // 非空 API Key 表示用户主动轮换密钥。
    payload.api_key = form.apiKey.trim()
  } else {
    // 编辑时密钥留空由后端沿用旧值，前端不提交空 api_key。
  }
  return payload
}

/**
 * 脱敏 LLM Markdown 配置错误消息中的密钥片段。
 *
 * @param message - 原始错误消息
 * @returns 脱敏后的错误消息
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function sanitizeLlmMarkdownConfigErrorMessage(message: string): string {
  return message
    .replace(API_KEY_VALUE_PATTERN, `$1${MASKED_SECRET}`)
    .replace(BEARER_VALUE_PATTERN, `$1${MASKED_SECRET}`)
}

/**
 * 返回 LLM 配置能力提示与测试按钮状态。
 *
 * @param form - LLM Markdown 配置表单状态
 * @returns 能力提示
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function llmConfigCapabilityHints(form: LlmMarkdownConfigFormState): LlmConfigCapabilityHints {
  const hasUrl = form.url.trim() !== ''
  const hasModel = form.model.trim() !== ''
  return {
    urlPlaceholder: URL_PLACEHOLDER,
    canTest: hasUrl && hasModel && isHttpUrl(form.url)
  }
}

/**
 * 创建 LLM Markdown 配置列表行。
 *
 * @param response - LLM Markdown 配置响应
 * @returns 配置列表行
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
export function createLlmMarkdownConfigRow(response: LlmMarkdownConfigResponse | undefined): LlmMarkdownConfigRow {
  if (!response) {
    // 空列表时展示非阻塞空状态，OCR 主流程仍可正常运行。
    return createEmptyConfigRow()
  } else {
    // 有配置时将接口字段收敛成列表展示模型。
    return createResponseConfigRow(response)
  }
}

/**
 * 切换指定配置行启停状态。
 *
 * @param rows - 配置行列表
 * @param id - 配置 ID
 * @param enabled - 启停状态
 * @returns 更新后的配置行列表
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
export function toggleLlmMarkdownConfigRowEnabled(
  rows: LlmMarkdownConfigRow[],
  id: string,
  enabled: boolean
): LlmMarkdownConfigRow[] {
  return rows.map((row) => row.id === id ? { ...row, enabled, statusLabel: statusLabel(enabled, row.healthy) } : row)
}

/**
 * 创建空配置行。
 *
 * @returns 空配置行
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
function createEmptyConfigRow(): LlmMarkdownConfigRow {
  return {
    id: '',
    name: '未命名配置',
    apiType: 'openai',
    url: '',
    model: '',
    credentialConfigured: false,
    usageType: DEFAULT_USAGE_TYPE,
    priority: DEFAULT_PRIORITY,
    defaultConfig: false,
    enabled: false,
    healthy: false,
    healthMessage: '',
    lastHealthAt: '',
    statusLabel: EMPTY_CONFIG_STATUS
  }
}

/**
 * 创建接口响应配置行。
 *
 * @param response - LLM Markdown 配置响应
 * @returns 配置行
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
function createResponseConfigRow(response: LlmMarkdownConfigResponse): LlmMarkdownConfigRow {
  return {
    id: response.id ?? '',
    name: response.name || '未命名配置',
    apiType: response.api_type,
    url: response.url,
    model: response.model,
    credentialConfigured: response.credential_configured,
    usageType: response.usage_type ?? DEFAULT_USAGE_TYPE,
    priority: response.priority ?? DEFAULT_PRIORITY,
    defaultConfig: response.is_default ?? false,
    enabled: response.enabled,
    healthy: response.healthy,
    healthMessage: response.health_message ?? '',
    lastHealthAt: response.last_health_at ?? '',
    statusLabel: statusLabel(response.enabled, response.healthy)
  }
}

/**
 * 生成配置状态文案。
 *
 * @param enabled - 是否启用
 * @param healthy - 是否健康
 * @returns 状态文案
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
function statusLabel(enabled: boolean, healthy: boolean): string {
  if (!enabled) {
    // 手动暂停优先于健康状态展示。
    return '已暂停'
  } else {
    // 启用时根据健康状态展示可用性。
    return healthy ? '可用' : '心跳不可用'
  }
}

/**
 * 返回去空后的字符串或 undefined。
 *
 * @param value - 原始字符串
 * @returns 去空字符串或 undefined
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
function trimmedValueOrUndefined(value: string): string | undefined {
  const trimmed = value.trim()
  return trimmed ? trimmed : undefined
}

/**
 * 判断字符串是否为可调用 HTTP URL。
 *
 * @param value - URL 字符串
 * @returns 是否为 HTTP 或 HTTPS URL
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function isHttpUrl(value: string): boolean {
  try {
    const url = new URL(value)
    return Boolean(url.host) && (url.protocol === HTTP_PROTOCOL || url.protocol === HTTPS_PROTOCOL)
  } catch {
    return false
  }
}
