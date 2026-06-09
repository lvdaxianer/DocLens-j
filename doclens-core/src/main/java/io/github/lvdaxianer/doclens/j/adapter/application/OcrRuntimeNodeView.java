package io.github.lvdaxianer.doclens.j.adapter.application;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * OCR 运行时节点选择视图。
 *
 * @param nodeId 节点 ID
 * @param modelKey OCR 模型标识
 * @param enabled 是否启用
 * @param participateGlobal 是否参与全局负载均衡
 * @param status 节点状态
 * @param weight 节点权重
 * @param maxConcurrency 最大并发图片数
 * @param inflightImages 正在解析图片数
 * @param queuedImages 排队图片数
 * @param availableSlots 可用槽位数
 * @param avgLatencyMs 平均耗时
 * @param circuitOpenUntil 熔断结束时间
 * @param consecutiveFailureCount 连续失败次数
 * @param recoverySuccessCount 恢复成功次数
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public record OcrRuntimeNodeView(
        String nodeId,
        String modelKey,
        boolean enabled,
        boolean participateGlobal,
        OcrNodeStatus status,
        int weight,
        int maxConcurrency,
        int inflightImages,
        int queuedImages,
        int availableSlots,
        long avgLatencyMs,
        Optional<OffsetDateTime> circuitOpenUntil,
        long consecutiveFailureCount,
        long recoverySuccessCount
) {

    /**
     * 创建兼容旧字段的 OCR 运行时节点视图。
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
     * @date 2026-06-10
     */
    public OcrRuntimeNodeView(
            String nodeId,
            String modelKey,
            boolean enabled,
            boolean participateGlobal,
            OcrNodeStatus status,
            int maxConcurrency,
            int inflightImages,
            long avgLatencyMs
    ) {
        this(nodeId, modelKey, enabled, participateGlobal, status, 0, maxConcurrency, inflightImages, 0,
                Math.max(0, maxConcurrency - inflightImages), avgLatencyMs, Optional.empty(), 0L, 0L);
    }
}
