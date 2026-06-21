package io.github.lvdaxianer.doclens.j.adapter.application;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallRepository;

/**
 * OCR 路由服务依赖集合。
 *
 * @param nodeProvider OCR 运行时节点池端口
 * @param nodeSelector OCR 节点选择器
 * @param dispatchCoordinator OCR 同步派发协调器
 * @param nodeExecutor OCR 节点执行端口
 * @param callRepository OCR 调用记录仓储
 * @param callIdGenerator OCR 调用记录 ID 生成器
 * @param batchHitTracker 批次运行时命中跟踪器
 * @param properties OCR 路由服务配置
 * @param documentAffinityTracker OCR 文档级模型亲和力跟踪器
 * @param runningPageTaskTracker OCR 运行中图片页任务追踪器
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public record OcrRoutingDependencies(
        OcrRuntimeNodeProvider nodeProvider,
        OcrNodeSelector nodeSelector,
        OcrDispatchCoordinator dispatchCoordinator,
        OcrNodeImageExecutor nodeExecutor,
        OcrNodeCallRepository callRepository,
        OcrCallIdGenerator callIdGenerator,
        OcrBatchHitTracker batchHitTracker,
        OcrRoutingServiceProperties properties,
        OcrDocumentAffinityTracker documentAffinityTracker,
        OcrRunningPageTaskTracker runningPageTaskTracker
) {

    /**
     * 创建兼容旧装配路径的 OCR 路由依赖集合。
     *
     * @param nodeProvider OCR 运行时节点池端口
     * @param nodeSelector OCR 节点选择器
     * @param dispatchCoordinator OCR 同步派发协调器
     * @param nodeExecutor OCR 节点执行端口
     * @param callRepository OCR 调用记录仓储
     * @param callIdGenerator OCR 调用记录 ID 生成器
     * @param batchHitTracker 批次运行时命中跟踪器
     * @param properties OCR 路由服务配置
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public OcrRoutingDependencies(
            OcrRuntimeNodeProvider nodeProvider,
            OcrNodeSelector nodeSelector,
            OcrDispatchCoordinator dispatchCoordinator,
            OcrNodeImageExecutor nodeExecutor,
            OcrNodeCallRepository callRepository,
            OcrCallIdGenerator callIdGenerator,
            OcrBatchHitTracker batchHitTracker,
            OcrRoutingServiceProperties properties
    ) {
        this(nodeProvider, nodeSelector, dispatchCoordinator, nodeExecutor, callRepository, callIdGenerator,
                batchHitTracker, properties, new InMemoryOcrDocumentAffinityTracker(),
                new NoopOcrRunningPageTaskTracker());
    }

    /**
     * 创建带文档亲和力、无运行页任务追踪器的 OCR 路由依赖集合。
     *
     * @param nodeProvider OCR 运行时节点池端口
     * @param nodeSelector OCR 节点选择器
     * @param dispatchCoordinator OCR 同步派发协调器
     * @param nodeExecutor OCR 节点执行端口
     * @param callRepository OCR 调用记录仓储
     * @param callIdGenerator OCR 调用记录 ID 生成器
     * @param batchHitTracker 批次运行时命中跟踪器
     * @param properties OCR 路由服务配置
     * @param documentAffinityTracker OCR 文档级模型亲和力跟踪器
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    public OcrRoutingDependencies(
            OcrRuntimeNodeProvider nodeProvider,
            OcrNodeSelector nodeSelector,
            OcrDispatchCoordinator dispatchCoordinator,
            OcrNodeImageExecutor nodeExecutor,
            OcrNodeCallRepository callRepository,
            OcrCallIdGenerator callIdGenerator,
            OcrBatchHitTracker batchHitTracker,
            OcrRoutingServiceProperties properties,
            OcrDocumentAffinityTracker documentAffinityTracker
    ) {
        this(nodeProvider, nodeSelector, dispatchCoordinator, nodeExecutor, callRepository, callIdGenerator,
                batchHitTracker, properties, documentAffinityTracker, new NoopOcrRunningPageTaskTracker());
    }
}
