import type {
  OcrModelListResponse,
  OcrNode,
  OcrNodeListResponse,
  OcrNodePayload,
  OcrNodeTestResponse
} from '@/types/ocrResources'
import { logDashboardDebug, logDashboardWarn } from '@/utils/dashboardLogger'

const OCR_API_BUSINESS = '[OCR 资源 API]'
const JSON_CONTENT_TYPE = 'application/json'
const NETWORK_ERROR_STATUS = 'NETWORK_ERROR'

interface OcrRequestOptions {
  method: string
  body?: unknown
}

interface OcrRequestContext {
  path: string
  method: string
  startedAt: number
  responseStatus: number | string
  responseBody: string
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
 * 创建 OCR 资源请求上下文。
 *
 * @param path - 接口路径
 * @param method - HTTP 方法
 * @returns 请求上下文
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function createContext(path: string, method: string): OcrRequestContext {
  return {
    path,
    method,
    startedAt: performance.now(),
    responseStatus: NETWORK_ERROR_STATUS,
    responseBody: ''
  }
}

/**
 * 记录 OCR 资源请求日志。
 *
 * @param context - 请求上下文
 * @returns 日志记录完成信号
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function logRequest(context: OcrRequestContext): void {
  logDashboardDebug({
    business: OCR_API_BUSINESS,
    stage: 'REQUEST',
    message: '发起 OCR 资源接口请求',
    context: { method: context.method, path: context.path }
  })
}

/**
 * 记录 OCR 资源响应日志。
 *
 * @param context - 请求上下文
 * @returns 日志记录完成信号
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function logResponse(context: OcrRequestContext): void {
  logDashboardDebug({
    business: OCR_API_BUSINESS,
    stage: 'RESPONSE',
    message: '收到 OCR 资源接口响应',
    context: {
      method: context.method,
      path: context.path,
      status: context.responseStatus,
      durationMs: Math.round(performance.now() - context.startedAt),
      body: context.responseBody
    }
  })
}

/**
 * 记录 OCR 资源请求失败日志。
 *
 * @param context - 请求上下文
 * @param error - 捕获到的异常
 * @returns 日志记录完成信号
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function logRequestError(context: OcrRequestContext, error: unknown): void {
  logDashboardWarn({
    business: OCR_API_BUSINESS,
    stage: 'ERROR',
    message: 'OCR 资源接口请求失败',
    context: {
      method: context.method,
      path: context.path,
      status: context.responseStatus,
      body: context.responseBody,
      error: toErrorMessage(error)
    }
  })
}

/**
 * 执行 OCR 资源 fetch 请求。
 *
 * @param context - 请求上下文
 * @param options - 请求参数
 * @returns 原始响应对象
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
function fetchOcrResource(context: OcrRequestContext, options: OcrRequestOptions): Promise<Response> {
  const headers = options.body ? { 'Content-Type': JSON_CONTENT_TYPE } : undefined
  return fetch(context.path, {
    method: options.method,
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined
  })
}

/**
 * 解析 OCR 资源响应体。
 *
 * @param context - 请求上下文
 * @param response - 原始响应
 * @returns JSON 解析结果
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
async function parseResponse<T>(context: OcrRequestContext, response: Response): Promise<T> {
  context.responseStatus = response.status
  context.responseBody = await response.text()
  logResponse(context)
  if (!response.ok) {
    throw new Error(`请求失败：${response.status} ${response.statusText}`)
  } else if (context.responseBody) {
    return JSON.parse(context.responseBody) as T
  } else {
    return undefined as T
  }
}

/**
 * 请求 OCR 资源 JSON 接口。
 *
 * @param path - 接口路径
 * @param options - 请求参数
 * @returns JSON 响应体
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
async function requestJson<T>(path: string, options: OcrRequestOptions): Promise<T> {
  const context = createContext(path, options.method)
  logRequest(context)
  try {
    return await parseResponse<T>(context, await fetchOcrResource(context, options))
  } catch (error) {
    logRequestError(context, error)
    throw error
  }
}

/**
 * 获取系统支持的 OCR 模型。
 *
 * @returns OCR 模型列表响应
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function fetchOcrModels(): Promise<OcrModelListResponse> {
  return requestJson<OcrModelListResponse>('/api/v1/ocr-models', { method: 'GET' })
}

/**
 * 获取指定模型下的 OCR 节点。
 *
 * @param modelKey - OCR 模型标识
 * @returns OCR 节点列表响应
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function fetchOcrNodes(modelKey: string): Promise<OcrNodeListResponse> {
  return requestJson<OcrNodeListResponse>(`/api/v1/ocr-models/${encodeURIComponent(modelKey)}/nodes`, {
    method: 'GET'
  })
}

/**
 * 创建 OCR 节点。
 *
 * @param modelKey - OCR 模型标识
 * @param payload - 节点配置
 * @returns 创建后的 OCR 节点
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function createOcrNode(modelKey: string, payload: OcrNodePayload): Promise<OcrNode> {
  return requestJson<OcrNode>(`/api/v1/ocr-models/${encodeURIComponent(modelKey)}/nodes`, {
    method: 'POST',
    body: payload
  })
}

/**
 * 更新 OCR 节点。
 *
 * @param nodeId - OCR 节点 ID
 * @param payload - 节点配置
 * @returns 更新后的 OCR 节点
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function updateOcrNode(nodeId: string, payload: OcrNodePayload): Promise<OcrNode> {
  return requestJson<OcrNode>(`/api/v1/ocr-nodes/${encodeURIComponent(nodeId)}`, {
    method: 'PUT',
    body: payload
  })
}

/**
 * 更新 OCR 节点启用状态。
 *
 * @param nodeId - OCR 节点 ID
 * @param enabled - 是否启用
 * @returns 更新后的 OCR 节点
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function updateOcrNodeEnabled(nodeId: string, enabled: boolean): Promise<OcrNode> {
  return requestJson<OcrNode>(`/api/v1/ocr-nodes/${encodeURIComponent(nodeId)}/enabled`, {
    method: 'PATCH',
    body: { enabled }
  })
}

/**
 * 删除 OCR 节点。
 *
 * @param nodeId - OCR 节点 ID
 * @returns 删除完成信号
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function deleteOcrNode(nodeId: string): Promise<void> {
  return requestJson<void>(`/api/v1/ocr-nodes/${encodeURIComponent(nodeId)}`, { method: 'DELETE' })
}

/**
 * 手动测试 OCR 节点健康。
 *
 * @param nodeId - OCR 节点 ID
 * @returns 节点测试响应
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
export function testOcrNode(nodeId: string): Promise<OcrNodeTestResponse> {
  return requestJson<OcrNodeTestResponse>(`/api/v1/ocr-nodes/${encodeURIComponent(nodeId)}/test`, {
    method: 'POST'
  })
}
