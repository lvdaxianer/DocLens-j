package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * 分片 Markdown 运行时选项。
 *
 * @param chunkExecutor 分片执行器
 * @param checkpointExecutor checkpoint 写入执行器
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
public record ChunkedMarkdownRuntimeOptions(
        ExecutorService chunkExecutor,
        ExecutorService checkpointExecutor
) {

    /**
     * 规整运行时执行器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    public ChunkedMarkdownRuntimeOptions {
        chunkExecutor = Objects.requireNonNull(chunkExecutor, "markdown chunk executor is required");
        checkpointExecutor = checkpointExecutor == null ? chunkExecutor : checkpointExecutor;
    }
}
