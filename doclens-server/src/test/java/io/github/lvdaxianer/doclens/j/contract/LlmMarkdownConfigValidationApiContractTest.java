package io.github.lvdaxianer.doclens.j.contract;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * LLM Markdown 后处理配置校验 API 契约测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
class LlmMarkdownConfigValidationApiContractTest extends LlmMarkdownConfigApiContractSupport {

    /*
     * 该类只覆盖保存配置前的输入校验。
     * 读写成功路径和连通性探测不放在这里，
     * 保持校验失败的契约边界清晰。
     */

    /**
     * URL 和模型名称必须成对配置。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void updateConfigRequiresUrlAndModelTogether() throws Exception {
        mockMvc.perform(authenticatedPut("/api/v1/llm-markdown-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configJson(DEFAULT_LLM_URL, "", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("llm markdown model is required"));
    }

    /**
     * URL 必须是 HTTP 或 HTTPS 地址。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void updateConfigRejectsNonHttpUrl() throws Exception {
        mockMvc.perform(authenticatedPut("/api/v1/llm-markdown-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configJson("file:///tmp/llm.sock", "markdown-model", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("llm markdown url must be http or https URL"));
    }

    /**
     * URL 格式非法时应返回统一的 HTTP/HTTPS 校验错误。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void updateConfigRejectsMalformedUrl() throws Exception {
        mockMvc.perform(authenticatedPut("/api/v1/llm-markdown-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configJson("http:// bad-url", "markdown-model", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("llm markdown url must be http or https URL"));
    }

    /**
     * HTTP URL 缺少 host 时应拒绝保存。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void updateConfigRejectsHttpUrlWithoutHost() throws Exception {
        mockMvc.perform(authenticatedPut("/api/v1/llm-markdown-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configJson("http:foo", "markdown-model", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("llm markdown url must be http or https URL"));
    }

    /**
     * 最大上下文 token 数过小时应拒绝保存。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    @Test
    void updateConfigRejectsSmallMaxContextTokens() throws Exception {
        mockMvc.perform(authenticatedPut("/api/v1/llm-markdown-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "api_type": "openai",
                                  "url": "%s",
                                  "model": "markdown-model",
                                  "credential_env_var": "MINIMAX_API_KEY",
                                  "max_context_tokens": 999,
                                  "max_concurrency": 1,
                                  "request_interval_millis": 1000
                                }
                                """.formatted(DEFAULT_LLM_URL)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("llm markdown max context tokens must be at least 1000"));
    }

    /**
     * 凭证环境变量名格式非法时应拒绝保存。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    @Test
    void updateConfigRejectsInvalidCredentialEnvVarName() throws Exception {
        mockMvc.perform(authenticatedPut("/api/v1/llm-markdown-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configJson(DEFAULT_LLM_URL, "markdown-model", "bad-name")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("credential env var must match [A-Z_][A-Z0-9_]*"));
    }
}
