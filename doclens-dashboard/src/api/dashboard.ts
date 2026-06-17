import type {
  BatchDetailResponse,
  BatchListResponse,
  DashboardSummary,
  DocumentResultResponse,
  OcrHealthResponse
} from '@/types/dashboard'
import { withCallerCredentialHeaders } from '@/api/callerCredential'
import { logDashboardDebug, logDashboardWarn } from '@/utils/dashboardLogger'

const DASHBOARD_API_BUSINESS = '[Dashboard API]'
const JSON_ACCEPT_HEADER = 'application/json'
const HTTP_GET_METHOD = 'GET'
const HTTP_POST_METHOD = 'POST'
const HTTP_DELETE_METHOD = 'DELETE'
const NETWORK_ERROR_STATUS = 'NETWORK_ERROR'

interface DashboardRequestContext {
  method: string
  path: string
  startedAt: number
  responseStatus: number | string
  responseBody: string
}

/**
 * 将未知异常转换为可读错误消息。
 *
 * @param error - 捕获到的异常对象
 * @returns 可展示和记录的错误消息
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
function toErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error)
}

/**
 * 创建 Dashboard 请求上下文。
 *
 * @param path - 接口路径
 * @returns 请求上下文
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
function createRequestContext(path: string, method: string): DashboardRequestContext {
  return {
    method,
    path,
    startedAt: performance.now(),
    responseStatus: NETWORK_ERROR_STATUS,
    responseBody: ''
  }
}

/**
 * 记录 Dashboard 请求日志。
 *
 * @param context - 请求上下文
 * @returns 日志记录完成信号
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
function logRequest(context: DashboardRequestContext): void {
  logDashboardDebug({
    business: DASHBOARD_API_BUSINESS,
    stage: 'REQUEST',
    message: '发起 Dashboard 接口请求',
    context: { method: context.method, path: context.path, headers: { Accept: JSON_ACCEPT_HEADER } }
  })
}

/**
 * 执行 Dashboard fetch 请求。
 *
 * @param context - 请求上下文
 * @returns 原始响应对象
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
function fetchDashboardJson(context: DashboardRequestContext): Promise<Response> {
  return fetch(context.path, {
    method: context.method,
    headers: withCallerCredentialHeaders({
      Accept: JSON_ACCEPT_HEADER
    })
  })
}

/**
 * 记录 Dashboard 响应日志。
 *
 * @param context - 请求上下文
 * @returns 日志记录完成信号
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
function logResponse(context: DashboardRequestContext): void {
  logDashboardDebug({
    business: DASHBOARD_API_BUSINESS,
    stage: 'RESPONSE',
    message: '收到 Dashboard 接口响应',
    context: { method: context.method, path: context.path, status: context.responseStatus, durationMs: Math.round(performance.now() - context.startedAt), body: context.responseBody }
  })
}

/**
 * 解析 Dashboard 响应体。
 *
 * @param context - 请求上下文
 * @param response - 原始响应对象
 * @returns 解析后的 JSON 响应体
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
async function parseResponse<T>(context: DashboardRequestContext, response: Response): Promise<T> {
  context.responseStatus = response.status
  context.responseBody = await response.text()
  logResponse(context)
  if (!response.ok) {
    // 响应状态异常时抛出错误，并由 catch 统一记录 WARN 日志。
    throw new Error(`请求失败：${response.status} ${response.statusText}`)
  } else {
    // 响应状态正常时继续解析 JSON 响应体。
    return JSON.parse(context.responseBody) as T
  }
}

/**
 * 记录 Dashboard 请求失败日志。
 *
 * @param context - 请求上下文
 * @param error - 捕获到的异常对象
 * @returns 日志记录完成信号
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
function logRequestError(context: DashboardRequestContext, error: unknown): void {
  logDashboardWarn({
    business: DASHBOARD_API_BUSINESS,
    stage: 'ERROR',
    message: 'Dashboard 接口请求失败',
    context: { method: context.method, path: context.path, status: context.responseStatus, body: context.responseBody, error: toErrorMessage(error) }
  })
}

/**
 * 请求 Dashboard JSON 接口，并记录请求、响应与失败日志。
 *
 * @param path - 接口路径
 * @returns 解析后的 JSON 响应体
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
async function requestJson<T>(path: string, method = HTTP_GET_METHOD): Promise<T> {
  const context = createRequestContext(path, method)
  logRequest(context)
  try {
    return await parseResponse<T>(context, await fetchDashboardJson(context))
  } catch (error) {
    logRequestError(context, error)
    throw error
  }
}

/**
 * 获取 Dashboard 汇总指标。
 *
 * @returns Dashboard 汇总响应
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
export function fetchDashboardSummary(): Promise<DashboardSummary> {
  return requestJson<DashboardSummary>('/api/v1/dashboard/summary')
}

/**
 * 获取批次列表。
 *
 * @returns 批次列表响应
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
export function fetchBatchList(): Promise<BatchListResponse> {
  return requestJson<BatchListResponse>('/api/v1/dashboard/batches')
}

/**
 * 获取批次详情。
 *
 * @param batchId - 批次 ID
 * @returns 批次详情响应
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
export function fetchBatchDetail(batchId: string): Promise<BatchDetailResponse> {
  return requestJson<BatchDetailResponse>(`/api/v1/dashboard/batches/${encodeURIComponent(batchId)}`)
}

/**
 * 获取单个文档的解析结果。
 *
 * @param documentId - 文档 ID
 * @returns 文档解析结果响应
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
export function fetchDocumentResult(documentId: string): Promise<DocumentResultResponse> {
  return requestJson<DocumentResultResponse>(`/api/v1/documents/${encodeURIComponent(documentId)}/result`)
}

/**
 * 获取 OCR 服务健康状态。
 *
 * @returns OCR 健康检查响应
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
export function fetchOcrHealth(): Promise<OcrHealthResponse> {
  return requestJson<OcrHealthResponse>('/api/v1/dashboard/ocr-health')
}

/**
 * 重试单个失败或卡死文档。
 *
 * @param documentId - 文档 ID
 * @returns 重试结果
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
export function retryDocument(documentId: string): Promise<{ document_id: string; status: string }> {
  return requestJson<{ document_id: string; status: string }>(
    `/api/v1/documents/${encodeURIComponent(documentId)}/retry`,
    HTTP_POST_METHOD
  )
}

/**
 * 立即重试单个失败回调任务。
 *
 * @param callbackJobId - 回调任务 ID
 * @returns 回调重试结果
 * @author lvdaxianerplus
 * @date 2026-06-16
 */
export function retryCallbackJob(callbackJobId: string): Promise<{ callback_job_id: string; delivered_count: number }> {
  return requestJson<{ callback_job_id: string; delivered_count: number }>(
    `/api/v1/dashboard/callback-jobs/${encodeURIComponent(callbackJobId)}/retry`,
    HTTP_POST_METHOD
  )
}

/**
 * 删除单个已完成、失败或卡死文档。
 *
 * @param documentId - 文档 ID
 * @returns 删除结果
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
export function deleteDocument(documentId: string): Promise<{ document_id: string; status: string }> {
  return requestJson<{ document_id: string; status: string }>(
    `/api/v1/documents/${encodeURIComponent(documentId)}`,
    HTTP_DELETE_METHOD
  )
}

/**
 * 删除单个可删除批次。
 *
 * @param batchId - 批次 ID
 * @returns 批次删除结果
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
export function deleteBatch(batchId: string): Promise<{ batch_id: string; status: string; deleted_documents: number }> {
  return requestJson<{ batch_id: string; status: string; deleted_documents: number }>(
    `/api/v1/batches/${encodeURIComponent(batchId)}`,
    HTTP_DELETE_METHOD
  )
}
