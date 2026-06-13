package io.github.lvdaxianer.doclens.j.processing.application;

/**
 * Markdown 后处理分片。
 *
 * @param chunkIndex 分片序号
 * @param totalChunks 分片总数
 * @param previousContext 上文重叠，仅用于模型理解上下文
 * @param mainContent 当前分片主内容
 * @param nextContext 下文重叠，仅用于模型理解上下文
 * @param estimatedTokens 主内容估算 Token 数
 * @author lvdaxianerplus
 * @date 2026-06-13
 */
public record MarkdownChunk(
        int chunkIndex,
        int totalChunks,
        String previousContext,
        String mainContent,
        String nextContext,
        int estimatedTokens
) {

    /**
     * 规整分片可空文本。
     *
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    public MarkdownChunk {
        previousContext = previousContext == null ? "" : previousContext;
        mainContent = mainContent == null ? "" : mainContent;
        nextContext = nextContext == null ? "" : nextContext;
    }
}
