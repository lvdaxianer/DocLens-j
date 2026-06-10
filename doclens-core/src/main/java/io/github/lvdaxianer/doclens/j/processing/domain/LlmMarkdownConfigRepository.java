package io.github.lvdaxianer.doclens.j.processing.domain;

import java.util.Optional;

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
     * 保存 LLM Markdown 配置。
     *
     * @param config LLM Markdown 配置
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    void save(LlmMarkdownConfig config);

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
