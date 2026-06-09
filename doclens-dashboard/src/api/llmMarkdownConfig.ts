import type { LlmMarkdownConfigPayload, LlmMarkdownConfigResponse } from '@/types/llmMarkdownConfig'
import { logDashboardDebug, logDashboardWarn } from '@/utils/dashboardLogger'

const LLM_CONFIG_PATH = '/api/v1/llm-markdown-config'
const LLM_CONFIG_BUSINESS = '[LLM Markdown 配置 API]'
const JSON_CONTENT_TYPE = 'application/json'
const NETWORK_ERROR_STATUS = 'NETWORK_ERROR'
const DEFAULT_ERROR_MESSAGE = '请求失败'

interface LlmConfigRequestOptions {
  method: string
  body?: LlmMarkdownConfigPayload
}

interface LlmConfigRequestContext {
  method: string
  startedAt: number
  responseStatus: number | string
}

/**
 * 将未知异常转换为错误消息。
 *
 * @param error - 捕获到的异常
 * @returns 可展示错误消息
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function toErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error)
}

/**
 * 创建 LLM 配置请求上下文。
 *
 * @param method - HTTP 方法
 * @returns 请求上下文
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function createContext(method: string): LlmConfigRequestContext {
  return {
    method,
    startedAt: performance.now(),
    responseStatus: NETWORK_ERROR_STATUS
  }
}

/**
 * 记录不含敏感字段的请求日志。
 *
 * @param context - 请求上下文
 * @returns 日志记录完成信号
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function logRequest(context: LlmConfigRequestContext): void {
  logDashboardDebug({
    business: LLM_CONFIG_BUSINESS,
    stage: 'REQUEST',
    message: '发起 LLM Markdown 配置请求',
    context: { method: context.method, path: LLM_CONFIG_PATH }
  })
}

/**
 * 记录不含响应体的响应日志。
 *
 * @param context - 请求上下文
 * @returns 日志记录完成信号
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function logResponse(context: LlmConfigRequestContext): void {
  logDashboardDebug({
    business: LLM_CONFIG_BUSINESS,
    stage: 'RESPONSE',
    message: '收到 LLM Markdown 配置响应',
    context: {
      method: context.method,
      path: LLM_CONFIG_PATH,
      status: context.responseStatus,
      durationMs: Math.round(performance.now() - context.startedAt)
    }
  })
}

/**
 * 记录不含请求体和响应体的失败日志。
 *
 * @param context - 请求上下文
 * @param error - 捕获到的异常
 * @returns 日志记录完成信号
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function logRequestError(context: LlmConfigRequestContext, error: unknown): void {
  logDashboardWarn({
    business: LLM_CONFIG_BUSINESS,
    stage: 'ERROR',
    message: 'LLM Markdown 配置请求失败',
    context: {
      method: context.method,
      path: LLM_CONFIG_PATH,
      status: context.responseStatus,
      error: toErrorMessage(error)
    }
  })
}

/**
 * 执行 LLM 配置 fetch 请求。
 *
 * @param options - 请求参数
 * @returns 原始响应对象
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function fetchLlmConfig(options: LlmConfigRequestOptions): Promise<Response> {
  const headers = options.body ? { 'Content-Type': JSON_CONTENT_TYPE } : undefined
  return fetch(LLM_CONFIG_PATH, {
    method: options.method,
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined
  })
}

/**
 * 解析 LLM 配置接口响应。
 *
 * @param context - 请求上下文
 * @param response - 原始响应
 * @returns LLM 配置响应
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
async function parseResponse(context: LlmConfigRequestContext, response: Response): Promise<LlmMarkdownConfigResponse> {
  context.responseStatus = response.status
  const responseBody = await response.text()
  logResponse(context)
  if (!response.ok) {
    throw new Error(errorMessageFromResponse(response, responseBody))
  } else {
    return JSON.parse(responseBody) as LlmMarkdownConfigResponse
  }
}

/**
 * 从错误响应中提取可展示消息。
 *
 * @param response - 原始响应
 * @param responseBody - 响应体文本
 * @returns 可展示错误消息
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function errorMessageFromResponse(response: Response, responseBody: string): string {
  try {
    const parsed = JSON.parse(responseBody) as { detail?: string }
    return parsed.detail ?? `${DEFAULT_ERROR_MESSAGE}：${response.status} ${response.statusText}`
  } catch {
    return `${DEFAULT_ERROR_MESSAGE}：${response.status} ${response.statusText}`
  }
}

/**
 * 请求 LLM Markdown 配置接口。
 *
 * @param options - 请求参数
 * @returns LLM Markdown 配置响应
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
async function requestLlmConfig(options: LlmConfigRequestOptions): Promise<LlmMarkdownConfigResponse> {
  const context = createContext(options.method)
  logRequest(context)
  try {
    return await parseResponse(context, await fetchLlmConfig(options))
  } catch (error) {
    logRequestError(context, error)
    throw error
  }
}

/**
 * 获取 LLM Markdown 后处理配置。
 *
 * @returns LLM Markdown 配置响应
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function fetchLlmMarkdownConfig(): Promise<LlmMarkdownConfigResponse> {
  return requestLlmConfig({ method: 'GET' })
}

/**
 * 更新 LLM Markdown 后处理配置。
 *
 * @param payload - LLM Markdown 配置提交载荷
 * @returns LLM Markdown 配置响应
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function updateLlmMarkdownConfig(payload: LlmMarkdownConfigPayload): Promise<LlmMarkdownConfigResponse> {
  return requestLlmConfig({ method: 'PUT', body: payload })
}
