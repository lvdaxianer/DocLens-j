package io.github.lvdaxianer.doclens.j.processing.application;

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

    @Override
    public MarkdownPostProcessingResult process(MarkdownPostProcessingRequest request) {
        return MarkdownPostProcessingResult.markdown(markdown);
    }
}

/**
 * 固定失败的 LLM 后处理器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
class FailingMarkdownPostProcessor implements MarkdownPostProcessor {

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
