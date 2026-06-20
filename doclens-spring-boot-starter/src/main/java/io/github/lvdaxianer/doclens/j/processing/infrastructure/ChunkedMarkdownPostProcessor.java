package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunk;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkCheckpointPlan;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkCheckpointPlanSource;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkCheckpointStore;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkPlan;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunker;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessingRequest;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessingResult;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

/**
 * 支持大文本分片的 Markdown 后处理器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-13
 */
public final class ChunkedMarkdownPostProcessor implements MarkdownPostProcessor {

    private static final String CHUNK_FAILED_WARNING = "llm_markdown_chunk_failed";
    private static final String CHUNK_SEPARATOR = "\n\n";
    private static final String LLM_CHUNKED_FIELD = "llm_chunked";
    private static final String LLM_CHUNK_COUNT_FIELD = "llm_chunk_count";
    private static final String LLM_MAX_CONTEXT_TOKENS_FIELD = "llm_max_context_tokens";
    private static final String LLM_ESTIMATED_OCR_TOKENS_FIELD = "llm_estimated_ocr_tokens";

    private final MarkdownPostProcessor delegate;
    private final MarkdownChunker chunker;
    private final int maxContextTokens;
    private final ExecutorService chunkExecutor;
    private final ObjectMapper objectMapper;
    private final ChunkedMarkdownCheckpointCoordinator checkpointCoordinator;
    private final ChunkedMarkdownChunkWorker chunkWorker;

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
        this.objectMapper = new ObjectMapper();
        this.checkpointCoordinator = new ChunkedMarkdownCheckpointCoordinator(options.checkpointStore(),
                options.runtimeOptions().checkpointExecutor());
        this.chunkWorker = new ChunkedMarkdownChunkWorker(delegate, objectMapper, checkpointCoordinator);
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
        List<CompletableFuture<ChunkedMarkdownChunkResult>> futures = submitChunkFutures(request, plan, checkpointPlan);
        List<ChunkedMarkdownChunkResult> results = awaitChunkResults(futures);
        checkpointCoordinator.awaitSaves(request, results);
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
    private List<CompletableFuture<ChunkedMarkdownChunkResult>> submitChunkFutures(
            MarkdownPostProcessingRequest request,
            MarkdownChunkPlan plan,
            MarkdownChunkCheckpointPlan checkpointPlan
    ) {
        List<CompletableFuture<ChunkedMarkdownChunkResult>> futures = new ArrayList<>(plan.chunks().size());
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
    private List<ChunkedMarkdownChunkResult> awaitChunkResults(
            List<CompletableFuture<ChunkedMarkdownChunkResult>> futures
    ) {
        List<ChunkedMarkdownChunkResult> results = new ArrayList<>(futures.size());
        for (CompletableFuture<ChunkedMarkdownChunkResult> future : futures) {
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
    private ChunkedMarkdownChunkResult awaitChunkResult(CompletableFuture<ChunkedMarkdownChunkResult> future) {
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
    private MarkdownPostProcessingResult joinChunkResults(
            MarkdownChunkPlan plan,
            List<ChunkedMarkdownChunkResult> results
    ) {
        List<String> markdownParts = new ArrayList<>(results.size());
        boolean markdownApplied = false;
        List<ChunkedMarkdownChunkResult> orderedResults = results.stream()
                .sorted(Comparator.comparingInt(ChunkedMarkdownChunkResult::chunkIndex)).toList();
        for (ChunkedMarkdownChunkResult result : orderedResults) {
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
    private ChunkedMarkdownChunkResult processChunk(
            MarkdownPostProcessingRequest request,
            MarkdownChunk chunk,
            MarkdownChunkCheckpointPlan checkpointPlan
    ) {
        return chunkWorker.process(request, chunk, checkpointPlan);
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

}
