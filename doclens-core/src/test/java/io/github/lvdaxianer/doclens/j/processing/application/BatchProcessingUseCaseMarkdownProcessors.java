package io.github.lvdaxianer.doclens.j.processing.application;

import java.util.Map;
import java.util.LinkedHashMap;

/**
 * 批次处理 Markdown 后处理器测试替身集合。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
final class BatchProcessingUseCaseMarkdownProcessors {

    /**
     * 禁止实例化 Markdown 后处理器测试替身集合。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private BatchProcessingUseCaseMarkdownProcessors() {
    }
}

/**
 * 返回固定 Markdown 的 LLM 后处理器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
record FixedMarkdownPostProcessor(String markdown) implements MarkdownPostProcessor {

    /**
     * 返回固定 Markdown 结果。
     *
     * @param request Markdown 后处理请求
     * @return 固定 Markdown 结果
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    @Override
    public MarkdownPostProcessingResult process(MarkdownPostProcessingRequest request) {
        return MarkdownPostProcessingResult.markdown(markdown);
    }
}

/**
 * 返回带分片元数据 Markdown 的 LLM 后处理器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-13
 */
record ChunkedMetadataMarkdownPostProcessor(
        String markdown,
        int chunkCount,
        int maxContextTokens,
        int estimatedOcrTokens
) implements MarkdownPostProcessor {

    private static final int CHUNK_METADATA_CAPACITY = 4;

    /**
     * 返回带 LLM 分片观测字段的 Markdown 结果。
     *
     * @param request Markdown 后处理请求
     * @return Markdown 后处理结果
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    @Override
    public MarkdownPostProcessingResult process(MarkdownPostProcessingRequest request) {
        Map<String, Object> metadata = new LinkedHashMap<>(CHUNK_METADATA_CAPACITY);
        metadata.put("llm_chunked", true);
        metadata.put("llm_chunk_count", chunkCount);
        metadata.put("llm_max_context_tokens", maxContextTokens);
        metadata.put("llm_estimated_ocr_tokens", estimatedOcrTokens);
        return MarkdownPostProcessingResult.markdown(markdown, metadata);
    }
}

/**
 * 固定失败的 LLM 后处理器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
class FailingMarkdownPostProcessor implements MarkdownPostProcessor {

    /**
     * 抛出固定 LLM 不可用异常。
     *
     * @param request Markdown 后处理请求
     * @return 不会返回成功结果
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    @Override
    public MarkdownPostProcessingResult process(MarkdownPostProcessingRequest request) {
        throw new IllegalStateException("llm unavailable");
    }
}

/**
 * 前若干次失败、随后成功的 LLM 后处理器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
class RetryingMarkdownPostProcessor implements MarkdownPostProcessor {

    private final int failTimes;
    private final String markdown;
    private int attempts;

    /**
     * 创建重试型 LLM 后处理器。
     *
     * @param failTimes 前置失败次数
     * @param markdown 成功后返回的 Markdown
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    RetryingMarkdownPostProcessor(int failTimes, String markdown) {
        this.failTimes = failTimes;
        this.markdown = markdown;
    }

    /**
     * 执行一次 Markdown 后处理尝试。
     *
     * @param request Markdown 后处理请求
     * @return 成功时返回 Markdown 结果，失败时抛出异常
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public MarkdownPostProcessingResult process(MarkdownPostProcessingRequest request) {
        attempts++;
        if (attempts <= failTimes) {
            throw new IllegalStateException("transient llm failure");
        }
        return MarkdownPostProcessingResult.markdown(markdown);
    }

    /**
     * 获取已执行的尝试次数。
     *
     * @return 尝试次数
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    int attempts() {
        return attempts;
    }
}

/**
 * 始终失败并记录次数的 LLM 后处理器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
class CountingFailingMarkdownPostProcessor implements MarkdownPostProcessor {

    private final String message;
    private int attempts;

    /**
     * 创建始终失败的 LLM 后处理器。
     *
     * @param message 失败消息
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    CountingFailingMarkdownPostProcessor(String message) {
        this.message = message;
    }

    /**
     * 执行一次始终失败的 Markdown 后处理尝试。
     *
     * @param request Markdown 后处理请求
     * @return 不会返回成功结果
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public MarkdownPostProcessingResult process(MarkdownPostProcessingRequest request) {
        attempts++;
        throw new IllegalStateException(message);
    }

    /**
     * 获取已执行的尝试次数。
     *
     * @return 尝试次数
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    int attempts() {
        return attempts;
    }
}

/**
 * 始终失败并保留 cause 的 LLM 后处理器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-19
 */
class WrappedFailingMarkdownPostProcessor implements MarkdownPostProcessor {

    private final String message;
    private final String causeMessage;

    /**
     * 创建带 cause 的失败后处理器。
     *
     * @param message 外层失败消息
     * @param causeMessage 根因消息
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    WrappedFailingMarkdownPostProcessor(String message, String causeMessage) {
        this.message = message;
        this.causeMessage = causeMessage;
    }

    /**
     * 执行一次总是失败的 Markdown 后处理尝试。
     *
     * @param request Markdown 后处理请求
     * @return 不会返回成功结果
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    @Override
    public MarkdownPostProcessingResult process(MarkdownPostProcessingRequest request) {
        throw new IllegalStateException(message, new RuntimeException(causeMessage));
    }
}
