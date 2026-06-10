package io.github.lvdaxianer.doclens.j.processing.interfaces;

/**
 * LLM Markdown 配置 HTTP 操作端口。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public interface LlmMarkdownConfigOperations {

    /**
     * 查询 LLM Markdown 配置。
     *
     * @return LLM Markdown 配置响应
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    LlmMarkdownConfigResponse getConfig();

    /**
     * 保存 LLM Markdown 配置。
     *
     * @param request LLM Markdown 配置请求
     * @return LLM Markdown 配置响应
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    LlmMarkdownConfigResponse updateConfig(LlmMarkdownConfigRequest request);

    /**
     * 测试 LLM Markdown 配置连通性。
     *
     * @param request LLM Markdown 配置请求
     * @return 连通性测试结果
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    LlmMarkdownConfigTestResultResponse testConfig(LlmMarkdownConfigRequest request);
}
