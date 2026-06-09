package io.github.lvdaxianer.doclens.j.processing.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Test;

/**
 * LLM Markdown 配置服务测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
class LlmMarkdownConfigServiceTest {

    /**
     * DashScope 兼容模式基础地址应补全到 chat completions。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void normalizeCompatibleEndpointCompletesDashScopeBasePath() {
        URI normalized = LlmMarkdownConfigService.normalizeCompatibleEndpoint(
                URI.create("https://dashscope.aliyuncs.com/compatible-mode/v1"));

        assertThat(normalized).hasToString(
                "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions");
    }

    /**
     * 非 DashScope 地址不应被错误补全。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void normalizeCompatibleEndpointLeavesOtherHostsUnchanged() {
        URI original = URI.create("http://127.0.0.1:18080/compatible-mode/v1");

        URI normalized = LlmMarkdownConfigService.normalizeCompatibleEndpoint(original);

        assertThat(normalized).isEqualTo(original);
    }
}
