package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.processing.application.ApproximateTokenEstimator;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunker;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessingRequest;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessingResult;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 分片 Markdown 后处理器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-13
 */
class ChunkedMarkdownPostProcessorTest {

    private static final int DEFAULT_MAX_CONTEXT_TOKENS = 16000;
    private static final int SMALL_CHUNK_MAX_CONTEXT_TOKENS = 2000;
    private static final int LARGE_DOCUMENT_REPEAT_COUNT = 5000;
    private static final int SECOND_REQUEST_INDEX = 1;

    /**
     * 小文档应只调用一次下游处理器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    @Test
    void processesSmallDocumentOnce() {
        RecordingProcessor delegate = new RecordingProcessor(List.of(MarkdownPostProcessingResult.markdown("整理后")));
        ChunkedMarkdownPostProcessor processor = new ChunkedMarkdownPostProcessor(delegate,
                new MarkdownChunker(new ApproximateTokenEstimator()), DEFAULT_MAX_CONTEXT_TOKENS);

        MarkdownPostProcessingResult result = processor.process(request("短文档"));

        assertThat(result.markdown()).isEqualTo("整理后");
        assertThat(delegate.requests()).hasSize(1);
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
        ChunkedMarkdownPostProcessor processor = new ChunkedMarkdownPostProcessor(delegate,
                new MarkdownChunker(new ApproximateTokenEstimator()), SMALL_CHUNK_MAX_CONTEXT_TOKENS);

        MarkdownPostProcessingResult result = processor.process(request("段落内容\n\n".repeat(LARGE_DOCUMENT_REPEAT_COUNT)));

        assertThat(result.markdown()).startsWith("第一段\n\n第二段");
        assertThat(delegate.requests()).hasSizeGreaterThan(1);
        assertThat(delegate.requests().get(SECOND_REQUEST_INDEX).ocrText()).contains("previous_context");
    }

    /**
     * 任一分片失败时应回退原始 OCR 文本。
     *
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    @Test
    void fallsBackToOriginalTextWhenAnyChunkFails() {
        RecordingProcessor delegate = new RecordingProcessor(List.of(
                MarkdownPostProcessingResult.markdown("第一段"),
                MarkdownPostProcessingResult.passthrough("原文", "LLM failed")));
        String original = "段落内容\n\n".repeat(LARGE_DOCUMENT_REPEAT_COUNT);
        ChunkedMarkdownPostProcessor processor = new ChunkedMarkdownPostProcessor(delegate,
                new MarkdownChunker(new ApproximateTokenEstimator()), SMALL_CHUNK_MAX_CONTEXT_TOKENS);

        MarkdownPostProcessingResult result = processor.process(request(original));

        assertThat(result.markdown()).isEqualTo(original);
        assertThat(result.markdownApplied()).isFalse();
        assertThat(result.warnings()).anyMatch(warning -> warning.contains("chunk"));
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
        return new MarkdownPostProcessingRequest("doc-1", "demo.txt", Map.of("source", "test"), text);
    }

    /**
     * 记录请求的测试处理器。
     *
     * @param responses 预置响应
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private static final class RecordingProcessor implements MarkdownPostProcessor {

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
}
