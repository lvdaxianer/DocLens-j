package io.github.lvdaxianer.doclens.j.query.infrastructure;

import java.util.Optional;
import java.util.concurrent.ExecutorService;

/**
 * Dashboard 线程池集合。
 *
 * @param core 核心线程池集合
 * @param pageTaskWorkerExecutor 页任务 OCR 执行线程池
 * @param pageTaskWorkerSettings 页任务 worker 生效配置
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
public record DashboardThreadPools(
        DashboardCoreThreadPools core,
        Optional<ExecutorService> pageTaskWorkerExecutor,
        Optional<DashboardPageTaskWorkerSettings> pageTaskWorkerSettings
) {

    /**
     * 创建 Dashboard 线程池集合。
     *
     * @param core 核心线程池集合
     * @param pageTaskWorkerExecutor 页任务 OCR 执行线程池
     * @param pageTaskWorkerSettings 页任务 worker 生效配置
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    public DashboardThreadPools {
        pageTaskWorkerExecutor = pageTaskWorkerExecutor == null ? Optional.empty() : pageTaskWorkerExecutor;
        pageTaskWorkerSettings = pageTaskWorkerSettings == null ? Optional.empty() : pageTaskWorkerSettings;
    }

    /**
     * 创建无页任务 worker 配置的 Dashboard 线程池集合。
     *
     * @param core 核心线程池集合
     * @param pageTaskWorkerExecutor 页任务 OCR 执行线程池
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    public DashboardThreadPools(DashboardCoreThreadPools core, ExecutorService pageTaskWorkerExecutor) {
        this(core, Optional.ofNullable(pageTaskWorkerExecutor), Optional.empty());
    }

    /**
     * 获取文档处理线程池。
     *
     * @return 文档处理线程池
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    Optional<ExecutorService> documentProcessing() {
        return Optional.ofNullable(core).map(DashboardCoreThreadPools::documentProcessing);
    }

    /**
     * 获取 OCR 请求线程池。
     *
     * @return OCR 请求线程池
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    Optional<ExecutorService> ocrRequest() {
        return Optional.ofNullable(core).map(DashboardCoreThreadPools::ocrRequest);
    }

    /**
     * 获取 OCR 健康检查线程池。
     *
     * @return OCR 健康检查线程池
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    Optional<ExecutorService> ocrHealth() {
        return Optional.ofNullable(core).map(DashboardCoreThreadPools::ocrHealth);
    }

    /**
     * 获取回调线程池。
     *
     * @return 回调线程池
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    Optional<ExecutorService> callback() {
        return Optional.ofNullable(core).map(DashboardCoreThreadPools::callback);
    }

    /**
     * 获取 LLM Markdown 分块线程池。
     *
     * @return LLM Markdown 分块线程池
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    Optional<ExecutorService> llmMarkdownChunk() {
        return Optional.ofNullable(core).map(DashboardCoreThreadPools::llmMarkdownChunk);
    }
}
