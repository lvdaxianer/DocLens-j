package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkCheckpointStore;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * 可配置 Markdown 运行时选项。
 *
 * @param chunkExecutor 分片执行器
 * @param checkpointExecutor checkpoint 写入执行器
 * @param checkpointStore chunk checkpoint 存储
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
public record ConfigurableMarkdownRuntimeOptions(
        ExecutorService chunkExecutor,
        ExecutorService checkpointExecutor,
        MarkdownChunkCheckpointStore checkpointStore
) {

    /**
     * 创建兼容旧调用方的运行时选项。
     *
     * @param chunkExecutor 分片执行器
     * @param checkpointStore chunk checkpoint 存储
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    public ConfigurableMarkdownRuntimeOptions(
            ExecutorService chunkExecutor,
            MarkdownChunkCheckpointStore checkpointStore
    ) {
        this(chunkExecutor, chunkExecutor, checkpointStore);
    }

    /**
     * 规整可选字段。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    public ConfigurableMarkdownRuntimeOptions {
        chunkExecutor = Objects.requireNonNull(chunkExecutor, "markdown chunk executor is required");
        checkpointExecutor = checkpointExecutor == null ? chunkExecutor : checkpointExecutor;
        checkpointStore = checkpointStore == null ? MarkdownChunkCheckpointStore.noop() : checkpointStore;
    }
}
