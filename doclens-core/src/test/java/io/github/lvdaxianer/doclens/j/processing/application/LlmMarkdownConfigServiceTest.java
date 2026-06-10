package io.github.lvdaxianer.doclens.j.processing.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Optional;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfig;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfigRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownApiType;
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
                new LlmMarkdownConfigSettings("openai", "https://llm.example.com/v1/chat/completions",
                        "markdown-model", ""));

        assertThat(settings.apiType()).isEqualTo(LlmMarkdownApiType.OPENAI.value());
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

        URI normalized = LlmMarkdownConfigService.normalizeOpenAiEndpoint(original);

        assertThat(normalized).isEqualTo(original);
    }

    /**
     * OpenAI URL 截止到 /v1 时应补全 chat completions。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void normalizeOpenAiEndpointCompletesGenericV1BasePath() {
        URI normalized = LlmMarkdownConfigService.normalizeOpenAiEndpoint(
                URI.create("https://api.openai-compatible.example/v1"));

        assertThat(normalized).hasToString("https://api.openai-compatible.example/v1/chat/completions");
    }

    /**
     * Anthropic URL 截止到 /anthropic 时应补全 messages。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void normalizeAnthropicEndpointCompletesBasePath() {
        URI normalized = LlmMarkdownConfigService.normalizeAnthropicEndpoint(
                URI.create("https://api.minimaxi.com/anthropic"));

        assertThat(normalized).hasToString("https://api.minimaxi.com/anthropic/v1/messages");
    }

    /**
     * 保存 Anthropic 配置时应保留协议类型并归一化 endpoint。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void saveConfigPersistsAnthropicApiType() {
        InMemoryConfigRepository repository = new InMemoryConfigRepository(null);
        LlmMarkdownConfigService service = new LlmMarkdownConfigService(repository);

        LlmMarkdownConfig saved = service.saveConfig(new LlmMarkdownConfigSettings("anthropic",
                "https://api.minimaxi.com/anthropic", "MiniMax-M3", "sk-minimax"));

        assertThat(saved.apiType()).isEqualTo(LlmMarkdownApiType.ANTHROPIC);
        assertThat(saved.url()).isEqualTo("https://api.minimaxi.com/anthropic/v1/messages");
        assertThat(saved.model()).isEqualTo("MiniMax-M3");
        assertThat(saved.credentialConfigured()).isTrue();
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
