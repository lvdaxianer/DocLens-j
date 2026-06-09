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
}
