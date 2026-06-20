package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * 分片 Markdown checkpoint 失败处理测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
class ChunkedMarkdownPostProcessorCheckpointFailureTest {

    private static final int SMALL_CHUNK_MAX_CONTEXT_TOKENS = 2000;
    private static final String LARGE_DOCUMENT = "段落内容\n\n".repeat(400);

    /**
     * checkpoint 写入失败不应覆盖内存中的成功 Markdown。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Test
    void keepsMarkdownResultAndLogsWarningWhenCheckpointSaveFails() {
        FailingCheckpointStore checkpointStore = new FailingCheckpointStore();
        LogCapture logCapture = LogCapture.start();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            MarkdownPostProcessingResult result = processor(checkpointStore, executor)
                    .process(request(LARGE_DOCUMENT));

            assertThat(result.markdown()).contains("chunk-0");
            assertThat(result.markdownApplied()).isTrue();
            assertThat(logCapture.containsCheckpointWarning()).isTrue();
        } finally {
            executor.shutdownNow();
            logCapture.stop();
        }
    }

    /**
     * 创建分片处理器。
     *
     * @param checkpointStore checkpoint 存储
     * @param executor 执行器
     * @return 分片处理器
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private ChunkedMarkdownPostProcessor processor(
            MarkdownChunkCheckpointStore checkpointStore,
            ExecutorService executor
    ) {
        ChunkedMarkdownPostProcessorOptions options = new ChunkedMarkdownPostProcessorOptions(
                new StaticMarkdownProcessor(), new MarkdownChunker(new ApproximateTokenEstimator()),
                SMALL_CHUNK_MAX_CONTEXT_TOKENS, checkpointStore, new ChunkedMarkdownRuntimeOptions(executor, executor));
        return new ChunkedMarkdownPostProcessor(options);
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
     * 固定返回 Markdown 的测试处理器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private static final class StaticMarkdownProcessor implements MarkdownPostProcessor {

        /**
         * 返回固定 Markdown。
         *
         * @param request Markdown 后处理请求
         * @return Markdown 后处理结果
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        @Override
        public MarkdownPostProcessingResult process(MarkdownPostProcessingRequest request) {
            return MarkdownPostProcessingResult.markdown("chunk-0");
        }
    }

    /**
     * checkpoint 写入失败的测试 store。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private static final class FailingCheckpointStore implements MarkdownChunkCheckpointStore {

        /**
         * 始终抛出 checkpoint 写入异常。
         *
         * @param checkpoint chunk checkpoint 内容
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        @Override
        public void save(MarkdownChunkCheckpoint checkpoint) {
            throw new IllegalStateException("checkpoint disk unavailable");
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
    }

    /**
     * ChunkedMarkdownCheckpointCoordinator 日志捕获器。
     *
     * @param logger logback logger
     * @param appender 日志 appender
     * @param isAdditive 原始 additive 状态
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private record LogCapture(Logger logger, ListAppender<ILoggingEvent> appender, boolean isAdditive) {

        private static final String WARNING_MESSAGE = "[LLM Markdown 分片] checkpoint 写入失败";

        /**
         * 开始捕获日志。
         *
         * @return 日志捕获器
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        private static LogCapture start() {
            Logger logger = (Logger) LoggerFactory.getLogger(ChunkedMarkdownCheckpointCoordinator.class);
            boolean isAdditive = logger.isAdditive();
            ListAppender<ILoggingEvent> appender = new ListAppender<>();
            appender.start();
            logger.addAppender(appender);
            logger.setAdditive(false);
            return new LogCapture(logger, appender, isAdditive);
        }

        /**
         * 判断是否包含 checkpoint warn 日志。
         *
         * @return 是否包含日志
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        private boolean containsCheckpointWarning() {
            return appender.list.stream().anyMatch(this::isCheckpointWarning);
        }

        /**
         * 判断日志事件是否是 checkpoint warn。
         *
         * @param event 日志事件
         * @return 是否匹配
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        private boolean isCheckpointWarning(ILoggingEvent event) {
            return event.getLevel().equals(Level.WARN)
                    && event.getFormattedMessage().contains(WARNING_MESSAGE);
        }

        /**
         * 停止日志捕获。
         *
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        private void stop() {
            logger.detachAppender(appender);
            logger.setAdditive(isAdditive);
            appender.stop();
        }
    }
}
