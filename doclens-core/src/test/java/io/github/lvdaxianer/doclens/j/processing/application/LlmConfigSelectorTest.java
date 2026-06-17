package io.github.lvdaxianer.doclens.j.processing.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownApiType;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfig;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfigBuilder;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfigRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmUsageType;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * LLM 配置选择器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
class LlmConfigSelectorTest {

    private static final OffsetDateTime CHECKED_AT = OffsetDateTime.parse("2026-06-12T12:00:00+08:00");

    /**
     * 首次选择应命中稳定排序池中的第一个健康配置。
     *
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void selectsFirstHealthyConfigFromStableRoundRobinPool() {
        LlmConfigSelector selector = new LlmConfigSelector(new InMemoryConfigRepository(List.of(
                healthyConfig("backup", 1),
                defaultConfig("default", 20, true))));

        Optional<LlmMarkdownConfig> selected = selector.select(LlmUsageType.MARKDOWN_POST_PROCESSING);

        assertThat(selected).get().extracting(LlmMarkdownConfig::id).isEqualTo("backup");
    }

    /**
     * 默认配置不可用时应选择优先级最小的健康配置。
     *
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void selectsLowestPriorityHealthyConfigWhenDefaultIsUnhealthy() {
        LlmConfigSelector selector = new LlmConfigSelector(new InMemoryConfigRepository(List.of(
                defaultConfig("default", 1, false),
                healthyConfig("slow", 30),
                healthyConfig("fast", 10))));

        Optional<LlmMarkdownConfig> selected = selector.select(LlmUsageType.MARKDOWN_POST_PROCESSING);

        assertThat(selected).get().extracting(LlmMarkdownConfig::id).isEqualTo("fast");
    }

    /**
     * 没有健康可用配置时应返回空。
     *
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void returnsEmptyWhenNoEnabledHealthyConfigExists() {
        LlmConfigSelector selector = new LlmConfigSelector(new InMemoryConfigRepository(List.of(
                disabledDefaultConfig("disabled", 1),
                unhealthyConfig("unhealthy", 2))));

        Optional<LlmMarkdownConfig> selected = selector.select(LlmUsageType.MARKDOWN_POST_PROCESSING);

        assertThat(selected).isEmpty();
    }

    /**
     * 没有任何配置时应返回空且不抛异常。
     *
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void returnsEmptyWhenNoConfigExists() {
        LlmConfigSelector selector = new LlmConfigSelector(new InMemoryConfigRepository(List.of()));

        Optional<LlmMarkdownConfig> selected = selector.select(LlmUsageType.MARKDOWN_POST_PROCESSING);

        assertThat(selected).isEmpty();
    }

    /**
     * 创建默认测试配置。
     *
     * @param id 配置 ID
     * @param priority 优先级
     * @param healthy 是否健康
     * @return LLM 配置
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private LlmMarkdownConfig defaultConfig(String id, int priority, boolean healthy) {
        return baseConfig(id, priority).defaultConfig(true).enabled(true).healthy(healthy).build();
    }

    /**
     * 创建健康备选测试配置。
     *
     * @param id 配置 ID
     * @param priority 优先级
     * @return LLM 配置
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private LlmMarkdownConfig healthyConfig(String id, int priority) {
        return baseConfig(id, priority).defaultConfig(false).enabled(true).healthy(true).build();
    }

    /**
     * 创建停用的默认测试配置。
     *
     * @param id 配置 ID
     * @param priority 优先级
     * @return LLM 配置
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private LlmMarkdownConfig disabledDefaultConfig(String id, int priority) {
        return baseConfig(id, priority).defaultConfig(true).enabled(false).healthy(true).build();
    }

    /**
     * 创建不健康备选测试配置。
     *
     * @param id 配置 ID
     * @param priority 优先级
     * @return LLM 配置
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private LlmMarkdownConfig unhealthyConfig(String id, int priority) {
        return baseConfig(id, priority).defaultConfig(false).enabled(true).healthy(false).build();
    }

    /**
     * 创建测试配置基础构建器。
     *
     * @param id 配置 ID
     * @param priority 优先级
     * @return 配置构建器
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private LlmMarkdownConfigBuilder baseConfig(String id, int priority) {
        return LlmMarkdownConfig.builder(id, id, LlmMarkdownApiType.OPENAI)
                .endpoint("https://llm.example.com/v1/chat/completions", "markdown-model")
                .credential("sk-test")
                .usage(LlmUsageType.MARKDOWN_POST_PROCESSING, priority)
                .healthMessage("")
                .lastHealthAt(Optional.of(CHECKED_AT))
                .createdAt(CHECKED_AT)
                .updatedAt(CHECKED_AT);
    }

    /**
     * 内存配置仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private static final class InMemoryConfigRepository implements LlmMarkdownConfigRepository {

        private final List<LlmMarkdownConfig> configs;

        /**
         * 创建内存配置仓储。
         *
         * @param configs 初始配置列表
         * @author lvdaxianerplus
         * @date 2026-06-12
         */
        private InMemoryConfigRepository(List<LlmMarkdownConfig> configs) {
            this.configs = new ArrayList<>(configs);
        }

        /**
         * 查询第一条测试配置。
         *
         * @return 第一条测试配置
         * @author lvdaxianerplus
         * @date 2026-06-12
         */
        @Override
        public Optional<LlmMarkdownConfig> find() {
            return configs.stream().findFirst();
        }

        /**
         * 按用途查询测试配置。
         *
         * @param usageType 配置用途
         * @return 配置列表
         * @author lvdaxianerplus
         * @date 2026-06-12
         */
        @Override
        public List<LlmMarkdownConfig> listByUsage(LlmUsageType usageType) {
            return configs.stream().filter(config -> config.usageType() == usageType).toList();
        }

        /**
         * 保存测试配置。
         *
         * @param config LLM 配置
         * @author lvdaxianerplus
         * @date 2026-06-12
         */
        @Override
        public void save(LlmMarkdownConfig config) {
            configs.removeIf(current -> current.id().equals(config.id()));
            configs.add(config);
        }

        /**
         * 批量保存测试配置。
         *
         * @param configs LLM 配置列表
         * @author lvdaxianerplus
         * @date 2026-06-12
         */
        @Override
        public void saveAll(List<LlmMarkdownConfig> configs) {
            this.configs.clear();
            this.configs.addAll(configs);
        }
    }
}
