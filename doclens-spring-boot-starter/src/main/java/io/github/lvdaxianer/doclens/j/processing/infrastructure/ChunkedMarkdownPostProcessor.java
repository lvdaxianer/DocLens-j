package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunk;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkCheckpoint;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkCheckpointPlan;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkCheckpointPlanSource;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkCheckpointStore;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkPlan;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunker;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessingRequest;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessingResult;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 支持大文本分片的 Markdown 后处理器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-13
 */
public final class ChunkedMarkdownPostProcessor implements MarkdownPostProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkedMarkdownPostProcessor.class);
    private static final String CHUNK_FAILED_WARNING = "llm_markdown_chunk_failed";
    private static final String CHUNK_SEPARATOR = "\n\n";
    private static final String LLM_CHUNKED_FIELD = "llm_chunked";
    private static final String LLM_CHUNK_COUNT_FIELD = "llm_chunk_count";
    private static final String LLM_MAX_CONTEXT_TOKENS_FIELD = "llm_max_context_tokens";
    private static final String LLM_ESTIMATED_OCR_TOKENS_FIELD = "llm_estimated_ocr_tokens";
    private static final int CHUNK_MAX_ATTEMPTS = 3;
    private static final int CHUNK_RETRY_DELAY_MILLIS = 100;

    private final MarkdownPostProcessor delegate;
    private final MarkdownChunker chunker;
    private final int maxContextTokens;
    private final ExecutorService chunkExecutor;
    private final ExecutorService checkpointExecutor;
    private final ObjectMapper objectMapper;
    private final MarkdownChunkCheckpointStore checkpointStore;

    /**
     * 创建分片 Markdown 后处理器。
     *
     * @param delegate 实际 LLM 后处理器
     * @param chunker Markdown 分片器
     * @param maxContextTokens 最大上下文 Token 数
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    public ChunkedMarkdownPostProcessor(
            MarkdownPostProcessor delegate,
            MarkdownChunker chunker,
            int maxContextTokens,
            ExecutorService chunkExecutor
    ) {
        this(new ChunkedMarkdownPostProcessorOptions(delegate, chunker, maxContextTokens, chunkExecutor,
                MarkdownChunkCheckpointStore.noop()));
    }

    /**
     * 创建分片 Markdown 后处理器。
     *
     * @param delegate 实际 LLM 后处理器
     * @param chunker Markdown 分片器
     * @param maxContextTokens 最大上下文 Token 数
     * @param chunkExecutor 分片执行器
     * @param checkpointStore chunk checkpoint 存储
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    public ChunkedMarkdownPostProcessor(ChunkedMarkdownPostProcessorOptions options) {
        this.delegate = options.delegate();
        this.chunker = options.chunker();
        this.maxContextTokens = options.maxContextTokens();
        this.chunkExecutor = options.runtimeOptions().chunkExecutor();
        this.checkpointExecutor = options.runtimeOptions().checkpointExecutor();
        this.objectMapper = new ObjectMapper();
        this.checkpointStore = options.checkpointStore();
    }

    /**
     * 执行分片 Markdown 后处理。
     *
     * @param request Markdown 后处理请求
     * @return Markdown 后处理结果
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    @Override
    public MarkdownPostProcessingResult process(MarkdownPostProcessingRequest request) {
        MarkdownChunkPlan plan = chunker.plan(request.ocrText(), maxContextTokens, request.chunkStrategy());
        if (plan.chunked()) {
            // 大文本走分片处理，避免单次 LLM 请求超过上下文窗口。
            return processChunks(request, plan);
        } else {
            // 小文本保持原调用路径，避免引入额外提示词噪音。
            return withChunkMetadata(delegate.process(request), plan);
        }
    }

    /**
     * 顺序处理所有分片。
     *
     * @param request 原始 Markdown 后处理请求
     * @param plan 分片计划
     * @return Markdown 后处理结果
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private MarkdownPostProcessingResult processChunks(MarkdownPostProcessingRequest request, MarkdownChunkPlan plan) {
        MarkdownChunkCheckpointPlan checkpointPlan = MarkdownChunkCheckpointPlan.from(
                new MarkdownChunkCheckpointPlanSource(request, plan, maxContextTokens));
        List<CompletableFuture<ChunkResult>> futures = submitChunkFutures(request, plan, checkpointPlan);
        List<ChunkResult> results = awaitChunkResults(futures);
        awaitCheckpointSaves(results);
        return joinChunkResults(plan, results);
    }

    /**
     * 提交所有分片任务到共享执行器。
     *
     * @param request 原始 Markdown 后处理请求
     * @param plan 分片计划
     * @return 分片任务 future 列表
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private List<CompletableFuture<ChunkResult>> submitChunkFutures(
            MarkdownPostProcessingRequest request,
            MarkdownChunkPlan plan,
            MarkdownChunkCheckpointPlan checkpointPlan
    ) {
        List<CompletableFuture<ChunkResult>> futures = new ArrayList<>(plan.chunks().size());
        for (MarkdownChunk chunk : plan.chunks()) {
            futures.add(CompletableFuture.supplyAsync(() -> processChunk(request, chunk, checkpointPlan),
                    chunkExecutor));
        }
        return futures;
    }

    /**
     * 等待所有分片任务完成。
     *
     * @param futures 分片任务 future 列表
     * @return 分片结果列表
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private List<ChunkResult> awaitChunkResults(List<CompletableFuture<ChunkResult>> futures) {
        List<ChunkResult> results = new ArrayList<>(futures.size());
        for (CompletableFuture<ChunkResult> future : futures) {
            results.add(awaitChunkResult(future));
        }
        return results;
    }

    /**
     * 等待单个分片任务完成。
     *
     * @param future 分片任务 future
     * @return 分片结果
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private ChunkResult awaitChunkResult(CompletableFuture<ChunkResult> future) {
        try {
            return future.join();
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            throw new IllegalStateException("llm markdown chunk execution failed", cause);
        }
    }

    /**
     * 合并分片结果。
     *
     * @param plan 分片计划
     * @param results 分片结果列表
     * @return 合并后的 Markdown 后处理结果
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private MarkdownPostProcessingResult joinChunkResults(MarkdownChunkPlan plan, List<ChunkResult> results) {
        List<String> markdownParts = new ArrayList<>(results.size());
        boolean markdownApplied = false;
        List<ChunkResult> orderedResults = results.stream().sorted(Comparator.comparingInt(ChunkResult::chunkIndex))
                .toList();
        for (ChunkResult result : orderedResults) {
            markdownParts.add(result.markdown());
            markdownApplied = markdownApplied || result.markdownApplied();
        }
        String joinedMarkdown = String.join(CHUNK_SEPARATOR, markdownParts);
        if (markdownApplied) {
            return MarkdownPostProcessingResult.markdown(joinedMarkdown, chunkMetadata(plan));
        } else {
            return MarkdownPostProcessingResult.passthrough(joinedMarkdown, CHUNK_FAILED_WARNING);
        }
    }

    /**
     * 处理单个分片。
     *
     * @param request 原始 Markdown 后处理请求
     * @param chunk Markdown 分片
     * @param checkpointPlan checkpoint 计划
     * @return 分片结果
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private ChunkResult processChunk(
            MarkdownPostProcessingRequest request,
            MarkdownChunk chunk,
            MarkdownChunkCheckpointPlan checkpointPlan
    ) {
        return checkpointStore.load(checkpointPlan, chunk)
                .map(markdown -> new ChunkResult(chunk.chunkIndex(), markdown, true, CompletableFuture.completedFuture(null)))
                .orElseGet(() -> processMissingChunk(request, chunk, checkpointPlan));
    }

    /**
     * 处理缺少 checkpoint 的单个分片。
     *
     * @param request 原始 Markdown 后处理请求
     * @param chunk Markdown 分片
     * @param checkpointPlan checkpoint 计划
     * @return 分片结果
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private ChunkResult processMissingChunk(
            MarkdownPostProcessingRequest request,
            MarkdownChunk chunk,
            MarkdownChunkCheckpointPlan checkpointPlan
    ) {
        for (int attempt = 1; attempt <= CHUNK_MAX_ATTEMPTS; attempt++) {
            try {
                ChunkResult result = processChunkOnce(request, chunk);
                return result.withCheckpointSave(scheduleCheckpointSave(checkpointPlan, chunk, result));
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
     * 成功应用 Markdown 后保存 chunk checkpoint。
     *
     * @param checkpointPlan checkpoint 计划
     * @param chunk Markdown 分片
     * @param result 分片结果
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private CompletableFuture<Void> scheduleCheckpointSave(
            MarkdownChunkCheckpointPlan checkpointPlan,
            MarkdownChunk chunk,
            ChunkResult result
    ) {
        // 只有真实应用 LLM Markdown 的 chunk 才写 checkpoint。
        if (result.markdownApplied()) {
            return CompletableFuture.runAsync(() -> checkpointStore.save(
                    new MarkdownChunkCheckpoint(checkpointPlan, chunk, result.markdown(), true)), checkpointExecutor);
        } else {
            // fallback chunk 不写 checkpoint，后续 retry 仍会重新处理。
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * 等待本轮 checkpoint 写入完成。
     *
     * @param results 分片处理结果
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private void awaitCheckpointSaves(List<ChunkResult> results) {
        for (ChunkResult result : results) {
            result.checkpointSave().join();
        }
    }

    /**
     * 处理单个分片一次。
     *
     * @param request 原始 Markdown 后处理请求
     * @param chunk Markdown 分片
     * @return 分片结果
     * @throws IOException 分片提示词构建失败
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    private ChunkResult processChunkOnce(MarkdownPostProcessingRequest request, MarkdownChunk chunk)
            throws IOException {
        MarkdownPostProcessingRequest chunkRequest = chunkRequest(request, chunk);
        MarkdownPostProcessingResult result = delegate.process(chunkRequest);
        return new ChunkResult(chunk.chunkIndex(), result.markdown(), result.markdownApplied(),
                CompletableFuture.completedFuture(null));
    }

    /**
     * 记录分片重试日志。
     *
     * @param request 原始 Markdown 后处理请求
     * @param chunk Markdown 分片
     * @param attempt 当前尝试次数
     * @param ex 失败异常
     * @author lvdaxianerplus
     * @date 2026-06-19
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
     * @date 2026-06-19
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
     * @date 2026-06-19
     */
    private ChunkResult fallbackChunk(MarkdownPostProcessingRequest request, MarkdownChunk chunk, Exception ex) {
        LOGGER.warn("[LLM Markdown 分片] 分片重试耗尽，回退原文 documentId={}, chunkIndex={}, error={}",
                request.documentId(), chunk.chunkIndex(), ex.getClass().getSimpleName());
        return new ChunkResult(chunk.chunkIndex(), chunk.mainContent(), false, CompletableFuture.completedFuture(null));
    }

    /**
     * 创建分片请求。
     *
     * @param request 原始 Markdown 后处理请求
     * @param chunk Markdown 分片
     * @return 分片请求
     * @throws IOException 元数据序列化失败
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private MarkdownPostProcessingRequest chunkRequest(MarkdownPostProcessingRequest request, MarkdownChunk chunk)
            throws IOException {
        String chunkPrompt = MarkdownPrompt.chunkUserPrompt(objectMapper, request, chunk);
        return new MarkdownPostProcessingRequest(request.documentId(), request.fileName(), request.metadata(),
                chunkPrompt, request.chunkStrategy());
    }

    /**
     * 为未分片结果补充分片观测字段。
     *
     * @param result 委托结果
     * @param plan 分片计划
     * @return 带元数据的结果
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    private MarkdownPostProcessingResult withChunkMetadata(
            MarkdownPostProcessingResult result,
            MarkdownChunkPlan plan
    ) {
        if (result.markdownApplied()) {
            return MarkdownPostProcessingResult.markdown(result.markdown(), chunkMetadata(plan));
        } else {
            return result;
        }
    }

    /**
     * 构建 LLM 分片观测元数据。
     *
     * @param plan 分片计划
     * @return 分片观测元数据
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private Map<String, Object> chunkMetadata(MarkdownChunkPlan plan) {
        return Map.of(
                LLM_CHUNKED_FIELD, plan.chunked(),
                LLM_CHUNK_COUNT_FIELD, plan.chunks().size(),
                LLM_MAX_CONTEXT_TOKENS_FIELD, maxContextTokens,
                LLM_ESTIMATED_OCR_TOKENS_FIELD, plan.estimatedInputTokens());
    }

    /**
     * 分片结果。
     *
     * @param chunkIndex 分片序号
     * @param markdown 分片 Markdown
     * @param markdownApplied 是否应用了 Markdown
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    private record ChunkResult(
            int chunkIndex,
            String markdown,
            boolean markdownApplied,
            CompletableFuture<Void> checkpointSave
    ) {

        /**
         * 附加 checkpoint 写入 future。
         *
         * @param checkpointSave checkpoint 写入 future
         * @return 更新后的分片结果
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        private ChunkResult withCheckpointSave(CompletableFuture<Void> checkpointSave) {
            return new ChunkResult(chunkIndex, markdown, markdownApplied, checkpointSave);
        }
    }
}
