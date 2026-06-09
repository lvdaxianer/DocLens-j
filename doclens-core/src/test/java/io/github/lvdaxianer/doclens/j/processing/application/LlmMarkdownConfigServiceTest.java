package io.github.lvdaxianer.doclens.j.processing.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Optional;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfig;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfigRepository;
import org.junit.jupiter.api.Test;

/**
 * LLM Markdown 配置服务测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
class LlmMarkdownConfigServiceTest {

    /**
     * 测试配置时 API Key 留空应沿用已保存旧密钥。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void settingsForTestKeepsSavedCredentialWhenApiKeyBlank() {
        InMemoryConfigRepository repository = new InMemoryConfigRepository(
                LlmMarkdownConfig.configured("default", "https://llm.example.com/v1/chat/completions",
                        "markdown-model", "sk-saved-secret"));
        LlmMarkdownConfigService service = new LlmMarkdownConfigService(repository);

        LlmMarkdownConfigSettings settings = service.settingsForTest(
                new LlmMarkdownConfigSettings("https://llm.example.com/v1/chat/completions", "markdown-model", ""));

        assertThat(settings.url()).isEqualTo("https://llm.example.com/v1/chat/completions");
        assertThat(settings.model()).isEqualTo("markdown-model");
        assertThat(settings.apiKey()).isEqualTo("sk-saved-secret");
    }

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

    /**
     * 用于单测的内存配置仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static final class InMemoryConfigRepository implements LlmMarkdownConfigRepository {

        private LlmMarkdownConfig config;

        private InMemoryConfigRepository(LlmMarkdownConfig config) {
            this.config = config;
        }

        @Override
        public Optional<LlmMarkdownConfig> find() {
            return Optional.ofNullable(config);
        }

        @Override
        public void save(LlmMarkdownConfig config) {
            this.config = config;
        }
    }
}
