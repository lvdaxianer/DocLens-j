package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.processing.application.ApproximateTokenEstimator;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunk;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkCheckpoint;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkCheckpointPlan;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkCheckpointStore;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunker;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessingRequest;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessingResult;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessor;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * 分片 Markdown 异步 checkpoint 测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
class ChunkedMarkdownPostProcessorAsyncCheckpointTest {

    private static final int SMALL_CHUNK_MAX_CONTEXT_TOKENS = 2000;
    private static final String LARGE_DOCUMENT = "段落内容\n\n".repeat(400);

    /**
     * checkpoint 保存应离开 LLM worker 执行，并在返回结果前完成。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Test
    void savesCheckpointsOffTheLlmWorkerAndWaitsBeforeReturning() {
        AsyncCheckpointRecordingStore checkpointStore = new AsyncCheckpointRecordingStore();
        ThreadRecordingProcessor delegate = new ThreadRecordingProcessor();
        ProcessorHarness harness = harness(delegate, checkpointStore);
        try {
            CompletableFuture<MarkdownPostProcessingResult> future = submit(harness);

            assertWaitingForCheckpoint(checkpointStore, delegate, future);
            checkpointStore.releaseSave();
            assertCompletedAfterCheckpointRelease(checkpointStore, future);
        } finally {
            harness.shutdownNow();
        }
    }

    /**
     * 断言文档处理正在等待 checkpoint 保存完成。
     *
     * @param checkpointStore checkpoint 存储
     * @param delegate 下游处理器
     * @param future 文档处理 future
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private void assertWaitingForCheckpoint(
            AsyncCheckpointRecordingStore checkpointStore,
            ThreadRecordingProcessor delegate,
            CompletableFuture<MarkdownPostProcessingResult> future
    ) {
        assertThat(checkpointStore.awaitSaveStarted()).isTrue();
        assertThat(future.isDone()).isFalse();
        assertThat(checkpointStore.saveThreadName()).startsWith("checkpoint-writer");
        assertThat(delegate.threadName()).startsWith("llm-chunk-worker");
    }

    /**
     * 断言 checkpoint 放行后文档处理完成。
     *
     * @param checkpointStore checkpoint 存储
     * @param future 文档处理 future
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private void assertCompletedAfterCheckpointRelease(
            AsyncCheckpointRecordingStore checkpointStore,
            CompletableFuture<MarkdownPostProcessingResult> future
    ) {
        MarkdownPostProcessingResult result = future.orTimeout(2, TimeUnit.SECONDS).join();
        assertThat(result.markdown()).contains("chunk-0");
        assertThat(checkpointStore.saveCompleted()).isTrue();
    }

    /**
     * 创建异步 checkpoint 测试上下文。
     *
     * @param delegate 下游处理器
     * @param checkpointStore checkpoint 存储
     * @return 测试上下文
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private ProcessorHarness harness(
            MarkdownPostProcessor delegate,
            MarkdownChunkCheckpointStore checkpointStore
    ) {
        ExecutorService chunkExecutor = Executors.newSingleThreadExecutor(namedThreadFactory("llm-chunk-worker"));
        ExecutorService checkpointExecutor = Executors.newSingleThreadExecutor(namedThreadFactory("checkpoint-writer"));
        ExecutorService callerExecutor = Executors.newSingleThreadExecutor(namedThreadFactory("caller-worker"));
        ChunkedMarkdownRuntimeOptions runtimeOptions = new ChunkedMarkdownRuntimeOptions(chunkExecutor,
                checkpointExecutor);
        return new ProcessorHarness(processor(delegate, checkpointStore, runtimeOptions),
                chunkExecutor, checkpointExecutor, callerExecutor);
    }

    /**
     * 创建分片处理器。
     *
     * @param delegate 下游处理器
     * @param checkpointStore checkpoint 存储
     * @param runtimeOptions 运行时选项
     * @return 分片处理器
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private ChunkedMarkdownPostProcessor processor(
            MarkdownPostProcessor delegate,
            MarkdownChunkCheckpointStore checkpointStore,
            ChunkedMarkdownRuntimeOptions runtimeOptions
    ) {
        ChunkedMarkdownPostProcessorOptions options = new ChunkedMarkdownPostProcessorOptions(delegate,
                new MarkdownChunker(new ApproximateTokenEstimator()), SMALL_CHUNK_MAX_CONTEXT_TOKENS,
                checkpointStore, runtimeOptions);
        return new ChunkedMarkdownPostProcessor(options);
    }

    /**
     * 异步提交文档处理。
     *
     * @param harness 测试上下文
     * @return 文档处理 future
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private CompletableFuture<MarkdownPostProcessingResult> submit(ProcessorHarness harness) {
        return CompletableFuture.supplyAsync(() -> harness.processor().process(request(LARGE_DOCUMENT)),
                harness.callerExecutor());
    }

    /**
     * 创建 Markdown 后处理请求。
     *
     * @param text OCR 文本
     * @return Markdown 后处理请求
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private MarkdownPostProcessingRequest request(String text) {
        return new MarkdownPostProcessingRequest("doc-1", "demo.txt", Map.of("source", "test"), text);
    }

    /**
     * 创建带名称的单线程工厂。
     *
     * @param threadName 线程名称
     * @return 线程工厂
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private ThreadFactory namedThreadFactory(String threadName) {
        return runnable -> new Thread(runnable, threadName);
    }

    /**
     * 分片处理器测试上下文。
     *
     * @param processor 分片处理器
     * @param chunkExecutor 分片执行器
     * @param checkpointExecutor checkpoint 写入执行器
     * @param callerExecutor 调用执行器
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private record ProcessorHarness(
            ChunkedMarkdownPostProcessor processor,
            ExecutorService chunkExecutor,
            ExecutorService checkpointExecutor,
            ExecutorService callerExecutor
    ) {

        /**
         * 关闭测试执行器。
         *
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        private void shutdownNow() {
            callerExecutor.shutdownNow();
            checkpointExecutor.shutdownNow();
            chunkExecutor.shutdownNow();
        }
    }

    /**
     * 记录线程的测试处理器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private static final class ThreadRecordingProcessor implements MarkdownPostProcessor {

        private final AtomicReference<String> threadName = new AtomicReference<>("");

        /**
         * 处理请求并记录线程名称。
         *
         * @param request Markdown 后处理请求
         * @return Markdown 后处理结果
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        @Override
        public MarkdownPostProcessingResult process(MarkdownPostProcessingRequest request) {
            threadName.set(Thread.currentThread().getName());
            return MarkdownPostProcessingResult.markdown("chunk-0");
        }

        /**
         * 获取最后处理线程名称。
         *
         * @return 线程名称
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        String threadName() {
            return threadName.get();
        }
    }

    /**
     * 异步 checkpoint 写入记录 store。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private static final class AsyncCheckpointRecordingStore implements MarkdownChunkCheckpointStore {

        private final AtomicBoolean saveCompleted = new AtomicBoolean();
        private final CountDownLatch saveStarted = new CountDownLatch(1);
        private final CountDownLatch saveRelease = new CountDownLatch(1);
        private final AtomicReference<String> saveThreadName = new AtomicReference<>("");

        /**
         * 保存 checkpoint 并记录线程。
         *
         * @param checkpoint chunk checkpoint 内容
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        @Override
        public void save(MarkdownChunkCheckpoint checkpoint) {
            saveThreadName.set(Thread.currentThread().getName());
            saveStarted.countDown();
            awaitRelease();
            saveCompleted.set(true);
        }

        /**
         * 等待测试放行保存。
         *
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        private void awaitRelease() {
            try {
                saveRelease.await();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(ex);
            }
        }

        /**
         * 不提供已有 checkpoint。
         *
         * @param plan checkpoint 计划
         * @param chunk Markdown chunk
         * @return 空 checkpoint
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        @Override
        public Optional<String> load(MarkdownChunkCheckpointPlan plan, MarkdownChunk chunk) {
            return Optional.empty();
        }

        /**
         * 等待 checkpoint save 开始。
         *
         * @return 是否已开始
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        boolean awaitSaveStarted() {
            try {
                return saveStarted.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(ex);
            }
        }

        /**
         * 放行 checkpoint save。
         *
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        void releaseSave() {
            saveRelease.countDown();
        }

        /**
         * checkpoint save 是否已完成。
         *
         * @return 是否完成
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        boolean saveCompleted() {
            return saveCompleted.get();
        }

        /**
         * checkpoint save 线程名称。
         *
         * @return 线程名称
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        String saveThreadName() {
            return saveThreadName.get();
        }
    }
}
