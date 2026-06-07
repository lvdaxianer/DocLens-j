package io.github.lvdaxianer.doclens.j.shared.config;

/**
 * DocLens core 运行时配置。
 *
 * @param storageRoot 本地对象存储根目录
 * @param autoProcessOnUpload 上传请求是否触发进程内 Worker 执行
 * @param workerId 用于后续任务获取的本地 Worker 标识
 * @param callback 回调投递配置
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
     * 回调重试与超时配置。
     *
     * @param maxRetries 最大回调重试次数
     * @param timeoutSeconds 回调请求超时时间，单位秒
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public record CallbackProperties(int maxRetries, int timeoutSeconds) {
    }
}
