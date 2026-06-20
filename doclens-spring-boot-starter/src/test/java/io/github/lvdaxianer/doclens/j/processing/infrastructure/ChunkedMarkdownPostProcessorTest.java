package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.processing.application.ApproximateTokenEstimator;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunk;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkCheckpoint;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkCheckpointPlan;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkCheckpointPlanSource;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkPlan;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunker;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessingRequest;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessingResult;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessor;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 分片 Markdown 后处理器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-13
 */
class ChunkedMarkdownPostProcessorTest {

    private static final int DEFAULT_MAX_CONTEXT_TOKENS = 16000;
    private static final int SMALL_CHUNK_MAX_CONTEXT_TOKENS = 2000;
    private static final String LARGE_DOCUMENT = "段落内容\n\n".repeat(5000);

    @TempDir
    private Path storageRoot;

    /**
     * 小文档应只调用一次下游处理器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    @Test
    void processesSmallDocumentOnce() {
        RecordingProcessor delegate = new RecordingProcessor(List.of(MarkdownPostProcessingResult.markdown("整理后")));
        ExecutorService chunkExecutor = Executors.newSingleThreadExecutor();
        try {
            ChunkedMarkdownPostProcessor processor = new ChunkedMarkdownPostProcessor(delegate,
                    new MarkdownChunker(new ApproximateTokenEstimator()), DEFAULT_MAX_CONTEXT_TOKENS, chunkExecutor);

            MarkdownPostProcessingResult result = processor.process(request("短文档"));

            assertThat(result.markdown()).isEqualTo("整理后");
            assertThat(delegate.requests()).hasSize(1);
        } finally {
            chunkExecutor.shutdownNow();
        }
    }

    /**
     * 大文档应按分片顺序处理并按顺序合并输出。
     *
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    @Test
    void processesLargeDocumentInOrderAndMergesOutputs() {
        RecordingProcessor delegate = new RecordingProcessor(List.of(
                MarkdownPostProcessingResult.markdown("第一段"),
                MarkdownPostProcessingResult.markdown("第二段"),
                MarkdownPostProcessingResult.markdown("第三段"),
                MarkdownPostProcessingResult.markdown("第四段")));
        ExecutorService chunkExecutor = Executors.newSingleThreadExecutor();
        try {
            ChunkedMarkdownPostProcessor processor = new ChunkedMarkdownPostProcessor(delegate,
                    new MarkdownChunker(new ApproximateTokenEstimator()), SMALL_CHUNK_MAX_CONTEXT_TOKENS, chunkExecutor);

            MarkdownPostProcessingResult result = processor.process(request(LARGE_DOCUMENT));

            assertThat(result.markdown()).startsWith("第一段\n\n第二段");
            assertThat(delegate.requests()).hasSizeGreaterThan(1);
            assertThat(delegate.requests().get(1).ocrText()).contains("previous_context");
        } finally {
            chunkExecutor.shutdownNow();
        }
    }

    /**
     * 大文档分片应并发进入共享执行器，并按原始顺序合并结果。
     *
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    @Test
    void processesChunksInParallelAcrossASharedExecutorAndMergesByChunkOrder() throws Exception {
        ParallelChunkRecordingProcessor delegate = new ParallelChunkRecordingProcessor();
        MarkdownChunkPlan plan = plan();
        ExecutorService chunkExecutor = chunkExecutor(plan);
        ExecutorService callerExecutor = callerExecutor();
        try {
            MarkdownPostProcessingResult result = processAsync(delegate, chunkExecutor, callerExecutor);
            assertThat(result.markdown()).isEqualTo(orderedChunkMarkdown(plan));
        } finally {
            callerExecutor.shutdownNow();
            chunkExecutor.shutdownNow();
        }
    }

    /**
     * 两个文档应共享同一个 chunk 执行器并同时开始分片工作。
     *
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    @Test
    void processesTwoDocumentsWithTheSameChunkExecutorWithoutSerializingThem() throws Exception {
        SharedExecutorRecordingProcessor delegate = new SharedExecutorRecordingProcessor();
        MarkdownChunkPlan plan = plan();
        ExecutorService chunkExecutor = chunkExecutor(plan);
        ExecutorService callerExecutor = Executors.newFixedThreadPool(2);
        try {
            ChunkedMarkdownPostProcessor processor = new ChunkedMarkdownPostProcessor(delegate,
                    new MarkdownChunker(new ApproximateTokenEstimator()), SMALL_CHUNK_MAX_CONTEXT_TOKENS, chunkExecutor);
            CompletableFuture<MarkdownPostProcessingResult> first = submit(processor, callerExecutor, "doc-a");
            CompletableFuture<MarkdownPostProcessingResult> second = submit(processor, callerExecutor, "doc-b");
            assertThat(delegate.awaitStartedCount(2)).isTrue();
            assertThat(delegate.awaitStartedDocuments("doc-a", "doc-b")).isTrue();
            delegate.release();
            assertThat(first.get(5, TimeUnit.SECONDS).markdown()).isEqualTo(orderedChunkMarkdown(plan));
            assertThat(second.get(5, TimeUnit.SECONDS).markdown()).isEqualTo(orderedChunkMarkdown(plan));
        } finally {
            callerExecutor.shutdownNow();
            chunkExecutor.shutdownNow();
        }
    }

    /**
     * 单个分片重试耗尽后应回退该分片原文，其他分片继续合并。
     *
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    @Test
    void fallsBackOnlyTheFailedChunkWhenRetriesAreExhausted() {
        MarkdownChunkPlan plan = plan();
        FailingChunkRecordingProcessor delegate = new FailingChunkRecordingProcessor(1);
        ExecutorService chunkExecutor = Executors.newFixedThreadPool(3);
        try {
            delegate.prepare(plan.chunks());
            ChunkedMarkdownPostProcessor processor = new ChunkedMarkdownPostProcessor(delegate,
                    new MarkdownChunker(new ApproximateTokenEstimator()),
                    SMALL_CHUNK_MAX_CONTEXT_TOKENS, chunkExecutor);

            MarkdownPostProcessingResult result = processor.process(request(LARGE_DOCUMENT));

            assertThat(result.markdown()).isEqualTo(fallbackChunkMarkdown(plan));
            assertThat(result.markdownApplied()).isTrue();
        } finally {
            chunkExecutor.shutdownNow();
        }
    }

    /**
     * 已有有效 checkpoint 的 chunk 应跳过 LLM，仅缺失 chunk 调用下游处理器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Test
    void reusesCheckpointedChunksAndProcessesOnlyMissingChunks() {
        MarkdownChunkPlan plan = plan();
        MarkdownPostProcessingRequest request = request(LARGE_DOCUMENT);
        FileSystemMarkdownChunkCheckpointStore checkpointStore = new FileSystemMarkdownChunkCheckpointStore(storageRoot);
        MarkdownChunkCheckpointPlan checkpointPlan = MarkdownChunkCheckpointPlan.from(
                new MarkdownChunkCheckpointPlanSource(request, plan, SMALL_CHUNK_MAX_CONTEXT_TOKENS));
        checkpointStore.save(new MarkdownChunkCheckpoint(checkpointPlan, plan.chunks().getFirst(), "cached-chunk-0",
                true));
        RecordingProcessor delegate = new RecordingProcessor(List.of(
                MarkdownPostProcessingResult.markdown("chunk-1"),
                MarkdownPostProcessingResult.markdown("chunk-2"),
                MarkdownPostProcessingResult.markdown("chunk-3")));
        ExecutorService chunkExecutor = Executors.newSingleThreadExecutor();
        try {
            ChunkedMarkdownPostProcessor processor = new ChunkedMarkdownPostProcessor(
                    new ChunkedMarkdownPostProcessorOptions(delegate, new MarkdownChunker(new ApproximateTokenEstimator()),
                            SMALL_CHUNK_MAX_CONTEXT_TOKENS, chunkExecutor, checkpointStore));

            MarkdownPostProcessingResult result = processor.process(request);

            assertThat(result.markdown()).startsWith("cached-chunk-0\n\nchunk-1");
            assertThat(delegate.requests()).hasSize(plan.chunks().size() - 1);
            assertThat(extractChunkIndex(delegate.requests().getFirst().ocrText())).isEqualTo(1);
        } finally {
            chunkExecutor.shutdownNow();
        }
    }

    /**
     * fallback chunk 不应写入 checkpoint，后续运行应重新处理该 chunk。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Test
    void doesNotCheckpointFallbackChunksSoLaterRunRetriesThem() {
        MarkdownChunkPlan plan = plan();
        MarkdownPostProcessingRequest request = request(LARGE_DOCUMENT);
        FileSystemMarkdownChunkCheckpointStore checkpointStore = new FileSystemMarkdownChunkCheckpointStore(storageRoot);
        CheckpointLookup checkpointLookup = new CheckpointLookup(checkpointStore, request, plan);
        ExecutorService firstExecutor = Executors.newSingleThreadExecutor();
        try {
            ChunkedMarkdownPostProcessor firstProcessor = checkpointedProcessor(
                    new FailingChunkRecordingProcessor(1), firstExecutor, checkpointStore);

            MarkdownPostProcessingResult firstResult = firstProcessor.process(request);

            assertThat(firstResult.markdown()).isEqualTo(fallbackChunkMarkdown(plan));
            assertThat(loadCheckpoint(checkpointLookup, 1)).isEmpty();
        } finally {
            firstExecutor.shutdownNow();
        }
        assertOnlyFallbackChunkRetries(request, checkpointStore);
    }

    /**
     * 宕机恢复时应复用已完成 chunk 文件，只处理缺失 chunk。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Test
    void resumesInterruptedDocumentFromPersistedChunkFiles() {
        MarkdownChunkPlan plan = plan();
        MarkdownPostProcessingRequest request = request(LARGE_DOCUMENT);
        FileSystemMarkdownChunkCheckpointStore checkpointStore = new FileSystemMarkdownChunkCheckpointStore(storageRoot);
        int missingChunkIndex = plan.chunks().size() - 1;
        CrashResumeSetup crashResumeSetup = new CrashResumeSetup(checkpointStore, request, plan, missingChunkIndex);
        seedCompletedChunks(crashResumeSetup);
        RecordingProcessor delegate = new RecordingProcessor(List.of(
                MarkdownPostProcessingResult.markdown("recovered-chunk-" + missingChunkIndex)));
        ExecutorService chunkExecutor = Executors.newSingleThreadExecutor();
        try {
            ChunkedMarkdownPostProcessor restartedProcessor = checkpointedProcessor(delegate, chunkExecutor,
                    checkpointStore);

            MarkdownPostProcessingResult result = restartedProcessor.process(request);

            assertThat(result.markdown()).isEqualTo(crashResumeMarkdown(plan, missingChunkIndex));
            assertThat(delegate.requests()).hasSize(1);
            assertThat(extractChunkIndex(delegate.requests().getFirst().ocrText())).isEqualTo(missingChunkIndex);
        } finally {
            chunkExecutor.shutdownNow();
        }
    }

    /**
     * 创建 Markdown 后处理请求。
     *
     * @param text OCR 文本
     * @return Markdown 后处理请求
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private MarkdownPostProcessingRequest request(String text) {
        return request("doc-1", text);
    }

    /**
     * 创建 Markdown 后处理请求。
     *
     * @param documentId 文档 ID
     * @param text OCR 文本
     * @return Markdown 后处理请求
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    private MarkdownPostProcessingRequest request(String documentId, String text) {
        return new MarkdownPostProcessingRequest(documentId, "demo.txt", Map.of("source", "test"), text);
    }

    /**
     * 生成大文档分片计划。
     *
     * @return 分片计划
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    private MarkdownChunkPlan plan() {
        return new MarkdownChunker(new ApproximateTokenEstimator()).plan(LARGE_DOCUMENT,
                SMALL_CHUNK_MAX_CONTEXT_TOKENS);
    }

    /**
     * 创建共享分片执行器。
     *
     * @param plan 分片计划
     * @return 共享分片执行器
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    private ExecutorService chunkExecutor(MarkdownChunkPlan plan) {
        return Executors.newFixedThreadPool(plan.chunks().size());
    }

    /**
     * 创建调用执行器。
     *
     * @return 调用执行器
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    private ExecutorService callerExecutor() {
        return Executors.newSingleThreadExecutor();
    }

    /**
     * 提交单个文档的分片处理任务。
     *
     * @param processor 分片处理器
     * @param callerExecutor 调用执行器
     * @param documentId 文档 ID
     * @return 分片结果 future
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    private CompletableFuture<MarkdownPostProcessingResult> submit(
            ChunkedMarkdownPostProcessor processor,
            ExecutorService callerExecutor,
            String documentId
    ) {
        return CompletableFuture.supplyAsync(() -> processor.process(request(documentId, LARGE_DOCUMENT)),
                callerExecutor);
    }

    /**
     * 创建带 checkpoint store 的分片处理器。
     *
     * @param delegate 下游处理器
     * @param chunkExecutor 分片执行器
     * @param checkpointStore checkpoint 存储
     * @return 分片处理器
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private ChunkedMarkdownPostProcessor checkpointedProcessor(
            MarkdownPostProcessor delegate,
            ExecutorService chunkExecutor,
            FileSystemMarkdownChunkCheckpointStore checkpointStore
    ) {
        ChunkedMarkdownPostProcessorOptions options = new ChunkedMarkdownPostProcessorOptions(delegate,
                new MarkdownChunker(new ApproximateTokenEstimator()), SMALL_CHUNK_MAX_CONTEXT_TOKENS,
                chunkExecutor, checkpointStore);
        return new ChunkedMarkdownPostProcessor(options);
    }

    /**
     * 写入模拟宕机前已完成的 chunk 文件。
     *
     * @param crashResumeSetup 宕机恢复测试上下文
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private void seedCompletedChunks(CrashResumeSetup crashResumeSetup) {
        MarkdownChunkCheckpointPlan checkpointPlan = MarkdownChunkCheckpointPlan.from(
                new MarkdownChunkCheckpointPlanSource(crashResumeSetup.request(), crashResumeSetup.plan(),
                        SMALL_CHUNK_MAX_CONTEXT_TOKENS));
        for (MarkdownChunk chunk : crashResumeSetup.plan().chunks()) {
            saveCompletedChunk(crashResumeSetup, checkpointPlan, chunk);
        }
    }

    /**
     * 保存非缺失 chunk 的完成结果。
     *
     * @param crashResumeSetup 宕机恢复测试上下文
     * @param checkpointPlan checkpoint 计划
     * @param chunk Markdown 分片
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private void saveCompletedChunk(
            CrashResumeSetup crashResumeSetup,
            MarkdownChunkCheckpointPlan checkpointPlan,
            MarkdownChunk chunk
    ) {
        // 当前 chunk 不是模拟宕机缺失 chunk 时写入完成 checkpoint。
        if (chunk.chunkIndex() != crashResumeSetup.missingChunkIndex()) {
            crashResumeSetup.checkpointStore().save(new MarkdownChunkCheckpoint(checkpointPlan, chunk,
                    "completed-chunk-" + chunk.chunkIndex(), true));
        }
    }

    /**
     * 读取指定 chunk checkpoint。
     *
     * @param checkpointLookup checkpoint 查询上下文
     * @param chunkIndex 分片序号
     * @return checkpoint 内容
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private Optional<String> loadCheckpoint(CheckpointLookup checkpointLookup, int chunkIndex) {
        MarkdownChunkCheckpointPlan checkpointPlan = MarkdownChunkCheckpointPlan.from(
                new MarkdownChunkCheckpointPlanSource(checkpointLookup.request(), checkpointLookup.plan(),
                        SMALL_CHUNK_MAX_CONTEXT_TOKENS));
        return checkpointLookup.checkpointStore().load(checkpointPlan, checkpointLookup.plan().chunks().get(chunkIndex));
    }

    /**
     * 断言后续运行只重试 fallback chunk。
     *
     * @param request Markdown 后处理请求
     * @param checkpointStore checkpoint 存储
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private void assertOnlyFallbackChunkRetries(
            MarkdownPostProcessingRequest request,
            FileSystemMarkdownChunkCheckpointStore checkpointStore
    ) {
        RecordingProcessor delegate = new RecordingProcessor(List.of(
                MarkdownPostProcessingResult.markdown("retried-chunk-1")));
        ExecutorService secondExecutor = Executors.newSingleThreadExecutor();
        try {
            ChunkedMarkdownPostProcessor secondProcessor = checkpointedProcessor(delegate, secondExecutor,
                    checkpointStore);
            MarkdownPostProcessingResult secondResult = secondProcessor.process(request);
            assertThat(secondResult.markdown()).contains("retried-chunk-1");
            assertThat(delegate.requests()).hasSize(1);
            assertThat(extractChunkIndex(delegate.requests().getFirst().ocrText())).isEqualTo(1);
        } finally {
            secondExecutor.shutdownNow();
        }
    }

    /**
     * checkpoint 查询上下文。
     *
     * @param checkpointStore checkpoint 存储
     * @param request Markdown 后处理请求
     * @param plan 分片计划
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private record CheckpointLookup(
            FileSystemMarkdownChunkCheckpointStore checkpointStore,
            MarkdownPostProcessingRequest request,
            MarkdownChunkPlan plan
    ) {
    }

    /**
     * 宕机恢复测试上下文。
     *
     * @param checkpointStore checkpoint 存储
     * @param request Markdown 后处理请求
     * @param plan 分片计划
     * @param missingChunkIndex 缺失 chunk 序号
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private record CrashResumeSetup(
            FileSystemMarkdownChunkCheckpointStore checkpointStore,
            MarkdownPostProcessingRequest request,
            MarkdownChunkPlan plan,
            int missingChunkIndex
    ) {
    }

    /**
     * 等待指定分片数进入处理区。
     *
     * @param delegate 并发记录器
     * @param expectedStarted 预期已开始数量
     * @return 是否满足预期
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    private boolean awaitStartedCount(ParallelChunkRecordingProcessor delegate, int expectedStarted) {
        return delegate.awaitStartedCount(expectedStarted);
    }

    /**
     * 生成按 chunk 索引顺序的结果。
     *
     * @param plan 分片计划
     * @return 结果字符串
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    private String orderedChunkMarkdown(MarkdownChunkPlan plan) {
        return joinChunkMarkdown(plan, chunk -> "chunk-" + chunk.chunkIndex());
    }

    /**
     * 生成带失败回退的分片结果。
     *
     * @param plan 分片计划
     * @return 结果字符串
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    private String fallbackChunkMarkdown(MarkdownChunkPlan plan) {
        return joinChunkMarkdown(plan, chunk -> chunk.chunkIndex() == 1 ? chunk.mainContent()
                : "chunk-" + chunk.chunkIndex());
    }

    /**
     * 生成宕机恢复后的预期 Markdown。
     *
     * @param plan 分片计划
     * @param missingChunkIndex 缺失 chunk 序号
     * @return 预期 Markdown
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private String crashResumeMarkdown(MarkdownChunkPlan plan, int missingChunkIndex) {
        return joinChunkMarkdown(plan, chunk -> {
            if (chunk.chunkIndex() == missingChunkIndex) {
                return "recovered-chunk-" + missingChunkIndex;
            } else {
                return "completed-chunk-" + chunk.chunkIndex();
            }
        });
    }

    /**
     * 处理单个文档并等待共享分片结果。
     *
     * @param delegate 记录器
     * @param chunkExecutor 分片执行器
     * @param callerExecutor 调用执行器
     * @return 分片结果
     * @throws Exception 异常
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    private MarkdownPostProcessingResult processAsync(
            ParallelChunkRecordingProcessor delegate,
            ExecutorService chunkExecutor,
            ExecutorService callerExecutor
    ) throws Exception {
        MarkdownChunkPlan plan = plan();
        delegate.prepare(plan.chunks());
        ChunkedMarkdownPostProcessor processor = new ChunkedMarkdownPostProcessor(delegate,
                new MarkdownChunker(new ApproximateTokenEstimator()), SMALL_CHUNK_MAX_CONTEXT_TOKENS, chunkExecutor);
        CompletableFuture<MarkdownPostProcessingResult> future = CompletableFuture.supplyAsync(
                () -> processor.process(request(LARGE_DOCUMENT)), callerExecutor);
        assertThat(awaitStartedCount(delegate, 2)).isTrue();
        delegate.release();
        assertThat(delegate.maxInFlight()).isGreaterThan(1);
        return future.get(5, TimeUnit.SECONDS);
    }

    /**
     * 将分片结果按顺序拼接为 Markdown。
     *
     * @param plan 分片计划
     * @param mapper 分片到结果的映射
     * @return 拼接后的 Markdown
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    private String joinChunkMarkdown(MarkdownChunkPlan plan, java.util.function.Function<MarkdownChunk, String> mapper) {
        return plan.chunks().stream().map(mapper).collect(Collectors.joining("\n\n"));
    }

    /**
     * 记录请求的测试处理器。
     *
     * @param responses 预置响应
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private static class RecordingProcessor implements MarkdownPostProcessor {

        private final List<MarkdownPostProcessingResult> responses;
        private final List<MarkdownPostProcessingRequest> requests = new ArrayList<>();

        /**
         * 创建记录请求的测试处理器。
         *
         * @param responses 预置响应
         * @author lvdaxianerplus
         * @date 2026-06-13
         */
        private RecordingProcessor(List<MarkdownPostProcessingResult> responses) {
            this.responses = responses;
        }

