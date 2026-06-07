package io.github.lvdaxianer.doclens.j.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 绑定到 Spring 的 DocLens 配置属性。
 *
 * @param storageRoot 存储根目录
 * @param autoProcessOnUpload 自动处理标志
 * @param workerId Worker 标识
 * @param callback 回调配置
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
     * 回调重试与超时属性。
     *
     * @param maxRetries 最大重试次数
     * @param timeoutSeconds 超时秒数
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public record CallbackProperties(int maxRetries, int timeoutSeconds) {
    }
}
