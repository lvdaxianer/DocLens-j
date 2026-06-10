package io.github.lvdaxianer.doclens.j.processing.application;

import static org.assertj.core.api.Assertions.assertThat;

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
     * OpenAI compatible 地址应按用户输入原样保存。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void saveConfigKeepsOpenAiCompatibleUrlUnchanged() {
        InMemoryConfigRepository repository = new InMemoryConfigRepository(null);
        LlmMarkdownConfigService service = new LlmMarkdownConfigService(repository);

        LlmMarkdownConfig saved = service.saveConfig(new LlmMarkdownConfigSettings("openai",
                "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-vl-ocr-2025-11-20",
                "sk-dashscope"));

        assertThat(saved.apiType()).isEqualTo(LlmMarkdownApiType.OPENAI);
        assertThat(saved.url()).isEqualTo("https://dashscope.aliyuncs.com/compatible-mode/v1");
    }

    /**
     * 保存 Anthropic 配置时应保留协议类型和用户输入 URL。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void saveConfigKeepsAnthropicUrlUnchanged() {
        InMemoryConfigRepository repository = new InMemoryConfigRepository(null);
        LlmMarkdownConfigService service = new LlmMarkdownConfigService(repository);

        LlmMarkdownConfig saved = service.saveConfig(new LlmMarkdownConfigSettings("anthropic",
                "https://api.minimaxi.com/anthropic", "MiniMax-M3", "sk-minimax"));

        assertThat(saved.apiType()).isEqualTo(LlmMarkdownApiType.ANTHROPIC);
        assertThat(saved.url()).isEqualTo("https://api.minimaxi.com/anthropic");
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
