package io.github.lvdaxianer.doclens.j.processing.interfaces;

import java.util.List;

/**
 * LLM Markdown 配置 HTTP 操作端口。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public interface LlmMarkdownConfigOperations {

    /**
     * 查询全部 LLM Markdown 配置。
     *
     * @return LLM Markdown 配置响应列表
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    List<LlmMarkdownConfigResponse> listConfigs();

    /**
     * 保存兼容单配置入口的 LLM Markdown 配置。
     *
     * @param request LLM Markdown 配置请求
     * @return LLM Markdown 配置响应
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    LlmMarkdownConfigResponse updateConfig(LlmMarkdownConfigRequest request);

    /**
     * 创建 LLM Markdown 配置。
     *
     * @param request LLM Markdown 配置请求
     * @return LLM Markdown 配置响应
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    LlmMarkdownConfigResponse createConfig(LlmMarkdownConfigRequest request);

    /**
     * 更新指定 LLM Markdown 配置。
     *
     * @param id 配置 ID
     * @param request LLM Markdown 配置请求
     * @return LLM Markdown 配置响应
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    LlmMarkdownConfigResponse updateConfig(String id, LlmMarkdownConfigRequest request);

    /**
     * 更新指定 LLM Markdown 配置启停状态。
     *
     * @param id 配置 ID
     * @param request 启停请求
     * @return LLM Markdown 配置响应
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    LlmMarkdownConfigResponse updateEnabled(String id, LlmMarkdownConfigEnabledRequest request);

    /**
     * 设置默认 LLM Markdown 配置。
     *
     * @param id 配置 ID
     * @return LLM Markdown 配置响应
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    LlmMarkdownConfigResponse makeDefault(String id);

    /**
     * 删除 LLM Markdown 配置。
     *
     * @param id 配置 ID
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    void deleteConfig(String id);

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
