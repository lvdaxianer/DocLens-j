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
 * LLM 配置轮询选择器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-13
 */
class LlmConfigSelectorRoundRobinTest {

    private static final int FIRST_PRIORITY = 10;
    private static final int SECOND_PRIORITY = 20;

    /**
     * 多个健康配置应按排序池轮询选择。
     *
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    @Test
    void selectsHealthyConfigsByRoundRobin() {
        InMemoryConfigRepository repository = new InMemoryConfigRepository();
        repository.save(config("llm-a", FIRST_PRIORITY, false));
        repository.save(config("llm-b", SECOND_PRIORITY, true));
        LlmConfigSelector selector = new LlmConfigSelector(repository);

        assertThat(selector.select(LlmUsageType.MARKDOWN_POST_PROCESSING)).get()
                .extracting(LlmMarkdownConfig::id).isEqualTo("llm-a");
        assertThat(selector.select(LlmUsageType.MARKDOWN_POST_PROCESSING)).get()
                .extracting(LlmMarkdownConfig::id).isEqualTo("llm-b");
        assertThat(selector.select(LlmUsageType.MARKDOWN_POST_PROCESSING)).get()
                .extracting(LlmMarkdownConfig::id).isEqualTo("llm-a");
    }

    /**
     * 轮询池应跳过已禁用配置。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Test
    void skipsDisabledConfigsDuringRoundRobin() {
        InMemoryConfigRepository repository = new InMemoryConfigRepository();
        repository.save(config("llm-a", FIRST_PRIORITY, false).withEnabled(false));
        repository.save(config("llm-b", SECOND_PRIORITY, true));
        LlmConfigSelector selector = new LlmConfigSelector(repository);

        assertThat(selector.select(LlmUsageType.MARKDOWN_POST_PROCESSING)).get()
                .extracting(LlmMarkdownConfig::id).isEqualTo("llm-b");
        assertThat(selector.select(LlmUsageType.MARKDOWN_POST_PROCESSING)).get()
                .extracting(LlmMarkdownConfig::id).isEqualTo("llm-b");
    }

    /**
     * 创建健康 LLM 配置。
     *
     * @param id 配置 ID
     * @param priority 优先级
     * @param defaultConfig 是否默认配置
     * @return 健康 LLM 配置
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private LlmMarkdownConfig config(String id, int priority, boolean defaultConfig) {
        return LlmMarkdownConfig.builder(id, "配置-" + id, LlmMarkdownApiType.OPENAI)
                .endpoint("https://llm.example.com/v1/chat/completions", "markdown-model")
                .credential("MINIMAX_API_KEY")
                .usage(LlmUsageType.MARKDOWN_POST_PROCESSING, priority)
                .defaultConfig(defaultConfig)
                .enabled(true)
                .healthy(true)
                .build();
    }

    /**
     * 内存配置仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private static final class InMemoryConfigRepository implements LlmMarkdownConfigRepository {

        private final List<LlmMarkdownConfig> configs = new ArrayList<>();

        /**
         * 查询默认配置。
         *
         * @return 默认配置
         * @author lvdaxianerplus
         * @date 2026-06-13
         */
        @Override
        public Optional<LlmMarkdownConfig> find() {
            return configs.stream().findFirst();
        }

        /**
         * 按用途查询配置。
         *
         * @param usageType 配置用途
         * @return 配置列表
         * @author lvdaxianerplus
         * @date 2026-06-13
         */
        @Override
        public List<LlmMarkdownConfig> listByUsage(LlmUsageType usageType) {
            return configs.stream().filter(config -> config.usageType() == usageType).toList();
        }

        /**
         * 保存配置。
         *
         * @param config LLM Markdown 配置
         * @author lvdaxianerplus
         * @date 2026-06-13
         */
        @Override
        public void save(LlmMarkdownConfig config) {
            configs.add(config);
        }

        /**
         * 批量保存配置。
         *
         * @param configs 配置列表
         * @author lvdaxianerplus
         * @date 2026-06-13
         */
        @Override
        public void saveAll(List<LlmMarkdownConfig> configs) {
            this.configs.addAll(configs);
        }
    }
}
