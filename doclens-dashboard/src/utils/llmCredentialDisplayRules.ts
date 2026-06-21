const CREDENTIAL_ENV_VAR_HELP_TEXT = '这里只填写服务进程可读取的环境变量名，真实 Key 需在启动服务前写入该环境变量。'

/**
 * 获取凭证环境变量名字段说明。
 *
 * @returns 环境变量名填写说明
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
export function credentialEnvVarHelpText(): string {
  return CREDENTIAL_ENV_VAR_HELP_TEXT
}
