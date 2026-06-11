package io.github.lvdaxianer.doclens.j.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * LLM Markdown 后处理配置读写 API 契约测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
class LlmMarkdownConfigApiContractTest extends LlmMarkdownConfigApiContractSupport {

    /*
     * 该类只覆盖配置查询、保存和密钥脱敏。
     * URL 校验场景拆到 Validation 契约测试，
     * /test 连通性探测拆到 Test 契约测试。
     *
     * 这里的核心安全边界是“配置可见但密钥不可见”。
     * 每个成功路径都只允许返回 credential_configured，
     * 不允许把 api_key 字段或密钥内容带回前端。
     */

    /**
     * 初始状态应返回未配置且不包含 API Key 字段。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void getConfigReturnsUnconfiguredStateWithoutApiKey() throws Exception {
        /*
         * 初始响应用于约束前端编辑态。
         * 未配置时也不能出现空 api_key 字段，
         * 避免前端误以为该字段可编辑回显。
         */
        String response = mockMvc.perform(get("/api/v1/llm-markdown-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value(""))
                .andExpect(jsonPath("$.api_type").value("openai"))
                .andExpect(jsonPath("$.model").value(""))
                .andExpect(jsonPath("$.credential_configured").value(false))
                .andExpect(jsonPath("$.api_key").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(response).doesNotContain("api_key");
    }

    /**
     * 保存配置后响应不能回显 API Key。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void updateConfigDoesNotExposeApiKey() throws Exception {
        /*
         * 提交测试密钥占位值后，接口只返回是否已配置。
         * 响应体字符串再做一次兜底检查，
         * 防止 JSON 序列化策略意外泄漏敏感字段。
         */
        String response = mockMvc.perform(put("/api/v1/llm-markdown-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configJson(DEFAULT_LLM_URL, "markdown-model", TEST_API_KEY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value(DEFAULT_LLM_URL))
                .andExpect(jsonPath("$.api_type").value("openai"))
                .andExpect(jsonPath("$.model").value("markdown-model"))
                .andExpect(jsonPath("$.credential_configured").value(true))
                .andExpect(jsonPath("$.api_key").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(response)
                .doesNotContain(TEST_API_KEY)
                .doesNotContain("api_key");
    }

    /**
     * 编辑时 API Key 留空应沿用旧密钥配置。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void updateConfigKeepsCredentialWhenApiKeyBlank() throws Exception {
        /*
         * 编辑页留空密钥表示沿用旧配置。
         * 该路径要同时验证更新非敏感字段成功，
         * 以及旧密钥不会被回显。
         */
        saveConfig(DEFAULT_LLM_URL, "markdown-model", OLD_TEST_API_KEY);

        String response = mockMvc.perform(put("/api/v1/llm-markdown-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configJson(DEFAULT_LLM_URL, "markdown-model-v2", "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("markdown-model-v2"))
                .andExpect(jsonPath("$.credential_configured").value(true))
                .andExpect(jsonPath("$.api_key").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(response)
                .doesNotContain(OLD_TEST_API_KEY)
                .doesNotContain("api_key");
    }

    /**
     * 重新填写 API Key 应覆盖旧密钥且响应仍不回显。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void updateConfigOverwritesCredentialWhenApiKeyProvided() throws Exception {
        /*
         * 非空密钥表示用户主动轮换。
         * 测试同时检查旧密钥和新密钥，
         * 保证轮换前后都不会出现在响应体。
         */
        saveConfig(DEFAULT_LLM_URL, "markdown-model", OLD_TEST_API_KEY);

        String response = mockMvc.perform(put("/api/v1/llm-markdown-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configJson(DEFAULT_LLM_URL, "markdown-model", NEW_TEST_API_KEY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credential_configured").value(true))
                .andExpect(jsonPath("$.api_key").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(response)
                .doesNotContain(OLD_TEST_API_KEY)
                .doesNotContain(NEW_TEST_API_KEY)
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
        /*
         * 用户现在输入完整地址。
         * 后端不再替用户追加 /v1 或其它路径，
         * 因此保存和返回都应保持原样。
         */
        mockMvc.perform(put("/api/v1/llm-markdown-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configJson(DASHSCOPE_COMPATIBLE_URL, "qwen-vl-ocr-2025-11-20", TEST_API_KEY)))
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
        /*
         * Anthropic 协议同样保存完整地址。
         * api_type 只决定后处理器协议，
         * 不应改变用户提交的 URL。
         */
        mockMvc.perform(put("/api/v1/llm-markdown-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configJson("anthropic", ANTHROPIC_URL, "MiniMax-M3", TEST_API_KEY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.api_type").value("anthropic"))
                .andExpect(jsonPath("$.url").value(ANTHROPIC_URL))
                .andExpect(jsonPath("$.model").value("MiniMax-M3"))
                .andExpect(jsonPath("$.credential_configured").value(true));
    }
}
