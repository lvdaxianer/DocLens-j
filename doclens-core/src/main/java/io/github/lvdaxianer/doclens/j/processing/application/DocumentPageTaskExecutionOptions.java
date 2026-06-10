package io.github.lvdaxianer.doclens.j.processing.application;

import java.util.concurrent.Executor;
import java.util.Objects;

/**
 * 文档页任务执行配置。
 *
 * @param workerId 工作线程标识
 * @param workerBatchSize 每轮最大抢占任务数
 * @param lockSeconds 任务锁秒数
 * @param pageTaskExecutor 页任务执行线程池
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
public record DocumentPageTaskExecutionOptions(
        String workerId,
        int workerBatchSize,
        int lockSeconds,
        Executor pageTaskExecutor
) {

    /**
     * 创建规整后的文档页任务执行配置。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    public DocumentPageTaskExecutionOptions {
        workerId = Objects.requireNonNull(workerId, "workerId must not be null");
        pageTaskExecutor = Objects.requireNonNull(pageTaskExecutor, "pageTaskExecutor must not be null");
        workerBatchSize = Math.max(1, workerBatchSize);
        lockSeconds = Math.max(1, lockSeconds);
    }
}
