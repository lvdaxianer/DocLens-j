package io.github.lvdaxianer.doclens.j.processing.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownApiType;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfig;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfigRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmUsageType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * LLM Markdown 配置服务测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
class LlmMarkdownConfigServiceTest {

    /**
     * 测试配置时凭证环境变量名留空应沿用已保存引用。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void settingsForTestKeepsSavedCredentialEnvVarWhenBlank() {
        InMemoryConfigRepository repository = new InMemoryConfigRepository(
                LlmMarkdownConfig.configured("default", "https://llm.example.com/v1/chat/completions",
                        "markdown-model", "MINIMAX_API_KEY"));
        LlmMarkdownConfigService service = new LlmMarkdownConfigService(repository);

        LlmMarkdownConfigSettings settings = service.settingsForTest(
                new LlmMarkdownConfigSettings("openai", "https://llm.example.com/v1/chat/completions",
                        "markdown-model", ""));

        assertThat(settings.apiType()).isEqualTo(LlmMarkdownApiType.OPENAI.value());
        assertThat(settings.url()).isEqualTo("https://llm.example.com/v1/chat/completions");
        assertThat(settings.model()).isEqualTo("markdown-model");
        assertThat(settings.apiKey()).isEqualTo("MINIMAX_API_KEY");
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
                "DASHSCOPE_API_KEY"));

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
                "https://api.minimaxi.com/anthropic", "MiniMax-M3", "MINIMAX_API_KEY", false));

        assertThat(saved.apiType()).isEqualTo(LlmMarkdownApiType.ANTHROPIC);
        assertThat(saved.url()).isEqualTo("https://api.minimaxi.com/anthropic");
        assertThat(saved.model()).isEqualTo("MiniMax-M3");
        assertThat(saved.credentialConfigured()).isTrue();
        assertThat(saved.enabled()).isFalse();
    }

    /**
     * 保存配置时未显式提交启用状态应继承当前状态。
     *
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void saveConfigKeepsCurrentEnabledStateWhenRequestOmitsIt() {
        InMemoryConfigRepository repository = new InMemoryConfigRepository(
                LlmMarkdownConfig.configured("default", "https://llm.example.com/v1/chat/completions",
                        "markdown-model", "MINIMAX_API_KEY").withEnabled(false));
        LlmMarkdownConfigService service = new LlmMarkdownConfigService(repository);

        LlmMarkdownConfig saved = service.saveConfig(new LlmMarkdownConfigSettings("openai",
                "https://llm.example.com/v1/chat/completions", "markdown-model-v2", "", null));

        assertThat(saved.enabled()).isFalse();
        assertThat(saved.isConfigured()).isTrue();
    }

    /**
     * 多 LLM 配置应允许保存不同名称的配置。
     *
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void saveConfigAllowsMultipleNamedConfigs() {
        InMemoryConfigRepository repository = new InMemoryConfigRepository(null);
        LlmMarkdownConfigService service = new LlmMarkdownConfigService(repository);

        LlmMarkdownConfig first = service.createConfig(settings("主配置", 10, true, true));
        LlmMarkdownConfig second = service.createConfig(settings("备用配置", 20, false, true));

        assertThat(first.name()).isEqualTo("主配置");
        assertThat(second.name()).isEqualTo("备用配置");
        assertThat(repository.listConfigs()).extracting(LlmMarkdownConfig::name)
                .containsExactly("主配置", "备用配置");
    }

    /**
     * 同一用途下设置默认配置时应清除其它默认配置。
     *
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void createConfigKeepsSingleDefaultPerUsageType() {
        InMemoryConfigRepository repository = new InMemoryConfigRepository(null);
        LlmMarkdownConfigService service = new LlmMarkdownConfigService(repository);

        service.createConfig(settings("默认一", 10, true, true));
        LlmMarkdownConfig second = service.createConfig(settings("默认二", 20, true, true));

        assertThat(second.defaultConfig()).isTrue();
        assertThat(repository.listConfigs()).filteredOn(LlmMarkdownConfig::defaultConfig).singleElement()
                .extracting(LlmMarkdownConfig::name).isEqualTo("默认二");
    }

    /**
     * 禁用单个配置不应影响其它 LLM 配置。
     *
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void updateEnabledOnlyChangesTargetConfig() {
        InMemoryConfigRepository repository = new InMemoryConfigRepository(null);
        LlmMarkdownConfigService service = new LlmMarkdownConfigService(repository);
        LlmMarkdownConfig first = service.createConfig(settings("主配置", 10, true, true));
        LlmMarkdownConfig second = service.createConfig(settings("备用配置", 20, false, true));

        service.updateEnabled(first.id(), false);

        assertThat(repository.require(first.id()).enabled()).isFalse();
        assertThat(repository.require(second.id()).enabled()).isTrue();
    }

    /**
     * 创建多配置提交参数。
     *
     * @param name 配置名称
     * @param priority 优先级
     * @param defaultConfig 是否默认配置
     * @param enabled 是否启用
     * @return 配置提交参数
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private LlmMarkdownConfigSettings settings(
            String name,
            int priority,
            boolean defaultConfig,
            boolean enabled
    ) {
        return LlmMarkdownConfigSettings.builder(name, "openai", "https://llm.example.com/v1/chat/completions")
                .model("markdown-model")
                .credentialEnvVar("MINIMAX_API_KEY")
                .usageType(LlmUsageType.MARKDOWN_POST_PROCESSING.name())
                .priority(priority)
                .defaultConfig(defaultConfig)
                .enabled(enabled)
                .build();
    }

    /**
     * 用于单测的内存配置仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static final class InMemoryConfigRepository implements LlmMarkdownConfigRepository {

        private final List<LlmMarkdownConfig> configs = new ArrayList<>();

        private InMemoryConfigRepository(LlmMarkdownConfig config) {
            // 传入初始配置时，模拟数据库已有一条记录。
            if (config != null) {
                configs.add(config);
            } else {
                // 空仓储用于模拟未配置 LLM 的初始状态。
            }
        }

        @Override
        public Optional<LlmMarkdownConfig> find() {
            return configs.stream().filter(LlmMarkdownConfig::defaultConfig).findFirst()
                    .or(() -> configs.stream().findFirst());
        }

        @Override
        public List<LlmMarkdownConfig> listConfigs() {
            return List.copyOf(configs);
        }

        @Override
        public void save(LlmMarkdownConfig config) {
            delete(config.id());
            configs.add(config);
        }

        @Override
        public void saveAll(List<LlmMarkdownConfig> configs) {
            List<String> ids = configs.stream().map(LlmMarkdownConfig::id).toList();
            this.configs.removeIf(config -> ids.contains(config.id()));
            this.configs.addAll(configs);
        }

        /**
         * 删除指定配置。
         *
         * @param id 配置 ID
         * @author lvdaxianerplus
         * @date 2026-06-12
         */
        private void delete(String id) {
            configs.removeIf(config -> config.id().equals(id));
        }

        /**
         * 查询指定配置。
         *
         * @param id 配置 ID
         * @return LLM 配置
         * @author lvdaxianerplus
         * @date 2026-06-12
         */
        private LlmMarkdownConfig require(String id) {
            return configs.stream().filter(config -> config.id().equals(id)).findFirst().orElseThrow();
        }
    }
}
