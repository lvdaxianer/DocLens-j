package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfig;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfigRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmUsageType;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * LLM 配置选择器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
public class LlmConfigSelector {

    private final LlmMarkdownConfigRepository repository;
    private final ConcurrentMap<LlmUsageType, AtomicLong> cursors = new ConcurrentHashMap<>();

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
        if (configs.isEmpty()) {
            // 无健康可用配置时返回空，由调用方保持 OCR 原文直通。
            return Optional.empty();
        } else {
            // 多个健康配置按稳定排序池轮询，避免默认配置长期独占流量。
            return Optional.of(roundRobin(usageType, configs));
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
                .sorted(Comparator.comparingInt(LlmMarkdownConfig::priority)
                        .thenComparing(LlmMarkdownConfig::id))
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
     * 从可用配置池轮询选择一个配置。
     *
     * @param usageType 配置用途
     * @param configs 可用配置列表
     * @return 本次选中的配置
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private LlmMarkdownConfig roundRobin(LlmUsageType usageType, List<LlmMarkdownConfig> configs) {
        AtomicLong cursor = cursors.computeIfAbsent(usageType, ignored -> new AtomicLong());
        int selectedIndex = Math.floorMod(cursor.getAndIncrement(), configs.size());
        return configs.get(selectedIndex);
    }
}
