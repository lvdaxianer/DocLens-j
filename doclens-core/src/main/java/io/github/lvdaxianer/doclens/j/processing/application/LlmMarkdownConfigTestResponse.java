package io.github.lvdaxianer.doclens.j.processing.application;

/**
 * LLM Markdown 配置连通性测试结果。
 *
 * @param healthy 是否连通
 * @param message 测试结果消息
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public record LlmMarkdownConfigTestResponse(boolean healthy, String message) {

    private static final String SUCCESS_MESSAGE = "llm markdown config is reachable";
    private static final String FAILURE_MESSAGE = "llm markdown config test failed";

    /**
     * 创建成功测试结果。
     *
     * @return 成功测试结果
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public static LlmMarkdownConfigTestResponse reachable() {
        return new LlmMarkdownConfigTestResponse(true, SUCCESS_MESSAGE);
    }

    /**
     * 创建失败测试结果。
     *
     * @return 失败测试结果
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public static LlmMarkdownConfigTestResponse unreachable() {
        return new LlmMarkdownConfigTestResponse(false, FAILURE_MESSAGE);
    }
}
