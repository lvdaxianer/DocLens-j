package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.adapter.application.NoopOcrRunningPageTaskTracker;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRunningPageTaskTracker;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingService;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageResultRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskRepository;
import io.github.lvdaxianer.doclens.j.storage.ObjectStorage;

/**
 * 文档页任务执行依赖集合。
 *
 * @param pageTaskRepository 页任务仓储
 * @param documentRepository 文档仓储
 * @param pageResultRepository 页结果仓储
 * @param objectStorage 对象存储
 * @param routingService OCR 路由服务
 * @param runningPageTaskTracker OCR 运行中图片页任务追踪器
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
public record DocumentPageTaskExecutionDependencies(
        DocumentPageTaskRepository pageTaskRepository,
        DocumentJobRepository documentRepository,
        DocumentPageResultRepository pageResultRepository,
        ObjectStorage objectStorage,
        OcrRoutingService routingService,
        OcrRunningPageTaskTracker runningPageTaskTracker
) {

    /**
     * 创建无运行态追踪器的执行依赖，兼容旧测试和嵌入式调用。
     *
     * @param pageTaskRepository 页任务仓储
     * @param documentRepository 文档仓储
     * @param pageResultRepository 页结果仓储
     * @param objectStorage 对象存储
     * @param routingService OCR 路由服务
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    public DocumentPageTaskExecutionDependencies(
            DocumentPageTaskRepository pageTaskRepository,
            DocumentJobRepository documentRepository,
            DocumentPageResultRepository pageResultRepository,
            ObjectStorage objectStorage,
            OcrRoutingService routingService
    ) {
        this(pageTaskRepository, documentRepository, pageResultRepository, objectStorage, routingService,
                new NoopOcrRunningPageTaskTracker());
    }
}
