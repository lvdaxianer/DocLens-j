package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import java.util.concurrent.CompletableFuture;

/**
 * 分片 Markdown 处理结果。
 *
 * @param chunkIndex 分片序号
 * @param markdown 分片 Markdown
 * @param markdownApplied 是否应用了 Markdown
 * @param checkpointSave checkpoint 写入 future
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
record ChunkedMarkdownChunkResult(
        int chunkIndex,
        String markdown,
        boolean markdownApplied,
        CompletableFuture<Void> checkpointSave
) {

    /**
     * 创建已完成 checkpoint future 的分片结果。
     *
     * @param chunkIndex 分片序号
     * @param markdown 分片 Markdown
     * @param markdownApplied 是否应用了 Markdown
     * @return 分片处理结果
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    static ChunkedMarkdownChunkResult completed(int chunkIndex, String markdown, boolean markdownApplied) {
        return new ChunkedMarkdownChunkResult(chunkIndex, markdown, markdownApplied,
                CompletableFuture.completedFuture(null));
    }

    /**
     * 附加 checkpoint 写入 future。
     *
     * @param checkpointSave checkpoint 写入 future
     * @return 更新后的分片结果
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    ChunkedMarkdownChunkResult withCheckpointSave(CompletableFuture<Void> checkpointSave) {
        return new ChunkedMarkdownChunkResult(chunkIndex, markdown, markdownApplied, checkpointSave);
    }
}
