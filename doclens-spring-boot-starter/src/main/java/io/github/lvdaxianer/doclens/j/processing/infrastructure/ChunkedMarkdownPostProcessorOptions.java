package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkCheckpointStore;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunker;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessor;
import java.util.concurrent.ExecutorService;

/**
 * 分片 Markdown 后处理器选项。
 *
 * @param delegate 实际 LLM 后处理器
 * @param chunker Markdown 分片器
 * @param maxContextTokens 最大上下文 Token 数
 * @param checkpointStore chunk checkpoint 存储
 * @param runtimeOptions 运行时选项
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
public record ChunkedMarkdownPostProcessorOptions(
        MarkdownPostProcessor delegate,
        MarkdownChunker chunker,
        int maxContextTokens,
        MarkdownChunkCheckpointStore checkpointStore,
        ChunkedMarkdownRuntimeOptions runtimeOptions
) {

    /**
     * 创建分片 Markdown 后处理器选项。
     *
     * @param delegate 实际 LLM 后处理器
     * @param chunker Markdown 分片器
     * @param maxContextTokens 最大上下文 Token 数
     * @param chunkExecutor 分片执行器
     * @param checkpointStore chunk checkpoint 存储
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    public ChunkedMarkdownPostProcessorOptions(
            MarkdownPostProcessor delegate,
            MarkdownChunker chunker,
            int maxContextTokens,
            ExecutorService chunkExecutor,
            MarkdownChunkCheckpointStore checkpointStore
    ) {
        this(delegate, chunker, maxContextTokens, checkpointStore,
                new ChunkedMarkdownRuntimeOptions(chunkExecutor, chunkExecutor));
    }

    /**
     * 规整可选字段。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    public ChunkedMarkdownPostProcessorOptions {
        checkpointStore = checkpointStore == null ? MarkdownChunkCheckpointStore.noop() : checkpointStore;
    }
}
