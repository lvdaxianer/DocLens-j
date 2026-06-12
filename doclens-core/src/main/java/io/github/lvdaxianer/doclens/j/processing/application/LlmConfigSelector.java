package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfig;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfigRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmUsageType;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * LLM 配置选择器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
public class LlmConfigSelector {

    private final LlmMarkdownConfigRepository repository;

    /**
     * 创建 LLM 配置选择器。
     *
     * @param repository LLM 配置仓储
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public LlmConfigSelector(LlmMarkdownConfigRepository repository) {
        this.repository = repository;
    }

    /**
     * 按用途选择可用 LLM 配置。
     *
     * @param usageType 配置用途
     * @return 可用 LLM 配置
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public Optional<LlmMarkdownConfig> select(LlmUsageType usageType) {
        List<LlmMarkdownConfig> configs = availableConfigs(usageType);
        Optional<LlmMarkdownConfig> defaultConfig = defaultConfig(configs);
        if (defaultConfig.isPresent()) {
            // 默认配置可用时优先使用默认配置。
            return defaultConfig;
        } else {
            // 没有可用默认配置时，按优先级选择第一个健康配置。
            return firstByPriority(configs);
        }
    }

    /**
     * 查询指定用途的可用配置。
     *
     * @param usageType 配置用途
     * @return 可用配置列表
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private List<LlmMarkdownConfig> availableConfigs(LlmUsageType usageType) {
        return repository.listByUsage(usageType).stream()
                .filter(this::isSelectable)
                .sorted(Comparator.comparingInt(LlmMarkdownConfig::priority))
                .toList();
    }

    /**
     * 判断配置是否可选择。
     *
     * @param config LLM 配置
     * @return 是否可选择
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private boolean isSelectable(LlmMarkdownConfig config) {
        return config.isAvailableForPostProcessing() && config.healthy();
    }

    /**
     * 查询可用默认配置。
     *
     * @param configs 可用配置列表
     * @return 默认配置
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private Optional<LlmMarkdownConfig> defaultConfig(List<LlmMarkdownConfig> configs) {
        return configs.stream().filter(LlmMarkdownConfig::defaultConfig).findFirst();
    }

    /**
     * 选择优先级最高的可用配置。
     *
     * @param configs 可用配置列表
     * @return 优先级最高的配置
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private Optional<LlmMarkdownConfig> firstByPriority(List<LlmMarkdownConfig> configs) {
        return configs.stream().findFirst();
    }
}
