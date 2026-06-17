package io.github.lvdaxianer.doclens.j.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * LLM Markdown 后处理配置兼容读写 API 契约测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
class LlmMarkdownConfigApiContractTest extends LlmMarkdownConfigApiContractSupport {

    /**
     * 初始列表应为空且不包含 api_key 字段。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void getConfigReturnsEmptyListWithoutApiKey() throws Exception {
        String response = mockMvc.perform(authenticatedGet("/api/v1/llm-markdown-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(response).doesNotContain("api_key");
    }

    /**
     * 兼容保存配置后响应只展示凭证环境变量名。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void updateConfigReturnsCredentialEnvVarWithoutApiKey() throws Exception {
        String response = mockMvc.perform(authenticatedPut("/api/v1/llm-markdown-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configJson(DEFAULT_LLM_URL, "markdown-model", TEST_CREDENTIAL_ENV_VAR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value(DEFAULT_LLM_URL))
                .andExpect(jsonPath("$.api_type").value("openai"))
                .andExpect(jsonPath("$.model").value("markdown-model"))
                .andExpect(jsonPath("$.max_context_tokens").value(16000))
                .andExpect(jsonPath("$.max_concurrency").value(2))
                .andExpect(jsonPath("$.request_interval_millis").value(1500))
                .andExpect(jsonPath("$.credential_env_var").value(TEST_CREDENTIAL_ENV_VAR))
                .andExpect(jsonPath("$.credential_configured").value(true))
                .andExpect(jsonPath("$.api_key").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(response).doesNotContain("api_key");
    }

    /**
     * 编辑时凭证环境变量名留空应沿用旧配置。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void updateConfigKeepsCredentialEnvVarWhenBlank() throws Exception {
        saveConfig(DEFAULT_LLM_URL, "markdown-model", OLD_TEST_CREDENTIAL_ENV_VAR);

        String response = mockMvc.perform(authenticatedPut("/api/v1/llm-markdown-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configJson(DEFAULT_LLM_URL, "markdown-model-v2", "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("markdown-model-v2"))
                .andExpect(jsonPath("$.credential_env_var").value(OLD_TEST_CREDENTIAL_ENV_VAR))
                .andExpect(jsonPath("$.credential_configured").value(true))
                .andExpect(jsonPath("$.api_key").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(response).doesNotContain("api_key");
    }

    /**
     * 重新填写凭证环境变量名应覆盖旧配置且响应仍不回显 api_key。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void updateConfigOverwritesCredentialEnvVarWhenProvided() throws Exception {
        saveConfig(DEFAULT_LLM_URL, "markdown-model", OLD_TEST_CREDENTIAL_ENV_VAR);

        String response = mockMvc.perform(authenticatedPut("/api/v1/llm-markdown-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configJson(DEFAULT_LLM_URL, "markdown-model", NEW_TEST_CREDENTIAL_ENV_VAR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credential_env_var").value(NEW_TEST_CREDENTIAL_ENV_VAR))
                .andExpect(jsonPath("$.credential_configured").value(true))
                .andExpect(jsonPath("$.api_key").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(response)
                .doesNotContain(OLD_TEST_CREDENTIAL_ENV_VAR)
                .doesNotContain("api_key");
    }

    /**
     * OpenAI compatible URL 应按用户输入原样保存。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void updateConfigKeepsOpenAiCompatibleUrlUnchanged() throws Exception {
        mockMvc.perform(authenticatedPut("/api/v1/llm-markdown-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configJson(DASHSCOPE_COMPATIBLE_URL, "qwen-vl-ocr-2025-11-20", TEST_CREDENTIAL_ENV_VAR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value(DASHSCOPE_COMPATIBLE_URL))
                .andExpect(jsonPath("$.model").value("qwen-vl-ocr-2025-11-20"))
                .andExpect(jsonPath("$.credential_configured").value(true));
    }

    /**
     * Anthropic URL 应按用户输入原样保存并返回协议类型。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void updateConfigKeepsAnthropicUrlUnchanged() throws Exception {
        mockMvc.perform(authenticatedPut("/api/v1/llm-markdown-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(anthropicConfigJson(ANTHROPIC_URL, "MiniMax-M3", TEST_CREDENTIAL_ENV_VAR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.api_type").value("anthropic"))
                .andExpect(jsonPath("$.url").value(ANTHROPIC_URL))
                .andExpect(jsonPath("$.model").value("MiniMax-M3"))
                .andExpect(jsonPath("$.credential_configured").value(true));
    }

    /**
     * 兼容配置启停状态应随保存并返回前端。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void updateConfigPersistsEnabledFlag() throws Exception {
        mockMvc.perform(authenticatedPut("/api/v1/llm-markdown-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(disabledOpenAiConfigJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(authenticatedGet("/api/v1/llm-markdown-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].enabled").value(false))
                .andExpect(jsonPath("$[0].max_context_tokens").value(16000))
                .andExpect(jsonPath("$[0].max_concurrency").value(2))
                .andExpect(jsonPath("$[0].request_interval_millis").value(1500));
    }
}
