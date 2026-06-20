package io.github.lvdaxianer.doclens.j.processing.application;

/**
 * Markdown chunk 检查点内容。
 *
 * @param plan checkpoint 计划
 * @param chunk Markdown chunk
 * @param markdown chunk Markdown
 * @param markdownApplied 是否应用 LLM Markdown
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
public record MarkdownChunkCheckpoint(
        MarkdownChunkCheckpointPlan plan,
        MarkdownChunk chunk,
        String markdown,
        boolean markdownApplied
) {

    /**
     * 规整 checkpoint 内容。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    public MarkdownChunkCheckpoint {
        // checkpoint 计划缺失时无法定位磁盘目录，直接拒绝。
        if (plan == null) {
            throw new IllegalArgumentException("checkpoint plan must not be null");
        // chunk 缺失时无法定位有序编号，直接拒绝。
        } else if (chunk == null) {
            throw new IllegalArgumentException("markdown chunk must not be null");
        }
        markdown = markdown == null ? "" : markdown;
    }
}
