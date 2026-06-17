package io.github.lvdaxianer.doclens.j.ingestion.domain;

import java.util.Map;
import java.util.Optional;

/**
 * 调用方身份值对象，用于把批次归因到接入系统而非个人用户。
 *
 * @param clientId 接入方标识
 * @param sourceApp 来源应用
 * @param tenantKey 租户或业务分区键
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
public record CallerIdentity(
        String clientId,
        String sourceApp,
        Optional<String> tenantKey
) {
    private static final String ANONYMOUS_CLIENT_ID = "anonymous";
    private static final String UNKNOWN_SOURCE_APP = "unknown";
    public static final String CLIENT_ID_FIELD = "client_id";
    public static final String SOURCE_APP_FIELD = "source_app";
    public static final String TENANT_KEY_FIELD = "tenant_key";

    /**
     * 创建带安全默认值的调用方身份。
     *
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public CallerIdentity {
        clientId = cleanOrDefault(clientId, ANONYMOUS_CLIENT_ID);
        sourceApp = cleanOrDefault(sourceApp, UNKNOWN_SOURCE_APP);
        tenantKey = tenantKey == null ? Optional.empty() : tenantKey.filter(value -> !value.isBlank());
    }

    /**
     * 创建匿名调用方身份。
     *
     * @return 匿名调用方身份
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public static CallerIdentity anonymous() {
        return new CallerIdentity(ANONYMOUS_CLIENT_ID, UNKNOWN_SOURCE_APP, Optional.empty());
    }

    /**
     * 转换为 API/Dashboard 字段。
     *
     * @return 调用方字段 Map
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public Map<String, Object> toMap() {
        return Map.of(CLIENT_ID_FIELD, clientId, SOURCE_APP_FIELD, sourceApp,
                TENANT_KEY_FIELD, tenantKey.orElse(""));
    }

    /**
     * 清理文本并提供默认值。
     *
     * @param value 原始值
     * @param defaultValue 默认值
     * @return 可用文本
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private static String cleanOrDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        } else {
            return value.trim();
        }
    }
}
