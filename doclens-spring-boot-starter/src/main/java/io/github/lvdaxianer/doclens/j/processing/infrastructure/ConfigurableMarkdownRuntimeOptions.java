package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkCheckpointStore;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * 可配置 Markdown 运行时选项。
 *
 * @param chunkExecutor 分片执行器
 * @param checkpointStore chunk checkpoint 存储
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
public record ConfigurableMarkdownRuntimeOptions(
        ExecutorService chunkExecutor,
        MarkdownChunkCheckpointStore checkpointStore
) {

    /**
     * 规整可选字段。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    public ConfigurableMarkdownRuntimeOptions {
        chunkExecutor = Objects.requireNonNull(chunkExecutor, "markdown chunk executor is required");
        checkpointStore = checkpointStore == null ? MarkdownChunkCheckpointStore.noop() : checkpointStore;
    }
}
