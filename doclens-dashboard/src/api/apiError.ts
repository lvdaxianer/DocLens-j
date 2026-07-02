const FALLBACK_ERROR_MESSAGE = '请求失败'
const CODE_FIELD = 'code'
const DETAIL_FIELD = 'detail'
const VALIDATION_FAILED_CODE = 'VALIDATION_FAILED'
const MISSING_PARTITION_CODE = 'MISSING_PARTITION'
const TRUSTED_GATEWAY_UNAUTHORIZED_CODE = 'TRUSTED_GATEWAY_UNAUTHORIZED'
const PARTITION_FORBIDDEN_CODE = 'PARTITION_FORBIDDEN'
const RATE_LIMITED_CODE = 'RATE_LIMITED'
const GLOBAL_PROTECTION_CODE = 'GLOBAL_PROTECTION'
const PAYLOAD_TOO_LARGE_CODE = 'PAYLOAD_TOO_LARGE'
const NOT_FOUND_CODE = 'NOT_FOUND'
const CONFLICT_CODE = 'CONFLICT'
const INTERNAL_ERROR_CODE = 'INTERNAL_ERROR'

const CODE_LABELS: Record<string, string> = {
  [VALIDATION_FAILED_CODE]: '请求参数不合法',
  [MISSING_PARTITION_CODE]: '缺少分区键',
  [TRUSTED_GATEWAY_UNAUTHORIZED_CODE]: '请求未通过可信网关认证',
  [PARTITION_FORBIDDEN_CODE]: '当前身份无权访问该分区',
  [RATE_LIMITED_CODE]: '请求过于频繁',
  [GLOBAL_PROTECTION_CODE]: '服务繁忙',
  [PAYLOAD_TOO_LARGE_CODE]: '上传文件过大',
  [NOT_FOUND_CODE]: '资源不存在',
  [CONFLICT_CODE]: '资源冲突',
  [INTERNAL_ERROR_CODE]: '服务暂时不可用'
}

/**
 * 判断未知值是否为可读取字段的普通对象。
 *
 * @param value - 待检查值
 * @returns 是否为记录对象
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

/**
 * 读取非空字符串字段。
 *
 * @param parsed - 已解析响应体
 * @param fieldName - 字段名
 * @returns 去空格后的字段值
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
function readStringField(parsed: Record<string, unknown>, fieldName: string): string {
  const value = parsed[fieldName]
  if (typeof value === 'string') {
    // 字符串字段统一去除首尾空白，避免展示空 detail。
    return value.trim()
  } else {
    // 非字符串字段不是后端稳定错误契约，交给兜底逻辑处理。
    return ''
  }
}

/**
 * 解析 JSON 错误响应体。
 *
 * @param responseBody - 响应体文本
 * @returns 可读取字段的响应体对象
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
function parseErrorBody(responseBody: string): Record<string, unknown> | undefined {
  try {
    const parsed = JSON.parse(responseBody) as unknown
    if (isRecord(parsed)) {
      // 稳定错误响应必须是对象，数组和原始值不参与字段读取。
      return parsed
    } else {
      // 非对象 JSON 不符合错误契约，交由 HTTP 状态兜底。
      return undefined
    }
  } catch {
    return undefined
  }
}

/**
 * 拼接错误码标签和后端详情。
 *
 * @param code - 后端稳定错误码
 * @param detail - 后端错误详情
 * @param fallbackMessage - 默认错误文案
 * @returns 用户可见错误消息
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
function messageFromCodeDetail(code: string, detail: string, fallbackMessage: string): string {
  const label = CODE_LABELS[code] ?? ''
  if (label && detail) {
    // 稳定 code 和 detail 同时存在时，展示可读分类和具体原因。
    return `${label}：${detail}`
  } else if (detail) {
    // 兼容旧响应体，只有 detail 时直接展示后端详情。
    return detail
  } else if (label) {
    // 极端情况下后端只有 code，仍给出稳定分类。
    return label
  } else {
    // 未知 code 且无 detail 时交给 HTTP 状态兜底。
    return fallbackMessage
  }
}

/**
 * 判断响应体是否包含可展示的结构化错误信息。
 *
 * @param code - 后端稳定错误码
 * @param detail - 后端错误详情
 * @returns 是否存在结构化错误信息
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
function hasStructuredErrorMessage(code: string, detail: string): boolean {
  return Boolean((CODE_LABELS[code] ?? '') || detail)
}

/**
 * 由已读取的响应体生成 API 错误消息。
 *
 * @param response - 原始响应对象
 * @param responseBody - 已读取响应体
 * @param fallbackMessage - 默认错误文案
 * @returns 用户可见错误消息
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
export function apiErrorMessageFromBody(
  response: Response,
  responseBody: string,
  fallbackMessage = FALLBACK_ERROR_MESSAGE
): string {
  const parsed = parseErrorBody(responseBody)
  const code = parsed ? readStringField(parsed, CODE_FIELD) : ''
  const detail = parsed ? readStringField(parsed, DETAIL_FIELD) : ''
  if (hasStructuredErrorMessage(code, detail)) {
    // 结构化响应可用时优先使用后端稳定错误契约。
    return messageFromCodeDetail(code, detail, fallbackMessage)
  } else {
    // 结构化响应不可用时保留 HTTP 状态，方便定位网关或代理错误。
    return `${fallbackMessage}：${response.status} ${response.statusText}`
  }
}

/**
 * 读取响应体并生成 API 错误消息。
 *
 * @param response - 原始响应对象
 * @param fallbackMessage - 默认错误文案
 * @returns 用户可见错误消息
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
export async function apiErrorMessageFromResponse(
  response: Response,
  fallbackMessage = FALLBACK_ERROR_MESSAGE
): Promise<string> {
  return apiErrorMessageFromBody(response, await response.text(), fallbackMessage)
}
