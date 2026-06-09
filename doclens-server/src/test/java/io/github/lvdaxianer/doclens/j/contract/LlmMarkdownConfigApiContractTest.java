package io.github.lvdaxianer.doclens.j.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigTestResponse;
import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigTester;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * LLM Markdown 后处理配置 API 契约测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
@SpringBootTest
@AutoConfigureMockMvc
class LlmMarkdownConfigApiContractTest {

    @TempDir
    static java.nio.file.Path tempDir;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LlmMarkdownConfigTester configTester;

    /**
     * 配置隔离的测试数据库和存储目录。
     *
     * @param registry 动态属性注册表
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:file:" + tempDir.resolve("llm-config-api") + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        registry.add("doclens.storage-root", () -> tempDir.resolve("storage").toString());
        registry.add("doclens.paddle-ocr.enabled", () -> "false");
    }

    /**
     * 初始状态应返回未配置且不包含 API Key 字段。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void getConfigReturnsUnconfiguredStateWithoutApiKey() throws Exception {
        // 初始响应用于约束前端编辑态，不能依赖缺省 API Key 回显。
        // 这里同时覆盖数据库尚未写入单例配置时的默认视图。
        String response = mockMvc.perform(get("/api/v1/llm-markdown-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value(""))
                .andExpect(jsonPath("$.model").value(""))
                .andExpect(jsonPath("$.credential_configured").value(false))
                .andExpect(jsonPath("$.api_key").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        // 原始响应文本也要检查，避免 Jackson 字段策略意外输出敏感字段。
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
        // 提交真实密钥形态的值，验证接口只返回是否已配置。
        // URL 使用 HTTPS，确保成功路径不会被 URL 校验拦截。
        String response = mockMvc.perform(put("/api/v1/llm-markdown-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configJson("https://llm.example.com/v1/chat/completions", "markdown-model",
                                "sk-llm-secret")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://llm.example.com/v1/chat/completions"))
                .andExpect(jsonPath("$.model").value("markdown-model"))
                .andExpect(jsonPath("$.credential_configured").value(true))
                .andExpect(jsonPath("$.api_key").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        // 响应体不能包含密钥原文，也不能出现可被前端误用的 api_key 字段。
        assertThat(response)
                .doesNotContain("sk-llm-secret")
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
        // 先保存旧密钥，再用空密钥编辑模型名称，模拟前端“留空沿用”。
        // 这个契约保证编辑页可以用空密码框提交非敏感配置变更。
        saveConfig("https://llm.example.com/v1/chat/completions", "markdown-model", "sk-old-secret");

        String response = mockMvc.perform(put("/api/v1/llm-markdown-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configJson("https://llm.example.com/v1/chat/completions", "markdown-model-v2", "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("markdown-model-v2"))
                .andExpect(jsonPath("$.credential_configured").value(true))
                .andExpect(jsonPath("$.api_key").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        // 沿用旧密钥只通过 credential_configured 表达，不回传旧密钥。
        assertThat(response)
                .doesNotContain("sk-old-secret")
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
        // 非空密钥代表用户主动覆盖，应替换旧密钥但仍保持响应脱敏。
        // 响应只暴露 credential_configured，密钥轮换结果由后端持久化负责。
        saveConfig("https://llm.example.com/v1/chat/completions", "markdown-model", "sk-old-secret");

        String response = mockMvc.perform(put("/api/v1/llm-markdown-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configJson("https://llm.example.com/v1/chat/completions", "markdown-model",
                                "sk-new-secret")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credential_configured").value(true))
                .andExpect(jsonPath("$.api_key").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        // 新旧密钥都不能在前端响应中出现，避免编辑页或网络面板泄漏。
        assertThat(response)
                .doesNotContain("sk-old-secret")
                .doesNotContain("sk-new-secret")
                .doesNotContain("api_key");
    }

    /**
     * URL 和模型名称必须成对配置。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void updateConfigRequiresUrlAndModelTogether() throws Exception {
        // URL 与模型是启用 LLM 的最小必要配置，缺少模型时应直接拒绝。
        // 这样可以避免保存一个运行时看似启用但无法发起请求的半配置。
        mockMvc.perform(put("/api/v1/llm-markdown-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configJson("https://llm.example.com/v1/chat/completions", "", "")))
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
        // file URL 不能进入运行时 HTTP 调用，避免本地资源语义混入远程接口配置。
        // 计划要求 LLM 配置为 URL、模型和可选 API Key，这里的 URL 固定为 HTTP 调用目标。
        mockMvc.perform(put("/api/v1/llm-markdown-config")
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
        // 畸形 URL 应返回产品级错误文案，而不是泄漏 JDK URI 解析细节。
        mockMvc.perform(put("/api/v1/llm-markdown-config")
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
        // http:foo 虽有 scheme 但不是可调用 endpoint，保存后会导致运行时失败。
        // 缺少 host 的地址不应落库，否则后处理器构造请求时才会暴露问题。
        mockMvc.perform(put("/api/v1/llm-markdown-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configJson("http:foo", "markdown-model", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("llm markdown url must be http or https URL"));
    }

    /**
     * DashScope 基础兼容地址应自动补全 chat completions 路径。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void updateConfigCompletesDashScopeCompatibleBaseUrl() throws Exception {
        mockMvc.perform(put("/api/v1/llm-markdown-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configJson("https://dashscope.aliyuncs.com/compatible-mode/v1",
                                "qwen-vl-ocr-2025-11-20", "sk-dashscope-secret")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url")
                        .value("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"))
                .andExpect(jsonPath("$.model").value("qwen-vl-ocr-2025-11-20"))
                .andExpect(jsonPath("$.credential_configured").value(true));
    }

    /**
     * 测试接口应按 OpenAI compatible 配置执行探测且不回显 API Key。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void testConfigReportsConnectivityWithoutExposingApiKey() throws Exception {
        given(configTester.test(any())).willReturn(LlmMarkdownConfigTestResponse.reachable());

        String response = mockMvc.perform(post("/api/v1/llm-markdown-config/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configJson("https://llm.example.com/v1/chat/completions", "markdown-model",
                                "sk-test-secret")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.healthy").value(true))
                .andExpect(jsonPath("$.message").value("llm markdown config is reachable"))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(response)
                .doesNotContain("sk-test-secret")
                .doesNotContain("api_key");
    }

    /**
     * 保存 LLM Markdown 配置。
     *
     * @param url 接口地址
     * @param model 模型名称
     * @param apiKey API Key
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private void saveConfig(String url, String model, String apiKey) throws Exception {
        // 保存辅助方法只验证成功路径，响应脱敏由独立契约测试覆盖。
        mockMvc.perform(put("/api/v1/llm-markdown-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configJson(url, model, apiKey)))
                .andExpect(status().isOk());
    }

    /**
     * 创建配置请求 JSON。
     *
     * @param url 接口地址
     * @param model 模型名称
     * @param apiKey API Key
     * @return 请求 JSON
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private String configJson(String url, String model, String apiKey) {
        // 保持 JSON 字段名与公开 API 契约一致，特别是 snake_case 的 api_key。
        return """
                {
                  "url": "%s",
                  "model": "%s",
                  "api_key": "%s"
                }
                """.formatted(url, model, apiKey);
    }
}
