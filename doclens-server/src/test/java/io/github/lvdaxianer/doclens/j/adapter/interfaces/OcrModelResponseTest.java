package io.github.lvdaxianer.doclens.j.adapter.interfaces;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrModelDefinition;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * OCR 模型响应 DTO 测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
class OcrModelResponseTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-21T10:00:00+08:00");
    private static final String PADDLE_MODEL_KEY = "paddle_ocr";
    private static final int PADDLE_DEFAULT_PORT = 18081;

    /**
     * 模型响应应聚合节点并发容量。
     *
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Test
    void fromAggregatesConcurrencyCapacity() {
        OcrModelResponse response = OcrModelResponse.from(definition(), List.of(
                node(new TestNodeConfig("ocr_node_a", true, true, OcrNodeStatus.UP, 10)),
                node(new TestNodeConfig("ocr_node_b", true, false, OcrNodeStatus.UP, 8)),
                node(new TestNodeConfig("ocr_node_c", false, true, OcrNodeStatus.DISABLED, 6))
        ));

        assertThat(response.maxConcurrency()).isEqualTo(24);
        assertThat(response.enabledMaxConcurrency()).isEqualTo(18);
        assertThat(response.globalMaxConcurrency()).isEqualTo(10);
    }

    /**
     * 创建 OCR 模型定义。
     *
     * @return OCR 模型定义
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private OcrModelDefinition definition() {
        return OcrModelDefinition.create(new OcrModelDefinition.CreateCommand(
                new OcrModelDefinition.Identity(PADDLE_MODEL_KEY, "PaddleOCR",
                        "PaddleOCR native-compatible HTTP API"),
                new OcrModelDefinition.Capability(List.of("image"), "/ocr", "/ocr"),
                new OcrModelDefinition.RuntimeDefaults(PADDLE_DEFAULT_PORT, "", "", true)));
    }

    /**
     * 创建 OCR 节点。
     *
     * @param config 节点配置
     * @return OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private OcrNode node(TestNodeConfig config) {
        return new OcrNode(config.nodeId(), PADDLE_MODEL_KEY, config.nodeId(), "10.0.0.1", PADDLE_DEFAULT_PORT,
                config.enabled(), config.participateGlobal(), 100, config.maxConcurrency(), config.status(), 0L, 0L,
                0L, 0L, Optional.of(BASE_TIME), Optional.empty(), Optional.empty(), Optional.empty(), BASE_TIME,
                BASE_TIME);
    }

    /**
     * 测试节点配置。
     *
     * @param nodeId 节点 ID
     * @param enabled 是否启用
     * @param participateGlobal 是否参与全局调度
     * @param status 节点状态
     * @param maxConcurrency 最大并发
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    private record TestNodeConfig(
            String nodeId,
            boolean enabled,
            boolean participateGlobal,
            OcrNodeStatus status,
            int maxConcurrency
    ) {
    }
}
