package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

/**
 * DashScope 在线 OCR 响应体脱敏器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
final class DashScopeOcrResponseSanitizer {

    private static final int ERROR_BODY_MAX_LENGTH = 500;
    private static final String MASKED_CREDENTIAL = "***";

    /**
     * 对第三方响应体中可能回显的密钥做脱敏。
     *
     * @param responseBody 响应体
     * @param credential 真实凭证
     * @return 脱敏后的响应体
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    String summarizeBody(String responseBody, String credential) {
        String sanitizedBody = sanitizeBody(responseBody, credential);
        if (sanitizedBody.length() <= ERROR_BODY_MAX_LENGTH) {
            // 短响应可完整保留，方便排查第三方错误。
            return sanitizedBody;
        } else {
            // 长响应只保留前缀，避免日志和异常消息过大。
            return sanitizedBody.substring(0, ERROR_BODY_MAX_LENGTH);
        }
    }

    /**
     * 对第三方响应体中可能回显的密钥做脱敏。
     *
     * @param responseBody 响应体
     * @param credential 真实凭证
     * @return 脱敏后的响应体
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private String sanitizeBody(String responseBody, String credential) {
        String safeBody = responseBody == null ? "" : responseBody;
        String safeCredential = credential == null ? "" : credential.trim();
        if (!safeCredential.isBlank()) {
            // 第三方错误体可能回显密钥，写日志前必须脱敏。
            return safeBody.replace(safeCredential, MASKED_CREDENTIAL);
        } else {
            // 未配置密钥时无需替换，直接返回安全响应体。
            return safeBody;
        }
    }
}
