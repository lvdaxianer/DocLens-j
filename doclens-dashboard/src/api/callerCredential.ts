const CALLER_CREDENTIAL_STORAGE_KEY = 'X-DocLens-Credential-Key'
const CALLER_API_KEY_HEADER = 'X-DocLens-Api-Key'
const AUTHORIZATION_HEADER = 'Authorization'
const BEARER_PREFIX = 'Bearer '

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
    return {}
  } else if (credential.startsWith(BEARER_PREFIX)) {
    return { [AUTHORIZATION_HEADER]: credential }
  } else {
    return { [CALLER_API_KEY_HEADER]: credential }
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
