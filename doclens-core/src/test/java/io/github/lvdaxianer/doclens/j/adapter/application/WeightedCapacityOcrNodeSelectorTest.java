package io.github.lvdaxianer.doclens.j.adapter.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutePolicy;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/**
 * 加权容量 OCR 节点选择器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
class WeightedCapacityOcrNodeSelectorTest {

    /**
     * 当空闲差距明显时应优先选择更空闲节点。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void selectorPrefersFreerNodeEvenWhenItsWeightIsLower() {
        WeightedCapacityOcrNodeSelector selector = new WeightedCapacityOcrNodeSelector(0.7D, 0.3D, 0.15D);

        List<OcrRuntimeNodeView> nodes = List.of(
                node("A", 80, 10, 10),
                node("B", 60, 10, 8),
                node("C", 20, 10, 0)
        );

        Optional<OcrRuntimeNodeView> selected = selector.select(OcrRoutePolicy.globalLoadBalance("weighted-idle"),
                nodes);

        assertThat(selected).isPresent();
        assertThat(selected.get().nodeId()).isEqualTo("C");
    }

    /**
     * 当节点空闲度接近时应按权重平滑轮转。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void selectorSmoothsAcrossSimilarlyIdleNodesByWeight() {
        WeightedCapacityOcrNodeSelector selector = new WeightedCapacityOcrNodeSelector(0.7D, 0.3D, 0.15D);

        List<OcrRuntimeNodeView> nodes = List.of(
                node("A", 80, 10, 0),
                node("B", 60, 10, 0),
                node("C", 20, 10, 0)
        );

        List<String> picks = IntStream.range(0, 8)
                .mapToObj(index -> selector.select(OcrRoutePolicy.globalLoadBalance("weighted-idle"), nodes)
                        .orElseThrow()
                        .nodeId())
                .toList();

        assertThat(picks).containsExactly("A", "B", "A", "C", "B", "A", "B", "A");
    }

    /**
     * 创建运行时节点视图。
     *
     * @param nodeId 节点 ID
     * @param weight 节点权重
     * @param maxConcurrency 最大并发
     * @param inflightImages 正在解析图片数
     * @return 运行时节点视图
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private OcrRuntimeNodeView node(String nodeId, int weight, int maxConcurrency, int inflightImages) {
        return new OcrRuntimeNodeView(nodeId, "paddle_ocr", true, true, OcrNodeStatus.UP, weight, maxConcurrency,
                inflightImages, 0, Math.max(0, maxConcurrency - inflightImages), 100L, Optional.empty(), 0L, 0L);
    }
}
