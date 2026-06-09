import type { LlmMarkdownConfigPayload, LlmMarkdownConfigResponse } from '@/types/llmMarkdownConfig'

export interface LlmMarkdownConfigFormState {
  url: string
  model: string
  apiKey: string
  credentialConfigured: boolean
}

export interface LlmConfigCapabilityHints {
  protocolHint: string
  endpointExample: string
  canTest: boolean
}

const HTTP_PROTOCOL = 'http:'
const HTTPS_PROTOCOL = 'https:'
const MASKED_SECRET = '***'
const OPENAI_COMPATIBLE_PROTOCOL_HINT = '仅支持 OpenAI compatible Chat Completions 格式'
const OPENAI_COMPATIBLE_ENDPOINT_EXAMPLE = 'https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions'
const API_KEY_VALUE_PATTERN = /(api_key\s*[:=]\s*["']?)([^"',\s]+)/gi
const BEARER_VALUE_PATTERN = /(Bearer\s+)([A-Za-z0-9._~+/=-]+)/gi

/**
 * 创建 LLM Markdown 配置表单默认值。
 *
 * @returns LLM Markdown 配置表单状态
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function createDefaultLlmMarkdownConfigForm(): LlmMarkdownConfigFormState {
  return {
    url: '',
    model: '',
    apiKey: '',
    credentialConfigured: false
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
    url: response.url,
    model: response.model,
    apiKey: '',
    credentialConfigured: response.credential_configured
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
    url: form.url.trim(),
    model: form.model.trim()
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
    protocolHint: OPENAI_COMPATIBLE_PROTOCOL_HINT,
    endpointExample: OPENAI_COMPATIBLE_ENDPOINT_EXAMPLE,
    canTest: hasUrl && hasModel && isHttpUrl(form.url)
  }
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
