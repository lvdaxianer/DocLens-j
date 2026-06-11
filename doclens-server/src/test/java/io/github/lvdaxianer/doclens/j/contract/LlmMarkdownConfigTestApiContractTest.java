package io.github.lvdaxianer.doclens.j.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigSettings;
import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigTestResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;

/**
 * LLM Markdown 配置连通性测试 API 契约测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
class LlmMarkdownConfigTestApiContractTest extends LlmMarkdownConfigApiContractSupport {

    /*
     * 该类只覆盖 /llm-markdown-config/test 探测接口。
     * 它验证健康响应、失败原因透传和空密钥沿用逻辑，
     * 不再混入保存配置的 URL 校验场景。
     *
     * 探测接口不会落库新配置，
     * 但会临时组合请求参数与已保存密钥。
     * 所以这里重点验证传给 tester 的配置语义。
     */

    /**
     * 测试接口应按 OpenAI compatible 配置执行探测且不回显 API Key。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void testConfigReportsConnectivityWithoutExposingApiKey() throws Exception {
        /*
         * 健康探测成功时，只能返回健康状态和展示消息。
         * 即使请求体带了密钥，也不能在响应中回显。
         */
        given(configTester.test(any())).willReturn(LlmMarkdownConfigTestResponse.reachable());

        String response = mockMvc.perform(post("/api/v1/llm-markdown-config/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configJson(DEFAULT_LLM_URL, "markdown-model", TEST_API_KEY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.healthy").value(true))
                .andExpect(jsonPath("$.message").value("llm markdown config is reachable"))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(response)
                .doesNotContain(TEST_API_KEY)
                .doesNotContain("api_key");
    }

    /**
     * 测试配置失败时应返回可展示的具体失败原因。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void testConfigReportsConcreteFailureMessage() throws Exception {
        /*
         * 失败消息来自后端探测器，可直接用于前端提示。
         * 这里约束接口不要把失败统一吞成泛化错误。
         */
        given(configTester.test(any())).willReturn(
                LlmMarkdownConfigTestResponse.unreachable("LLM Markdown returned HTTP 401: invalid api key"));

        mockMvc.perform(post("/api/v1/llm-markdown-config/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configJson(DEFAULT_LLM_URL, "markdown-model", TEST_API_KEY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.healthy").value(false))
                .andExpect(jsonPath("$.message").value("LLM Markdown returned HTTP 401: invalid api key"));
    }

    /**
     * 测试配置时 API Key 留空应沿用已保存旧密钥。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void testConfigKeepsSavedCredentialWhenApiKeyBlank() throws Exception {
        /*
         * 探测时密钥留空代表复用已保存密钥。
         * ArgumentCaptor 验证传入 tester 的实际配置，
         * 比只看 HTTP 响应更能覆盖该分支。
         */
        saveConfig(DEFAULT_LLM_URL, "markdown-model", OLD_TEST_API_KEY);
        given(configTester.test(any())).willReturn(LlmMarkdownConfigTestResponse.reachable());

        mockMvc.perform(post("/api/v1/llm-markdown-config/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configJson(DEFAULT_LLM_URL, "markdown-model", "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.healthy").value(true));

        ArgumentCaptor<LlmMarkdownConfigSettings> captor = ArgumentCaptor.forClass(LlmMarkdownConfigSettings.class);
        verify(configTester).test(captor.capture());
        assertThat(captor.getValue().apiKey()).isEqualTo(OLD_TEST_API_KEY);
    }
}
