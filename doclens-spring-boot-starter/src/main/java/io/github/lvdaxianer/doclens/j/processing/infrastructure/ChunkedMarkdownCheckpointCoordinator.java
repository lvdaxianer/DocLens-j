package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunk;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkCheckpoint;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkCheckpointPlan;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkCheckpointStore;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessingRequest;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 分片 Markdown checkpoint 协调器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
final class ChunkedMarkdownCheckpointCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkedMarkdownCheckpointCoordinator.class);

    private final MarkdownChunkCheckpointStore checkpointStore;
    private final ExecutorService checkpointExecutor;

    /**
     * 创建分片 Markdown checkpoint 协调器。
     *
     * @param checkpointStore chunk checkpoint 存储
     * @param checkpointExecutor checkpoint 写入执行器
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    ChunkedMarkdownCheckpointCoordinator(
            MarkdownChunkCheckpointStore checkpointStore,
            ExecutorService checkpointExecutor
    ) {
        this.checkpointStore = checkpointStore;
        this.checkpointExecutor = checkpointExecutor;
    }

    /**
     * 加载已有 chunk checkpoint。
     *
     * @param checkpointPlan checkpoint 计划
     * @param chunk Markdown 分片
     * @return checkpoint 分片结果
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    Optional<ChunkedMarkdownChunkResult> load(MarkdownChunkCheckpointPlan checkpointPlan, MarkdownChunk chunk) {
        return checkpointStore.load(checkpointPlan, chunk)
                .map(markdown -> ChunkedMarkdownChunkResult.completed(chunk.chunkIndex(), markdown, true));
    }

    /**
     * 为成功分片安排 checkpoint 保存。
     *
     * @param checkpointPlan checkpoint 计划
     * @param chunk Markdown 分片
     * @param result 分片结果
     * @return 附加 checkpoint future 的分片结果
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    ChunkedMarkdownChunkResult scheduleSave(
            MarkdownChunkCheckpointPlan checkpointPlan,
            MarkdownChunk chunk,
            ChunkedMarkdownChunkResult result
    ) {
        return result.withCheckpointSave(checkpointSave(checkpointPlan, chunk, result));
    }

    /**
     * 等待本轮 checkpoint 写入完成。
     *
     * @param request 原始 Markdown 后处理请求
     * @param results 分片处理结果
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    void awaitSaves(MarkdownPostProcessingRequest request, List<ChunkedMarkdownChunkResult> results) {
        for (ChunkedMarkdownChunkResult result : results) {
            awaitSave(request, result);
        }
    }

    /**
     * 创建 checkpoint 保存 future。
     *
     * @param checkpointPlan checkpoint 计划
     * @param chunk Markdown 分片
     * @param result 分片结果
     * @return checkpoint 保存 future
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private CompletableFuture<Void> checkpointSave(
            MarkdownChunkCheckpointPlan checkpointPlan,
            MarkdownChunk chunk,
            ChunkedMarkdownChunkResult result
    ) {
        if (result.markdownApplied()) {
            return CompletableFuture.runAsync(() -> checkpointStore.save(
                    new MarkdownChunkCheckpoint(checkpointPlan, chunk, result.markdown(), true)), checkpointExecutor);
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * 等待单个 checkpoint 保存完成。
     *
     * @param request 原始 Markdown 后处理请求
     * @param result 分片处理结果
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private void awaitSave(MarkdownPostProcessingRequest request, ChunkedMarkdownChunkResult result) {
        try {
            result.checkpointSave().join();
        } catch (CompletionException ex) {
            logCheckpointFailure(request, result, ex);
        }
    }

    /**
     * 记录 checkpoint 写入失败日志。
     *
     * @param request 原始 Markdown 后处理请求
     * @param result 分片处理结果
     * @param ex 写入异常
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private void logCheckpointFailure(
            MarkdownPostProcessingRequest request,
            ChunkedMarkdownChunkResult result,
            CompletionException ex
    ) {
        Throwable cause = ex.getCause() == null ? ex : ex.getCause();
        LOGGER.warn("[LLM Markdown 分片] checkpoint 写入失败 documentId={}, chunkIndex={}, error={}",
                request.documentId(), result.chunkIndex(), cause.getClass().getSimpleName(), cause);
    }
}
