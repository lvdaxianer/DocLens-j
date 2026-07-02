import type { OcrGovernanceConfigPayload, OcrGovernanceConfigResponse } from '@/types/ocrGovernanceConfig'
import { apiErrorMessageFromBody } from '@/api/apiError'
import { withCallerCredentialHeaders } from '@/api/callerCredential'
import { logDashboardDebug, logDashboardWarn } from '@/utils/dashboardLogger'

const OCR_GOVERNANCE_CONFIG_PATH = '/api/v1/ocr-governance-config'
const OCR_GOVERNANCE_CONFIG_BUSINESS = '[OCR 治理配置 API]'
const JSON_CONTENT_TYPE = 'application/json'
const NETWORK_ERROR_STATUS = 'NETWORK_ERROR'
const DEFAULT_ERROR_MESSAGE = '请求失败'

interface OcrGovernanceConfigRequestOptions {
  method: string
  body?: OcrGovernanceConfigPayload
}

interface OcrGovernanceConfigRequestContext {
  method: string
  path: string
  startedAt: number
  responseStatus: number | string
}

/**
 * 将未知异常转换为错误消息。
 *
 * @param error - 捕获到的异常
 * @returns 可展示错误消息
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
function toErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error)
}

/**
 * 创建 OCR 治理配置请求上下文。
 *
 * @param method - HTTP 方法
 * @returns 请求上下文
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
function createContext(method: string): OcrGovernanceConfigRequestContext {
  return {
    method,
    path: OCR_GOVERNANCE_CONFIG_PATH,
    startedAt: performance.now(),
    responseStatus: NETWORK_ERROR_STATUS
  }
}

/**
 * 记录 OCR 治理配置请求日志。
 *
 * @param context - 请求上下文
 * @returns 日志记录完成信号
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
function logRequest(context: OcrGovernanceConfigRequestContext): void {
  logDashboardDebug({
    business: OCR_GOVERNANCE_CONFIG_BUSINESS,
    stage: 'REQUEST',
    message: '发起 OCR 治理配置请求',
    context: { method: context.method, path: context.path }
  })
}

/**
 * 记录 OCR 治理配置响应日志。
 *
 * @param context - 请求上下文
 * @returns 日志记录完成信号
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
function logResponse(context: OcrGovernanceConfigRequestContext): void {
  logDashboardDebug({
    business: OCR_GOVERNANCE_CONFIG_BUSINESS,
    stage: 'RESPONSE',
    message: '收到 OCR 治理配置响应',
    context: {
      method: context.method,
      path: context.path,
      status: context.responseStatus,
      durationMs: Math.round(performance.now() - context.startedAt)
    }
  })
}

/**
 * 记录 OCR 治理配置失败日志。
 *
 * @param context - 请求上下文
 * @param error - 捕获到的异常
 * @returns 日志记录完成信号
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
function logRequestError(context: OcrGovernanceConfigRequestContext, error: unknown): void {
  logDashboardWarn({
    business: OCR_GOVERNANCE_CONFIG_BUSINESS,
    stage: 'ERROR',
    message: 'OCR 治理配置请求失败',
    context: {
      method: context.method,
      path: context.path,
      status: context.responseStatus,
      error: toErrorMessage(error)
    }
  })
}

/**
 * 解析 OCR 治理配置接口响应。
 *
 * @param context - 请求上下文
 * @param response - 原始响应
 * @returns OCR 治理配置响应
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
async function parseResponse<T>(context: OcrGovernanceConfigRequestContext, response: Response): Promise<T> {
  context.responseStatus = response.status
  const responseBody = await response.text()
  logResponse(context)
  if (!response.ok) {
    // 失败文案由后端稳定 code 决定分类，detail 只承载具体原因。
    // 治理配置错误响应复用共享 parser，避免与其他 API 的 code/detail 解析漂移。
    throw new Error(apiErrorMessageFromBody(response, responseBody, DEFAULT_ERROR_MESSAGE))
  } else {
    return JSON.parse(responseBody) as T
  }
}

/**
 * 请求 OCR 治理配置接口。
 *
 * @param options - 请求参数
 * @returns OCR 治理配置响应
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
async function requestOcrGovernanceConfig<T>(options: OcrGovernanceConfigRequestOptions): Promise<T> {
  const context = createContext(options.method)
  logRequest(context)
  try {
    const response = await fetch(OCR_GOVERNANCE_CONFIG_PATH, {
      method: options.method,
      headers: options.body
        ? withCallerCredentialHeaders({ 'Content-Type': JSON_CONTENT_TYPE })
        : withCallerCredentialHeaders({}),
      body: options.body ? JSON.stringify(options.body) : undefined
    })
    return await parseResponse<T>(context, response)
  } catch (error) {
    logRequestError(context, error)
    throw error
  }
}

/**
 * 获取 OCR 全局治理配置。
 *
 * @returns OCR 全局治理配置响应
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
export function fetchOcrGovernanceConfig(): Promise<OcrGovernanceConfigResponse> {
  return requestOcrGovernanceConfig<OcrGovernanceConfigResponse>({ method: 'GET' })
}

/**
 * 更新 OCR 全局治理配置。
 *
 * @param payload - OCR 全局治理配置载荷
 * @returns OCR 全局治理配置响应
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
export function updateOcrGovernanceConfig(
  payload: OcrGovernanceConfigPayload
): Promise<OcrGovernanceConfigResponse> {
  return requestOcrGovernanceConfig<OcrGovernanceConfigResponse>({ method: 'PUT', body: payload })
}