        /**
         * 处理请求并记录调用参数。
         *
         * @param request Markdown 后处理请求
         * @return 预置响应
         * @author lvdaxianerplus
         * @date 2026-06-13
         */
        @Override
        public MarkdownPostProcessingResult process(MarkdownPostProcessingRequest request) {
            requests.add(request);
            int responseIndex = Math.min(requests.size() - 1, responses.size() - 1);
            return responses.get(responseIndex);
        }

        /**
         * 获取已记录的请求。
         *
         * @return 请求列表
         * @author lvdaxianerplus
         * @date 2026-06-13
         */
        List<MarkdownPostProcessingRequest> requests() {
            return List.copyOf(requests);
        }
    }

    /**
     * 并发分片处理器记录器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    private static final class ParallelChunkRecordingProcessor implements MarkdownPostProcessor {

        private final CountDownLatch releaseLatch = new CountDownLatch(1);
        private final List<MarkdownChunk> chunks = new ArrayList<>();
        private final List<String> seenChunkTexts = new ArrayList<>();
        private int inFlight;
        private volatile int maxInFlight;

        /**
         * 预置分片列表。
         *
         * @param plannedChunks 计划分片
         * @author lvdaxianerplus
         * @date 2026-06-19
         */
        synchronized void prepare(List<MarkdownChunk> plannedChunks) {
            chunks.clear();
            chunks.addAll(plannedChunks);
        }

        /**
         * 放行全部请求。
         *
         * @author lvdaxianerplus
         * @date 2026-06-19
         */
        void release() {
            releaseLatch.countDown();
        }

        /**
         * 等待全部分片进入处理区。
         *
         * @return 是否全部进入处理区
         * @author lvdaxianerplus
         * @date 2026-06-19
         */
        synchronized boolean awaitStartedCount(int expectedStarted) {
            long deadline = System.currentTimeMillis() + 2000;
            while (seenChunkTexts.size() < expectedStarted && System.currentTimeMillis() < deadline) {
                try {
                    wait(10L);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ex);
                }
            }
            return seenChunkTexts.size() >= expectedStarted;
        }

        /**
         * 获取最大并发数。
         *
         * @return 最大并发数
         * @author lvdaxianerplus
         * @date 2026-06-19
         */
        int maxInFlight() {
            return maxInFlight;
        }

        /**
         * 处理请求并记录并发情况。
         *
         * @param request Markdown 后处理请求
         * @return Markdown 后处理结果
         * @author lvdaxianerplus
         * @date 2026-06-19
         */
        @Override
        public MarkdownPostProcessingResult process(MarkdownPostProcessingRequest request) {
            synchronized (this) {
                seenChunkTexts.add(request.ocrText());
                inFlight++;
                maxInFlight = Math.max(maxInFlight, inFlight);
                notifyAll();
            }
            awaitRelease();
            synchronized (this) {
                inFlight--;
            }
            int chunkIndex = chunkIndex(request);
            return MarkdownPostProcessingResult.markdown("chunk-" + chunkIndex);
        }

        /**
         * 根据请求匹配 chunk 序号。
         *
         * @param request Markdown 后处理请求
         * @return 分片序号
         * @author lvdaxianerplus
         * @date 2026-06-19
         */
        private synchronized int chunkIndex(MarkdownPostProcessingRequest request) {
            return extractChunkIndex(request.ocrText());
        }

        /**
         * 等待测试释放并发结果。
         *
         * @author lvdaxianerplus
         * @date 2026-06-19
         */
        private void awaitRelease() {
            try {
                releaseLatch.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(ex);
            }
        }
    }

    /**
     * 共享执行器文档记录器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    private static final class SharedExecutorRecordingProcessor implements MarkdownPostProcessor {

        private final CountDownLatch releaseLatch = new CountDownLatch(1);
        private final List<MarkdownChunk> chunks = new ArrayList<>();
        private final List<String> seenDocuments = new ArrayList<>();
        private int inFlight;
        private volatile int maxInFlight;

        /**
         * 预置分片列表。
         *
         * @param plannedChunks 计划分片
         * @author lvdaxianerplus
         * @date 2026-06-19
         */
        synchronized void prepare(List<MarkdownChunk> plannedChunks) {
            chunks.clear();
            chunks.addAll(plannedChunks);
        }

        /**
         * 放行全部请求。
         *
         * @author lvdaxianerplus
         * @date 2026-06-19
         */
        void release() {
            releaseLatch.countDown();
        }

        /**
         * 处理请求并记录并发情况。
         *
         * @param request Markdown 后处理请求
         * @return Markdown 后处理结果
         * @author lvdaxianerplus
         * @date 2026-06-19
         */
        @Override
        public MarkdownPostProcessingResult process(MarkdownPostProcessingRequest request) {
            synchronized (this) {
                seenDocuments.add(request.documentId());
                inFlight++;
                maxInFlight = Math.max(maxInFlight, inFlight);
                notifyAll();
            }
            awaitRelease();
            synchronized (this) {
                inFlight--;
            }
            return MarkdownPostProcessingResult.markdown("chunk-" + extractChunkIndex(request.ocrText()));
        }

        /**
         * 等待指定数量的分片开始处理。
         *
         * @param expectedStarted 预期开始数
         * @return 是否满足预期
         * @author lvdaxianerplus
         * @date 2026-06-19
         */
        synchronized boolean awaitStartedCount(int expectedStarted) {
            long deadline = System.currentTimeMillis() + 2000;
            while (seenDocuments.size() < expectedStarted && System.currentTimeMillis() < deadline) {
                try {
                    wait(10L);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ex);
                }
            }
            return seenDocuments.size() >= expectedStarted;
        }

        /**
         * 等待指定文档都开始处理。
         *
         * @param documentIds 预期文档 ID
         * @return 是否都已开始
         * @author lvdaxianerplus
         * @date 2026-06-19
         */
        synchronized boolean awaitStartedDocuments(String... documentIds) {
            long deadline = System.currentTimeMillis() + 2000;
            while (!seenDocuments.containsAll(List.of(documentIds)) && System.currentTimeMillis() < deadline) {
                try {
                    wait(10L);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ex);
                }
            }
            return seenDocuments.containsAll(List.of(documentIds));
        }

        /**
         * 获取最大并发数。
         *
         * @return 最大并发数
         * @author lvdaxianerplus
         * @date 2026-06-19
         */
        int maxInFlight() {
            return maxInFlight;
        }

        /**
         * 获取已开始处理的文档 ID。
         *
         * @return 已开始处理的文档 ID
         * @author lvdaxianerplus
         * @date 2026-06-19
         */
        synchronized List<String> seenDocuments() {
            return List.copyOf(seenDocuments);
        }

        /**
         * 等待测试释放并发结果。
         *
         * @author lvdaxianerplus
         * @date 2026-06-19
         */
        private void awaitRelease() {
            try {
                releaseLatch.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(ex);
            }
        }
    }

    /**
     * 指定 chunk 失败的记录器。
     *
     * @param failingChunkIndex 失败的 chunk 序号
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    private static final class FailingChunkRecordingProcessor extends RecordingProcessor {

        private final int failingChunkIndex;
        private final List<MarkdownChunk> chunks = new ArrayList<>();
        /**
         * 创建指定 chunk 失败的记录器。
         *
         * @param failingChunkIndex 失败的 chunk 序号
         * @author lvdaxianerplus
         * @date 2026-06-19
         */
        private FailingChunkRecordingProcessor(int failingChunkIndex) {
            super(List.of());
            this.failingChunkIndex = failingChunkIndex;
        }

        /**
         * 预置分片列表。
         *
         * @param plannedChunks 计划分片
         * @author lvdaxianerplus
         * @date 2026-06-19
         */
        synchronized void prepare(List<MarkdownChunk> plannedChunks) {
            chunks.clear();
            chunks.addAll(plannedChunks);
        }

        /**
         * 处理请求，针对指定 chunk 抛出异常。
         *
         * @param request Markdown 后处理请求
         * @return Markdown 后处理结果
         * @author lvdaxianerplus
         * @date 2026-06-19
         */
        @Override
        public MarkdownPostProcessingResult process(MarkdownPostProcessingRequest request) {
            int chunkIndex = extractChunkIndex(request.ocrText());
            if (chunkIndex == failingChunkIndex) {
                throw new IllegalStateException("forced failure");
            }
            return MarkdownPostProcessingResult.markdown("chunk-" + chunkIndex);
        }
    }

    /**
     * 从分片提示词中提取 chunk_index。
     *
     * @param prompt 分片提示词
     * @return chunk 序号
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    private static int extractChunkIndex(String prompt) {
        Matcher matcher = Pattern.compile("(?s).*?chunk_index:\\s*(\\d+).*").matcher(prompt);
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(1));
        }
        throw new IllegalStateException("missing chunk_index");
    }

}
