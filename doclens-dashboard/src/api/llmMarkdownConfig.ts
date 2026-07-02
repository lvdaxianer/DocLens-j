import type {
  LlmMarkdownConfigPayload,
  LlmMarkdownConfigResponse,
  LlmMarkdownConfigTestResponse
} from '@/types/llmMarkdownConfig'
import { apiErrorMessageFromBody } from '@/api/apiError'
import { withCallerCredentialHeaders } from '@/api/callerCredential'
import { logDashboardDebug, logDashboardWarn } from '@/utils/dashboardLogger'

const LLM_CONFIG_PATH = '/api/v1/llm-markdown-config'
const LLM_CONFIG_TEST_PATH = '/api/v1/llm-markdown-config/test'
const LLM_CONFIG_BUSINESS = '[LLM Markdown 配置 API]'
const JSON_CONTENT_TYPE = 'application/json'
const NETWORK_ERROR_STATUS = 'NETWORK_ERROR'
const DEFAULT_ERROR_MESSAGE = '请求失败'

interface LlmConfigRequestOptions {
  method: string
  path?: string
  body?: LlmMarkdownConfigPayload | LlmMarkdownConfigEnabledPayload
}

interface LlmConfigRequestContext {
  path: string
  method: string
  startedAt: number
  responseStatus: number | string
}

interface LlmMarkdownConfigEnabledPayload {
  enabled: boolean
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
 * @param path - 请求路径
 * @returns 请求上下文
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function createContext(method: string, path: string): LlmConfigRequestContext {
  return {
    path,
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
    context: { method: context.method, path: context.path }
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
      path: context.path,
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
      path: context.path,
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
  const headers = options.body
    ? withCallerCredentialHeaders({ 'Content-Type': JSON_CONTENT_TYPE })
    : withCallerCredentialHeaders({})
  return fetch(options.path ?? LLM_CONFIG_PATH, {
    method: options.method,
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined
  })
}

/**
 * 创建指定配置资源路径。
 *
 * @param id - 配置 ID
 * @returns 配置资源路径
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
function configPath(id: string): string {
  return `${LLM_CONFIG_PATH}/${encodeURIComponent(id)}`
}

/**
 * 创建指定配置动作路径。
 *
 * @param id - 配置 ID
 * @param action - 动作名称
 * @returns 配置动作路径
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
function configActionPath(id: string, action: string): string {
  return `${configPath(id)}/${action}`
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
async function parseResponse<T>(context: LlmConfigRequestContext, response: Response): Promise<T> {
  context.responseStatus = response.status
  const responseBody = await response.text()
  logResponse(context)
  if (!response.ok) {
    throw new Error(apiErrorMessageFromBody(response, responseBody, DEFAULT_ERROR_MESSAGE))
  } else if (!responseBody) {
    return undefined as T
  } else {
    return JSON.parse(responseBody) as T
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
async function requestLlmConfig<T>(options: LlmConfigRequestOptions): Promise<T> {
  const context = createContext(options.method, options.path ?? LLM_CONFIG_PATH)
  logRequest(context)
  try {
    return await parseResponse<T>(context, await fetchLlmConfig(options))
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
export function fetchLlmMarkdownConfig(): Promise<LlmMarkdownConfigResponse[]> {
  return requestLlmConfig<LlmMarkdownConfigResponse[]>({ method: 'GET' })
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
  return requestLlmConfig<LlmMarkdownConfigResponse>({ method: 'PUT', body: payload })
}

/**
 * 创建 LLM Markdown 后处理配置。
 *
 * @param payload - LLM Markdown 配置提交载荷
 * @returns LLM Markdown 配置响应
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
export function createLlmMarkdownConfig(payload: LlmMarkdownConfigPayload): Promise<LlmMarkdownConfigResponse> {
  return requestLlmConfig<LlmMarkdownConfigResponse>({ method: 'POST', body: payload })
}

/**
 * 更新指定 LLM Markdown 后处理配置。
 *
 * @param id - 配置 ID
 * @param payload - LLM Markdown 配置提交载荷
 * @returns LLM Markdown 配置响应
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
export function updateLlmMarkdownConfigById(
  id: string,
  payload: LlmMarkdownConfigPayload
): Promise<LlmMarkdownConfigResponse> {
  return requestLlmConfig<LlmMarkdownConfigResponse>({ method: 'PUT', path: configPath(id), body: payload })
}

/**
 * 更新 LLM Markdown 配置启停状态。
 *
 * @param id - 配置 ID
 * @param enabled - 是否启用
 * @returns LLM Markdown 配置响应
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
export function updateLlmMarkdownConfigEnabled(id: string, enabled: boolean): Promise<LlmMarkdownConfigResponse> {
  return requestLlmConfig<LlmMarkdownConfigResponse>({
    method: 'PATCH',
    path: configActionPath(id, 'enabled'),
    body: enabledPayload(enabled)
  })
}

/**
 * 设置默认 LLM Markdown 配置。
 *
 * @param id - 配置 ID
 * @returns LLM Markdown 配置响应
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
export function makeDefaultLlmMarkdownConfig(id: string): Promise<LlmMarkdownConfigResponse> {
  return requestLlmConfig<LlmMarkdownConfigResponse>({ method: 'PATCH', path: configActionPath(id, 'default') })
}

/**
 * 删除 LLM Markdown 配置。
 *
 * @param id - 配置 ID
 * @returns 删除完成信号
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
export function deleteLlmMarkdownConfig(id: string): Promise<void> {
  return requestLlmConfig<void>({ method: 'DELETE', path: configPath(id) })
}

/**
 * 测试当前 LLM Markdown 配置是否可连通。
 *
 * @param payload - LLM Markdown 配置测试载荷
 * @returns LLM 配置测试响应
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function testLlmMarkdownConfig(payload: LlmMarkdownConfigPayload): Promise<LlmMarkdownConfigTestResponse> {
  return requestLlmConfig<LlmMarkdownConfigTestResponse>({
    method: 'POST',
    path: LLM_CONFIG_TEST_PATH,
    body: payload
  })
}

/**
 * 创建启停请求载荷。
 *
 * @param enabled - 是否启用
 * @returns 启停请求载荷
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
function enabledPayload(enabled: boolean): LlmMarkdownConfigEnabledPayload {
  return { enabled }
}
