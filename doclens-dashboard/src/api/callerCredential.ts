const CALLER_CREDENTIAL_STORAGE_KEY = 'X-Recall-Key'
const CALLER_PARTITION_HEADER = 'X-Recall-Key'

/**
 * 读取 Dashboard 当前缓存的 caller 凭证。
 *
 * @returns caller 凭证文本
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
function readCallerCredential(): string {
  try {
    const credential = globalThis.sessionStorage?.getItem(CALLER_CREDENTIAL_STORAGE_KEY)
    return credential?.trim() ?? ''
  } catch {
    return ''
  }
}

/**
 * 将缓存的 caller 凭证映射为请求头。
 *
 * @returns caller 凭证请求头
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
function callerCredentialHeaders(): Record<string, string> {
  const credential = readCallerCredential()
  if (!credential) {
    // 当前会话没有 caller 分区键时，不附加隔离头。
    return {}
  } else {
    // caller 分区键只做数据隔离，必须原样透传给后端。
    return { [CALLER_PARTITION_HEADER]: credential }
  }
}

/**
 * 在现有请求头基础上追加 caller 凭证。
 *
 * @param headers - 现有请求头
 * @returns 追加 caller 凭证后的请求头
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
export function withCallerCredentialHeaders(headers: Record<string, string>): Record<string, string> {
  return { ...headers, ...callerCredentialHeaders() }
}
