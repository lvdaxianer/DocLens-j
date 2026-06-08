package io.github.lvdaxianer.doclens.j.adapter.application;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;

/**
 * OCR 运行时节点选择视图。
 *
 * @param nodeId 节点 ID
 * @param modelKey OCR 模型标识
 * @param enabled 是否启用
 * @param participateGlobal 是否参与全局负载均衡
 * @param status 节点状态
 * @param maxConcurrency 最大并发图片数
 * @param inflightImages 正在解析图片数
 * @param avgLatencyMs 平均耗时
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public record OcrRuntimeNodeView(
        String nodeId,
        String modelKey,
        boolean enabled,
        boolean participateGlobal,
        OcrNodeStatus status,
        int maxConcurrency,
        int inflightImages,
        long avgLatencyMs
) {
}
