package io.github.lvdaxianer.doclens.j.processing.application;

import java.util.List;

/**
 * Markdown 后处理分片计划。
 *
 * @param chunked 是否发生分片
 * @param estimatedInputTokens 输入估算 Token 数
 * @param contentBudgetTokens 主内容 Token 预算
 * @param overlapTokens 上下文重叠 Token 预算
 * @param chunks 有序分片列表
 * @author lvdaxianerplus
 * @date 2026-06-13
 */
public record MarkdownChunkPlan(
        boolean chunked,
        int estimatedInputTokens,
        int contentBudgetTokens,
        int overlapTokens,
        List<MarkdownChunk> chunks
) {

    /**
     * 规整分片集合。
     *
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    public MarkdownChunkPlan {
        chunks = chunks == null ? List.of() : List.copyOf(chunks);
    }
}
