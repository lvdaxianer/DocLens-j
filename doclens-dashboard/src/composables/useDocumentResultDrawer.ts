import { reactive, shallowRef, type ShallowRef } from 'vue'

import { fetchDocumentResult } from '@/api/dashboard'
import type { DocumentResultResponse, DocumentRow } from '@/types/dashboard'
import { logDashboardDebug, logDashboardWarn } from '@/utils/dashboardLogger'

const RESULT_LOG_BUSINESS = '[文档结果查看]'
const DEFAULT_RESULT_ERROR_MESSAGE = '解析内容加载失败'
const REQUEST_KEY_SEPARATOR = ':'

/**
 * 文档结果加载状态。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
interface DocumentResultState {
  loading: boolean
  error: string
}

/**
 * 文档结果抽屉上下文。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
interface DocumentResultDrawerContext {
  resultDrawerOpen: ShallowRef<boolean>
  selectedResultDocument: ShallowRef<DocumentRow | null>
  selectedDocumentResult: ShallowRef<DocumentResultResponse | null>
  activeResultRequestKey: ShallowRef<string>
  resultState: DocumentResultState
  resultRequestSequence: number
}

/**
 * 文档结果请求上下文。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
interface ResultRequestContext {
  drawerContext: DocumentResultDrawerContext
  documentId: string
  requestKey: string
}

/**
 * 创建文档结果抽屉上下文。
 *
 * @returns 文档结果抽屉上下文
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
function createDocumentResultDrawerContext(): DocumentResultDrawerContext {
  return {
    resultDrawerOpen: shallowRef(false),
    selectedResultDocument: shallowRef<DocumentRow | null>(null),
    selectedDocumentResult: shallowRef<DocumentResultResponse | null>(null),
    activeResultRequestKey: shallowRef(''),
    resultState: reactive<DocumentResultState>({ loading: false, error: '' }),
    resultRequestSequence: 0
  }
}

/**
 * 打开文档解析结果抽屉。
 *
 * @param context - 文档结果抽屉上下文
 * @param document - 文档行数据
 * @returns 打开完成信号
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
async function openDocumentResultWithContext(context: DocumentResultDrawerContext, document: DocumentRow): Promise<void> {
  context.selectedResultDocument.value = document
  context.selectedDocumentResult.value = null
  context.resultDrawerOpen.value = true
  await loadDocumentResult(context, document.document_id)
}

/**
 * 生成文档结果请求标识。
 *
 * @param context - 文档结果抽屉上下文
 * @param documentId - 文档 ID
 * @returns 请求标识
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
function createResultRequestKey(context: DocumentResultDrawerContext, documentId: string): string {
  context.resultRequestSequence += 1
  return `${documentId}${REQUEST_KEY_SEPARATOR}${context.resultRequestSequence}`
}

/**
 * 判断响应是否仍属于正在查看的文档。
 *
 * @param context - 文档结果抽屉上下文
 * @param documentId - 文档 ID
 * @param requestKey - 请求标识
 * @returns 是否为当前有效请求
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
function isActiveResultRequest(request: ResultRequestContext): boolean {
  return request.drawerContext.activeResultRequestKey.value === request.requestKey
    && request.drawerContext.selectedResultDocument.value?.document_id === request.documentId
}

/**
 * 将未知异常转换为文档结果错误消息。
 *
 * @param error - 捕获到的异常对象
 * @returns 可展示的错误消息
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
function toResultErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : DEFAULT_RESULT_ERROR_MESSAGE
}

/**
 * 应用文档解析结果响应。
 *
 * @param context - 文档结果抽屉上下文
 * @param documentId - 文档 ID
 * @param requestKey - 请求标识
 * @param result - 文档解析结果
 * @returns 应用完成信号
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
function applyDocumentResult(request: ResultRequestContext, result: DocumentResultResponse): void {
  if (isActiveResultRequest(request)) {
    // 当前请求仍匹配抽屉中的文档时更新展示结果。
    request.drawerContext.selectedDocumentResult.value = result
  } else {
    // 旧请求晚于新请求返回时丢弃结果，避免展示错文档。
    logStaleResult(request, 'STALE_RESPONSE')
  }
}

/**
 * 处理文档结果加载异常。
 *
 * @param context - 文档结果抽屉上下文
 * @param documentId - 文档 ID
 * @param requestKey - 请求标识
 * @param error - 捕获到的异常对象
 * @returns 处理完成信号
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
function applyResultError(request: ResultRequestContext, error: unknown): void {
  const errorMessage = toResultErrorMessage(error)
  logResultError(request, errorMessage)
  if (isActiveResultRequest(request)) {
    // 当前请求失败时展示错误信息。
    request.drawerContext.resultState.error = errorMessage
  } else {
    // 已切换到其他文档时不覆盖当前抽屉错误状态。
    logStaleResult(request, 'STALE_ERROR')
  }
}

/**
 * 结束文档结果加载态。
 *
 * @param context - 文档结果抽屉上下文
 * @param documentId - 文档 ID
 * @param requestKey - 请求标识
 * @returns 处理完成信号
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
function finishResultRequest(request: ResultRequestContext): void {
  if (isActiveResultRequest(request)) {
    // 当前请求结束时关闭加载态。
    request.drawerContext.resultState.loading = false
  } else {
    // 过期请求结束时保持当前新请求的加载态。
    logStaleResult(request, 'STALE_FINALLY')
  }
}

/**
 * 加载文档解析结果。
 *
 * @param context - 文档结果抽屉上下文
 * @param documentId - 文档 ID
 * @returns 加载完成信号
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
async function loadDocumentResult(context: DocumentResultDrawerContext, documentId: string): Promise<void> {
  const requestKey = createResultRequestKey(context, documentId)
  const request = { drawerContext: context, documentId, requestKey }
  context.activeResultRequestKey.value = requestKey
  context.resultState.loading = true
  context.resultState.error = ''
  try {
    applyDocumentResult(request, await fetchDocumentResult(documentId))
  } catch (error) {
    applyResultError(request, error)
  } finally {
    finishResultRequest(request)
  }
}

/**
 * 重新加载当前文档解析结果。
 *
 * @param context - 文档结果抽屉上下文
 * @returns 重新加载完成信号
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
function retryDocumentResultWithContext(context: DocumentResultDrawerContext): Promise<void> {
  if (context.selectedResultDocument.value) {
    // 已选择文档时重新请求该文档结果。
    return loadDocumentResult(context, context.selectedResultDocument.value.document_id)
  } else {
    // 未选择文档时无需请求。
    return Promise.resolve()
  }
}

/**
 * 记录过期文档结果请求。
 *
 * @param documentId - 文档 ID
 * @param requestKey - 请求标识
 * @param stage - 日志阶段
 * @returns 日志记录完成信号
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
function logStaleResult(request: ResultRequestContext, stage: string): void {
  logDashboardDebug({
    business: RESULT_LOG_BUSINESS,
    stage,
    message: '忽略过期文档结果请求',
    context: { documentId: request.documentId, requestKey: request.requestKey }
  })
}

/**
 * 记录文档结果加载失败。
 *
 * @param documentId - 文档 ID
 * @param requestKey - 请求标识
 * @param error - 错误消息
 * @returns 日志记录完成信号
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
function logResultError(request: ResultRequestContext, error: string): void {
  logDashboardWarn({
    business: RESULT_LOG_BUSINESS,
    stage: 'ERROR',
    message: '加载文档解析结果失败',
    context: { documentId: request.documentId, requestKey: request.requestKey, error }
  })
}

/**
 * 文档结果抽屉状态和加载逻辑。
 *
 * @returns 文档结果抽屉状态与操作
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
export function useDocumentResultDrawer() {
  const context = createDocumentResultDrawerContext()
  return {
    resultDrawerOpen: context.resultDrawerOpen,
    selectedResultDocument: context.selectedResultDocument,
    selectedDocumentResult: context.selectedDocumentResult,
    resultState: context.resultState,
    openDocumentResult: (document: DocumentRow) => openDocumentResultWithContext(context, document),
    retryDocumentResult: () => retryDocumentResultWithContext(context)
  }
}
