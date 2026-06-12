package io.github.lvdaxianer.doclens.j.processing.domain;

import java.util.Optional;
import java.util.List;

/**
 * LLM Markdown 配置仓储。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public interface LlmMarkdownConfigRepository {

    /**
     * 查询当前 LLM Markdown 配置。
     *
     * @return 当前配置
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    Optional<LlmMarkdownConfig> find();

    /**
     * 查询全部 LLM Markdown 配置。
     *
     * @return 配置列表
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    default List<LlmMarkdownConfig> listConfigs() {
        return find().map(List::of).orElseGet(List::of);
    }

    /**
     * 按用途查询 LLM Markdown 配置。
     *
     * @param usageType 配置用途
     * @return 配置列表
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    default List<LlmMarkdownConfig> listByUsage(LlmUsageType usageType) {
        return listConfigs().stream().filter(config -> config.usageType() == usageType).toList();
    }

    /**
     * 按 ID 查询 LLM Markdown 配置。
     *
     * @param id 配置 ID
     * @return 配置
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    default Optional<LlmMarkdownConfig> findById(String id) {
        return listConfigs().stream().filter(config -> config.id().equals(id)).findFirst();
    }

    /**
     * 保存 LLM Markdown 配置。
     *
     * @param config LLM Markdown 配置
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    void save(LlmMarkdownConfig config);

    /**
     * 批量保存 LLM Markdown 配置。
     *
     * @param configs 配置列表
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    void saveAll(List<LlmMarkdownConfig> configs);

    /**
     * 删除 LLM Markdown 配置。
     *
     * @param id 配置 ID
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    default void deleteById(String id) {
        throw new UnsupportedOperationException("llm markdown config delete is not supported");
    }

    /**
     * 更新 LLM Markdown 健康状态。
     *
     * @param config 含最新健康状态的 LLM Markdown 配置
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    default void updateHealth(LlmMarkdownConfig config) {
        save(config);
    }
}
