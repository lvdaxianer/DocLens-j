package io.github.lvdaxianer.doclens.j.adapter.application;

/**
 * OCR 节点评分明细。
 *
 * @param node 运行时节点
 * @param idleRatio 空闲容量占比
 * @param weightRatio 权重占比
 * @param score 综合评分
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public record OcrNodeScore(
        OcrRuntimeNodeView node,
        double idleRatio,
        double weightRatio,
        double score
) {
}
