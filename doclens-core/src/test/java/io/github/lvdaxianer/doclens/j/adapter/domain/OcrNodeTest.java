package io.github.lvdaxianer.doclens.j.adapter.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

/**
 * OCR 节点领域对象测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
class OcrNodeTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-08T12:00:00+08:00");

    /**
     * 创建节点时应拒绝无效端口。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void createNodeRejectsInvalidPort() {
        assertThatThrownBy(() -> OcrNode.create(requestWithPort(0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("port");
    }

    /**
     * 禁用节点创建后应处于禁用状态。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void disabledNodeUsesDisabledStatus() {
        OcrNode node = OcrNode.create(defaultRequest(false));

        assertThat(node.status()).isEqualTo(OcrNodeStatus.DISABLED);
    }

    /**
     * 创建指定端口的测试请求。
     *
     * @param port OCR 节点端口
     * @return OCR 节点创建请求
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private OcrNodeCreateRequest requestWithPort(int port) {
        return new OcrNodeCreateRequest("node_1", "paddle_ocr", "paddle-1",
                "10.100.30.215", port, true, true, 100, 4, BASE_TIME);
    }

    /**
     * 创建指定启用状态的测试请求。
     *
     * @param enabled 是否启用
     * @return OCR 节点创建请求
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private OcrNodeCreateRequest defaultRequest(boolean enabled) {
        return new OcrNodeCreateRequest("node_1", "paddle_ocr", "paddle-1",
                "10.100.30.215", 8080, enabled, true, 100, 4, BASE_TIME);
    }
}
