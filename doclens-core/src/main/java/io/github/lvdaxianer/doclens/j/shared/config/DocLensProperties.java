package io.github.lvdaxianer.doclens.j.shared.config;

/**
 * Runtime configuration for DocLens core.
 *
 * @param storageRoot storage root for local object storage
 * @param autoProcessOnUpload whether upload requests trigger in-process worker execution
 * @param workerId local worker identifier used for future task acquisition
 * @param callback callback delivery configuration
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
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
