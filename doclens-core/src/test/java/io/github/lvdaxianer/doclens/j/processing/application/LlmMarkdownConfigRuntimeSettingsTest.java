package io.github.lvdaxianer.doclens.j.processing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfig;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfigRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmUsageType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * LLM Markdown 运行时配置测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-13
 */
class LlmMarkdownConfigRuntimeSettingsTest {

    /**
     * 创建配置时应保存 LLM 上下文与调用限流参数。
     *
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    @Test
    void createsConfigWithContextAndRateLimitSettings() {
        LlmMarkdownConfigService service = new LlmMarkdownConfigService(new InMemoryConfigRepository());

        LlmMarkdownConfig saved = service.createConfig(settingsBuilder()
                .maxContextTokens(16000)
                .maxConcurrency(2)
                .requestIntervalMillis(1500)
                .build());

        assertThat(saved.maxContextTokens()).isEqualTo(16000);
        assertThat(saved.maxConcurrency()).isEqualTo(2);
        assertThat(saved.requestIntervalMillis()).isEqualTo(1500);
    }

    /**
     * 上下文 token 过小时应拒绝保存。
     *
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    @Test
    void rejectsInvalidMaxContextTokens() {
        LlmMarkdownConfigService service = new LlmMarkdownConfigService(new InMemoryConfigRepository());

        assertThatThrownBy(() -> service.createConfig(settingsBuilder().maxContextTokens(999).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("llm markdown max context tokens must be at least 1000");
    }

    /**
     * LLM 最大并发数必须大于等于一。
     *
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    @Test
    void rejectsInvalidMaxConcurrency() {
        LlmMarkdownConfigService service = new LlmMarkdownConfigService(new InMemoryConfigRepository());

        assertThatThrownBy(() -> service.createConfig(settingsBuilder().maxConcurrency(0).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("llm markdown max concurrency must be at least 1");
    }

    /**
     * LLM 请求间隔不能为负数。
     *
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    @Test
    void rejectsNegativeRequestIntervalMillis() {
        LlmMarkdownConfigService service = new LlmMarkdownConfigService(new InMemoryConfigRepository());

        assertThatThrownBy(() -> service.createConfig(settingsBuilder().requestIntervalMillis(-1).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("llm markdown request interval millis must be at least 0");
    }

    /**
     * 创建配置提交参数构建器。
     *
     * @return 配置提交参数构建器
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private LlmMarkdownConfigSettings.Builder settingsBuilder() {
        return LlmMarkdownConfigSettings.builder("主配置", "openai", "https://llm.example.com/v1/chat/completions")
                .model("markdown-model")
                .apiKey("sk-test")
                .usageType(LlmUsageType.MARKDOWN_POST_PROCESSING.name())
                .priority(10)
                .defaultConfig(true)
                .enabled(true);
    }

    /**
     * 用于单测的内存配置仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private static final class InMemoryConfigRepository implements LlmMarkdownConfigRepository {

        private final List<LlmMarkdownConfig> configs = new ArrayList<>();

        @Override
        public Optional<LlmMarkdownConfig> find() {
            return configs.stream().findFirst();
        }

        @Override
        public List<LlmMarkdownConfig> listConfigs() {
            return List.copyOf(configs);
        }

        @Override
        public void save(LlmMarkdownConfig config) {
            configs.removeIf(current -> current.id().equals(config.id()));
            configs.add(config);
        }

        @Override
        public void saveAll(List<LlmMarkdownConfig> configs) {
            List<String> ids = configs.stream().map(LlmMarkdownConfig::id).toList();
            this.configs.removeIf(config -> ids.contains(config.id()));
            this.configs.addAll(configs);
        }
    }
}
