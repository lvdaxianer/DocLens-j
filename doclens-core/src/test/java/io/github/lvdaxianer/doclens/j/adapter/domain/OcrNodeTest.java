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
     * 在线节点应忽略外部传入的 host 与 port。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void onlineNodeIgnoresHostAndPort() {
        OcrNode node = OcrNode.create(new OcrNodeCreateRequest("node_online", "paddle_ocr",
                OcrNodeDeploymentType.ONLINE, "online-1", "https://example.com/ocr", 8080,
                "aliyun_bailian_dashscope", "qwen-vl-ocr-2025-11-20", "sk-test", true, true,
                true, 100, 4, BASE_TIME));

        assertThat(node.host()).isEmpty();
        assertThat(node.port()).isZero();
    }

    /**
     * Ollama 离线节点需要保留协议渠道和供应商模型名称，用于构造 /api/generate 请求。
     *
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void ollamaOfflineNodeKeepsChannelAndProviderModel() {
        OcrNode node = OcrNode.create(new OcrNodeCreateRequest("node_ollama", "ollama_deepseek_ocr",
                OcrNodeDeploymentType.OFFLINE, "ollama-1", "10.100.30.215", 11434,
                "ollama", "deepseek-ocr:latest", "", false, true, true, 100, 4, BASE_TIME));

        assertThat(node.channelKey()).contains("ollama");
        assertThat(node.providerModel()).contains("deepseek-ocr:latest");
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
