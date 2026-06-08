package io.github.lvdaxianer.doclens.j.adapter.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutePolicy;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 最少解析中图片数 OCR 节点选择器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
class LeastInflightOcrNodeSelectorTest {

    /**
     * 选择器应忽略非健康节点。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void ignoresDownAndDisabledNodes() {
        LeastInflightOcrNodeSelector selector = new LeastInflightOcrNodeSelector();
        List<OcrRuntimeNodeView> nodes = List.of(
                node("down", "paddle_ocr", OcrNodeStatus.DOWN, 0, 100),
                node("disabled", "paddle_ocr", OcrNodeStatus.DISABLED, 0, 100),
                node("up", "paddle_ocr", OcrNodeStatus.UP, 2, 100)
        );

        assertThat(selector.select(OcrRoutePolicy.globalLoadBalance("least-inflight"), nodes))
                .get()
                .extracting(OcrRuntimeNodeView::nodeId)
                .isEqualTo("up");
    }

    /**
     * 全局负载均衡应允许跨模型选择健康节点。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void globalModeIncludesMultipleModels() {
        LeastInflightOcrNodeSelector selector = new LeastInflightOcrNodeSelector();
        List<OcrRuntimeNodeView> nodes = List.of(
                node("paddle", "paddle_ocr", OcrNodeStatus.UP, 3, 100),
                node("other", "other_ocr", OcrNodeStatus.UP, 1, 100)
        );

        assertThat(selector.select(OcrRoutePolicy.globalLoadBalance("least-inflight"), nodes))
                .get()
                .extracting(OcrRuntimeNodeView::nodeId)
                .isEqualTo("other");
    }

    /**
     * 指定模型负载均衡应只在该模型节点池内选择。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void modelModeIncludesOnlyRequestedModel() {
        LeastInflightOcrNodeSelector selector = new LeastInflightOcrNodeSelector();
        List<OcrRuntimeNodeView> nodes = List.of(
                node("paddle", "paddle_ocr", OcrNodeStatus.UP, 3, 100),
                node("other", "other_ocr", OcrNodeStatus.UP, 1, 100)
        );

        assertThat(selector.select(OcrRoutePolicy.modelLoadBalance("paddle_ocr", "least-inflight"), nodes))
                .get()
                .extracting(OcrRuntimeNodeView::nodeId)
                .isEqualTo("paddle");
    }

    /**
     * 解析中图片数相同时应选择平均耗时更低的节点。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void tieBreakerChoosesLowerAverageLatency() {
        LeastInflightOcrNodeSelector selector = new LeastInflightOcrNodeSelector();
        List<OcrRuntimeNodeView> nodes = List.of(
                node("slow", "paddle_ocr", OcrNodeStatus.UP, 1, 200),
                node("fast", "paddle_ocr", OcrNodeStatus.UP, 1, 100)
        );

        assertThat(selector.select(OcrRoutePolicy.globalLoadBalance("least-inflight"), nodes))
                .get()
                .extracting(OcrRuntimeNodeView::nodeId)
                .isEqualTo("fast");
    }

    /**
     * 创建运行时节点视图。
     *
     * @param nodeId 节点 ID
     * @param modelKey 模型标识
     * @param status 节点状态
     * @param inflightImages 正在解析图片数
     * @param avgLatencyMs 平均耗时
     * @return 运行时节点视图
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private OcrRuntimeNodeView node(
            String nodeId,
            String modelKey,
            OcrNodeStatus status,
            int inflightImages,
            long avgLatencyMs
    ) {
        return new OcrRuntimeNodeView(nodeId, modelKey, true, true, status, 4, inflightImages, avgLatencyMs);
    }
}
