package io.github.lvdaxianer.doclens.j.adapter.application;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallRepository;

/**
 * OCR 路由服务依赖集合。
 *
 * @param nodeProvider OCR 运行时节点池端口
 * @param nodeSelector OCR 节点选择器
 * @param nodeExecutor OCR 节点执行端口
 * @param callRepository OCR 调用记录仓储
 * @param callIdGenerator OCR 调用记录 ID 生成器
 * @param properties OCR 路由服务配置
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public record OcrRoutingDependencies(
        OcrRuntimeNodeProvider nodeProvider,
        OcrNodeSelector nodeSelector,
        OcrNodeImageExecutor nodeExecutor,
        OcrNodeCallRepository callRepository,
        OcrCallIdGenerator callIdGenerator,
        OcrRoutingServiceProperties properties
) {
}
