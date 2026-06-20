package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunk;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkCheckpointPlan;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessingRequest;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessingResult;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessor;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 单个 Markdown 分片处理 worker。
 *
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
final class ChunkedMarkdownChunkWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkedMarkdownChunkWorker.class);
    private static final int CHUNK_MAX_ATTEMPTS = 3;
    private static final int CHUNK_RETRY_DELAY_MILLIS = 100;

    private final MarkdownPostProcessor delegate;
    private final ObjectMapper objectMapper;
    private final ChunkedMarkdownCheckpointCoordinator checkpointCoordinator;

    /**
     * 创建单个 Markdown 分片处理 worker。
     *
     * @param delegate 实际 LLM 后处理器
     * @param objectMapper JSON 映射器
     * @param checkpointCoordinator checkpoint 协调器
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    ChunkedMarkdownChunkWorker(
            MarkdownPostProcessor delegate,
            ObjectMapper objectMapper,
            ChunkedMarkdownCheckpointCoordinator checkpointCoordinator
    ) {
        this.delegate = delegate;
        this.objectMapper = objectMapper;
        this.checkpointCoordinator = checkpointCoordinator;
    }

    /**
     * 处理单个分片。
     *
     * @param request 原始 Markdown 后处理请求
     * @param chunk Markdown 分片
     * @param checkpointPlan checkpoint 计划
     * @return 分片结果
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    ChunkedMarkdownChunkResult process(
            MarkdownPostProcessingRequest request,
            MarkdownChunk chunk,
            MarkdownChunkCheckpointPlan checkpointPlan
    ) {
        return checkpointCoordinator.load(checkpointPlan, chunk)
                .orElseGet(() -> processMissingCheckpoint(request, chunk, checkpointPlan));
    }

    /**
     * 处理缺少 checkpoint 的分片。
     *
     * @param request 原始 Markdown 后处理请求
     * @param chunk Markdown 分片
     * @param checkpointPlan checkpoint 计划
     * @return 分片结果
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private ChunkedMarkdownChunkResult processMissingCheckpoint(
            MarkdownPostProcessingRequest request,
            MarkdownChunk chunk,
            MarkdownChunkCheckpointPlan checkpointPlan
    ) {
        for (int attempt = 1; attempt <= CHUNK_MAX_ATTEMPTS; attempt++) {
            try {
                return checkpointCoordinator.scheduleSave(checkpointPlan, chunk, processOnce(request, chunk));
            } catch (IOException | RuntimeException ex) {
                if (attempt < CHUNK_MAX_ATTEMPTS) {
                    logChunkRetry(request, chunk, attempt, ex);
                    sleepBeforeRetry();
                } else {
                    return fallbackChunk(request, chunk, ex);
                }
            }
        }
        return fallbackChunk(request, chunk, new IllegalStateException("unreachable"));
    }

    /**
     * 调用 LLM 处理分片一次。
     *
     * @param request 原始 Markdown 后处理请求
     * @param chunk Markdown 分片
     * @return 分片结果
     * @throws IOException 分片提示词构建失败
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private ChunkedMarkdownChunkResult processOnce(MarkdownPostProcessingRequest request, MarkdownChunk chunk)
            throws IOException {
        MarkdownPostProcessingRequest chunkRequest = chunkRequest(request, chunk);
        MarkdownPostProcessingResult result = delegate.process(chunkRequest);
        return ChunkedMarkdownChunkResult.completed(chunk.chunkIndex(), result.markdown(), result.markdownApplied());
    }

    /**
     * 记录分片重试日志。
     *
     * @param request 原始 Markdown 后处理请求
     * @param chunk Markdown 分片
     * @param attempt 当前尝试次数
     * @param ex 失败异常
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private void logChunkRetry(
            MarkdownPostProcessingRequest request,
            MarkdownChunk chunk,
            int attempt,
            Exception ex
    ) {
        LOGGER.warn("[LLM Markdown 分片] 分片失败，准备重试 documentId={}, chunkIndex={}, attempt={}, error={}",
                request.documentId(), chunk.chunkIndex(), attempt, ex.getClass().getSimpleName());
    }

    /**
     * 重试前短暂停顿。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private void sleepBeforeRetry() {
        try {
            Thread.sleep(CHUNK_RETRY_DELAY_MILLIS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("llm markdown chunk retry interrupted", ex);
        }
    }

    /**
     * 构建分片回退结果。
     *
     * @param request 原始 Markdown 后处理请求
     * @param chunk Markdown 分片
     * @param ex 失败异常
     * @return 回退结果
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private ChunkedMarkdownChunkResult fallbackChunk(
            MarkdownPostProcessingRequest request,
            MarkdownChunk chunk,
            Exception ex
    ) {
        LOGGER.warn("[LLM Markdown 分片] 分片重试耗尽，回退原文 documentId={}, chunkIndex={}, error={}",
                request.documentId(), chunk.chunkIndex(), ex.getClass().getSimpleName());
        return ChunkedMarkdownChunkResult.completed(chunk.chunkIndex(), chunk.mainContent(), false);
    }

    /**
     * 创建分片请求。
     *
     * @param request 原始 Markdown 后处理请求
     * @param chunk Markdown 分片
     * @return 分片请求
     * @throws IOException 元数据序列化失败
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private MarkdownPostProcessingRequest chunkRequest(MarkdownPostProcessingRequest request, MarkdownChunk chunk)
            throws IOException {
        String chunkPrompt = MarkdownPrompt.chunkUserPrompt(objectMapper, request, chunk);
        return new MarkdownPostProcessingRequest(request.documentId(), request.fileName(), request.metadata(),
                chunkPrompt, request.chunkStrategy());
    }
}
