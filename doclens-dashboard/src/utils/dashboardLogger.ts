/**
 * Dashboard 前端日志工具，统一输出业务标识与结构化上下文。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */

export interface DashboardLogPayload {
  business: string
  stage: string
  message: string
  context?: Record<string, unknown>
}

const DASHBOARD_LOG_PREFIX = '[DocLens Dashboard]'
const EMPTY_CONTEXT_TEXT = ''
const CONTEXT_SEPARATOR = ' '
const BROWSER_LOGGER = globalThis['console']

/**
 * 将日志上下文序列化为稳定文本。
 *
 * @param context - 结构化日志上下文
 * @returns 可追加到日志消息后的上下文文本
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
function serializeContext(context: Record<string, unknown> | undefined): string {
  return context === undefined ? EMPTY_CONTEXT_TEXT : `${CONTEXT_SEPARATOR}${JSON.stringify(context)}`
}

/**
 * 格式化 Dashboard 日志消息。
 *
 * @param payload - 日志载荷
 * @returns 带业务标识、阶段和上下文的日志文本
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
function formatDashboardMessage(payload: DashboardLogPayload): string {
  return `${DASHBOARD_LOG_PREFIX} ${payload.business}|${payload.stage}|${payload.message}${serializeContext(payload.context)}`
}

/**
 * 输出 Dashboard 调试日志。
 *
 * @param payload - 日志载荷
 * @returns 日志输出完成信号
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
export function logDashboardDebug(payload: DashboardLogPayload): void {
  import.meta.env.DEV ? BROWSER_LOGGER.debug(formatDashboardMessage(payload)) : undefined
}

/**
 * 输出 Dashboard 警告日志。
 *
 * @param payload - 日志载荷
 * @returns 日志输出完成信号
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
export function logDashboardWarn(payload: DashboardLogPayload): void {
  BROWSER_LOGGER.warn(formatDashboardMessage(payload))
}
