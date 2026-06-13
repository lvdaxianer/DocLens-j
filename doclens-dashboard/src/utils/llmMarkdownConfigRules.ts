import type { LlmMarkdownApiType, LlmMarkdownConfigPayload, LlmMarkdownConfigResponse } from '@/types/llmMarkdownConfig'
import type { FormRules } from 'naive-ui'

export interface LlmMarkdownConfigFormState {
  id: string
  name: string
  apiType: LlmMarkdownApiType
  url: string
  model: string
  credentialEnvVar: string
  credentialConfigured: boolean
  usageType: string
  priority: number
  maxContextTokens: number | undefined
  maxConcurrency: number | undefined
  requestIntervalMillis: number | undefined
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
  credentialEnvVar: string
  credentialConfigured: boolean
  usageType: string
  priority: number
  maxContextTokens: number
  maxConcurrency: number
  requestIntervalMillis: number
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
const DEFAULT_MAX_CONTEXT_TOKENS = 16000
const DEFAULT_MAX_CONCURRENCY = 1
const DEFAULT_REQUEST_INTERVAL_MILLIS = 1000
const MIN_MAX_CONTEXT_TOKENS = 1000
const MIN_MAX_CONCURRENCY = 1
const MIN_REQUEST_INTERVAL_MILLIS = 0
const EMPTY_CONFIG_STATUS = '暂无 LLM 配置，OCR 可正常运行。'
const ENV_VAR_NAME_PATTERN = /^[A-Z_][A-Z0-9_]*$/

/**
 * 创建 LLM Markdown 配置表单校验规则。
 *
 * @param form - LLM Markdown 配置表单状态
 * @returns Naive UI 表单校验规则
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
export function createLlmMarkdownConfigFormRules(form: LlmMarkdownConfigFormState): FormRules {
  return {
    url: [{
      validator: () => validateLlmUrl(form),
      message: '填写 LLM 配置时必须提供有效 HTTP URL',
      trigger: ['input', 'blur']
    }],
    model: [{
      validator: () => validateLlmModel(form),
      message: '填写 LLM URL 后必须填写模型名称',
      trigger: ['input', 'blur']
    }]
  }
}

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
    credentialEnvVar: '',
    credentialConfigured: false,
    maxContextTokens: DEFAULT_MAX_CONTEXT_TOKENS,
    maxConcurrency: DEFAULT_MAX_CONCURRENCY,
    requestIntervalMillis: DEFAULT_REQUEST_INTERVAL_MILLIS,
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
    credentialEnvVar: response.credential_env_var ?? '',
    credentialConfigured: response.credential_configured,
    usageType: response.usage_type ?? DEFAULT_USAGE_TYPE,
    priority: response.priority ?? DEFAULT_PRIORITY,
    maxContextTokens: response.max_context_tokens ?? DEFAULT_MAX_CONTEXT_TOKENS,
    maxConcurrency: response.max_concurrency ?? DEFAULT_MAX_CONCURRENCY,
    requestIntervalMillis: response.request_interval_millis ?? DEFAULT_REQUEST_INTERVAL_MILLIS,
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
  const hasValidCredential = validateCredentialEnvVar(form.credentialEnvVar) === ''
  const hasValidMaxTokens = validateMaxContextTokens(form.maxContextTokens) === ''
  const hasValidConcurrency = validateMaxConcurrency(form.maxConcurrency) === ''
  const hasValidInterval = validateRequestIntervalMillis(form.requestIntervalMillis) === ''
  if (!hasUrl && !hasModel) {
    // URL 和模型都为空时表示关闭可选后处理。
    return true
  } else {
    // 只要填写任一核心字段，就必须形成可调用的完整配置。
    return hasUrl && hasModel && isHttpUrl(form.url) && hasValidCredential && hasValidMaxTokens
      && hasValidConcurrency && hasValidInterval
  }
}

/**
 * 校验凭证环境变量名。
 *
 * @param value - 环境变量名
 * @returns 错误文案，空字符串表示通过
 * @author lvdaxianerplus
 * @date 2026-06-13
 */
export function validateCredentialEnvVar(value: string | undefined): string {
  const trimmed = value?.trim() ?? ''
  if (trimmed === '') {
    // 没有密钥的本地或测试配置允许留空，由后端测试阶段判断是否需要真实凭证。
    return ''
  } else if (ENV_VAR_NAME_PATTERN.test(trimmed)) {
    // 合法环境变量名可以提交。
    return ''
  } else {
    // 环境变量名不允许使用横线、小写字母或数字开头。
    return '环境变量名只能包含大写字母、数字和下划线，且不能以数字开头'
  }
}

/**
 * 校验最大上下文 Token 数。
 *
 * @param value - 最大上下文 Token 数
 * @returns 错误文案，空字符串表示通过
 * @author lvdaxianerplus
 * @date 2026-06-13
 */
export function validateMaxContextTokens(value: number | undefined): string {
  if (value === undefined || value === null) {
    // Token 预算是必填项，否则无法判断是否需要分片。
    return '请输入最大上下文 Token 数'
  } else if (value >= MIN_MAX_CONTEXT_TOKENS) {
    // 达到后端最小上下文要求时允许提交。
    return ''
  } else {
    // 过小上下文会导致分片不可用。
    return '最大上下文 Token 数必须大于等于 1000'
  }
}

/**
 * 校验最大并发数。
 *
 * @param value - 最大并发数
 * @returns 错误文案，空字符串表示通过
 * @author lvdaxianerplus
 * @date 2026-06-13
 */
export function validateMaxConcurrency(value: number | undefined): string {
  if (value === undefined || value === null || value < MIN_MAX_CONCURRENCY) {
    // 并发数至少为 1，避免配置无法获得任何执行槽位。
    return '最大并发数必须大于等于 1'
  } else {
    // 有效并发数允许提交。
    return ''
  }
}

/**
 * 校验请求间隔毫秒数。
 *
 * @param value - 请求间隔毫秒数
 * @returns 错误文案，空字符串表示通过
 * @author lvdaxianerplus
 * @date 2026-06-13
 */
export function validateRequestIntervalMillis(value: number | undefined): string {
  if (value === undefined || value === null || value < MIN_REQUEST_INTERVAL_MILLIS) {
    // 间隔不能为负数，0 表示不额外等待。
    return '请求间隔不能小于 0'
  } else {
    // 非负间隔允许提交。
    return ''
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
    is_default: form.defaultConfig,
    credential_env_var: form.credentialEnvVar.trim(),
    max_context_tokens: form.maxContextTokens ?? DEFAULT_MAX_CONTEXT_TOKENS,
    max_concurrency: form.maxConcurrency ?? DEFAULT_MAX_CONCURRENCY,
    request_interval_millis: form.requestIntervalMillis ?? DEFAULT_REQUEST_INTERVAL_MILLIS
  }
  const trimmedName = trimmedValueOrUndefined(form.name)
  if (trimmedName) {
    // 非空名称才提交，空名称交由后端使用默认命名。
    payload.name = trimmedName
  } else {
    // 空名称不提交，避免出现 name: undefined。
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
    credentialEnvVar: '',
    credentialConfigured: false,
    usageType: DEFAULT_USAGE_TYPE,
    priority: DEFAULT_PRIORITY,
    maxContextTokens: DEFAULT_MAX_CONTEXT_TOKENS,
    maxConcurrency: DEFAULT_MAX_CONCURRENCY,
    requestIntervalMillis: DEFAULT_REQUEST_INTERVAL_MILLIS,
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
    credentialEnvVar: response.credential_env_var ?? '',
    credentialConfigured: response.credential_configured,
    usageType: response.usage_type ?? DEFAULT_USAGE_TYPE,
    priority: response.priority ?? DEFAULT_PRIORITY,
    maxContextTokens: response.max_context_tokens ?? DEFAULT_MAX_CONTEXT_TOKENS,
    maxConcurrency: response.max_concurrency ?? DEFAULT_MAX_CONCURRENCY,
    requestIntervalMillis: response.request_interval_millis ?? DEFAULT_REQUEST_INTERVAL_MILLIS,
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

/**
 * 校验 LLM URL 字段。
 *
 * @param form - LLM Markdown 配置表单状态
 * @returns 是否通过
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
function validateLlmUrl(form: LlmMarkdownConfigFormState): boolean {
  if (!form.url.trim() && !form.model.trim()) {
    // 两个核心字段都为空表示关闭 LLM 后处理。
    return true
  } else {
    // 只要进入配置状态，URL 必须是可调用 HTTP 地址。
    return form.url.trim() !== '' && isHttpUrl(form.url)
  }
}

/**
 * 校验 LLM 模型字段。
 *
 * @param form - LLM Markdown 配置表单状态
 * @returns 是否通过
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
function validateLlmModel(form: LlmMarkdownConfigFormState): boolean {
  if (!form.url.trim() && !form.model.trim()) {
    // 两个核心字段都为空表示关闭 LLM 后处理。
    return true
  } else {
    // 只要填写 URL 或模型任一项，就必须补齐模型名称。
    return form.model.trim() !== ''
  }
}
