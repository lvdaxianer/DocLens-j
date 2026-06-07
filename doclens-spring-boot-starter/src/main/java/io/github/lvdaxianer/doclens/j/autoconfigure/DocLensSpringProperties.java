package io.github.lvdaxianer.doclens.j.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring-bound DocLens configuration properties.
 *
 * @param storageRoot storage root
 * @param autoProcessOnUpload auto process flag
 * @param workerId worker identifier
 * @param callback callback configuration
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@ConfigurationProperties(prefix = "doclens")
public record DocLensSpringProperties(
        String storageRoot,
        boolean autoProcessOnUpload,
        String workerId,
        CallbackProperties callback
) {

    /**
     * Callback retry and timeout properties.
     *
     * @param maxRetries max retry count
     * @param timeoutSeconds timeout seconds
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public record CallbackProperties(int maxRetries, int timeoutSeconds) {
    }
}
