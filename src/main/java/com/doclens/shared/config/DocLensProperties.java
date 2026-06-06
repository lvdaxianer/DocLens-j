package com.doclens.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Runtime configuration for DocLens service.
 *
 * @param storageRoot storage root for local object storage
 * @param autoProcessOnUpload whether upload requests trigger in-process worker execution
 * @param workerId local worker identifier used for future task acquisition
 * @param callback callback delivery configuration
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@ConfigurationProperties(prefix = "doclens")
public record DocLensProperties(
        String storageRoot,
        boolean autoProcessOnUpload,
        String workerId,
        CallbackProperties callback
) {
    /**
     * Callback retry and timeout configuration.
     *
     * @param maxRetries maximum callback retry count
     * @param timeoutSeconds callback request timeout in seconds
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public record CallbackProperties(int maxRetries, int timeoutSeconds) {
    }
}
