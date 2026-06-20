package io.github.lvdaxianer.doclens.j.processing.application;

import java.util.Optional;

/**
 * Markdown chunk 检查点存储。
 *
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
public interface MarkdownChunkCheckpointStore {

    /**
     * 空 checkpoint 存储。
     *
     * @return 空 checkpoint 存储
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    static MarkdownChunkCheckpointStore noop() {
        return NoopMarkdownChunkCheckpointStore.INSTANCE;
    }

    /**
     * 保存 chunk 检查点。
     *
     * @param checkpoint chunk checkpoint 内容
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    void save(MarkdownChunkCheckpoint checkpoint);

    /**
     * 读取 chunk 检查点 Markdown。
     *
     * @param plan checkpoint 计划
     * @param chunk Markdown chunk
     * @return 已保存 Markdown
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    Optional<String> load(MarkdownChunkCheckpointPlan plan, MarkdownChunk chunk);

    /**
     * 空 Markdown chunk checkpoint 存储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    enum NoopMarkdownChunkCheckpointStore implements MarkdownChunkCheckpointStore {
        INSTANCE;

        /**
         * 忽略 checkpoint 保存。
         *
         * @param checkpoint chunk checkpoint 内容
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        @Override
        public void save(MarkdownChunkCheckpoint checkpoint) {
        }

        /**
         * 空存储永远没有可复用 checkpoint。
         *
         * @param plan checkpoint 计划
         * @param chunk Markdown chunk
         * @return 空结果
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        @Override
        public Optional<String> load(MarkdownChunkCheckpointPlan plan, MarkdownChunk chunk) {
            return Optional.empty();
        }
    }
}
