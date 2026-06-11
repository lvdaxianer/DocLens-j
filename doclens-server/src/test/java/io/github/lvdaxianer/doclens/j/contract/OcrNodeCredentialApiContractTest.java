package io.github.lvdaxianer.doclens.j.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * OCR 节点在线凭证 API 契约测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
class OcrNodeCredentialApiContractTest extends OcrNodeApiContractSupport {

    /** 创建在线节点时使用的测试 API Key。 */
    private static final String CREATE_FAKE_API_KEY = "test-key-create";
    /** 更新在线节点时已存在的测试 API Key。 */
    private static final String EXISTING_FAKE_API_KEY = "test-key-existing";

    /*
     * 该测试类集中验证在线节点凭证输入和响应脱敏规则。
     * 这些场景对安全更敏感，单独拆出后更容易审查是否泄漏 API Key。
     * 测试值使用 fake key 常量，不使用真实密钥或类似真实密钥的字面量。
     *
     * 每个测试都只检查 HTTP 契约：
     * 创建时不回显密钥、更新时保留旧密钥、离线转在线必须提供密钥。
     * 这样安全规则变化时，失败点会比原始大文件更清楚。
     *
     * 响应断言同时检查字段不存在和原始值不出现，
     * 防止后端误把凭证明文放进扩展字段或错误消息。
     */

    /**
     * 在线 DashScope 节点应保存渠道和模型配置但不回显 API Key。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void createOnlineDashScopeNodeDoesNotExposeApiKey() throws Exception {
        // 创建在线节点时发送 fake key，但响应体不允许出现该值。
        String response = mockMvc.perform(post("/api/v1/ocr-models/{modelKey}/nodes", "paddle_ocr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(onlineNodeJson("dashscope-main", "qwen-vl-ocr-2025-11-20", CREATE_FAKE_API_KEY)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deployment_type").value("ONLINE"))
                .andExpect(jsonPath("$.channel_key").value("aliyun_bailian_dashscope"))
                .andExpect(jsonPath("$.provider_model").value("qwen-vl-ocr-2025-11-20"))
                .andExpect(jsonPath("$.credential_configured").value(true))
                .andExpect(jsonPath("$.api_key").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(response).doesNotContain(CREATE_FAKE_API_KEY);
    }

    /**
     * 编辑在线节点时空 API Key 应沿用旧密钥配置。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void updateOnlineNodeKeepsCredentialWhenApiKeyBlank() throws Exception {
        // 空 API Key 表示保持原凭证，响应仍不能泄漏旧 fake key。
        String nodeId = createOnlineNode("dashscope-edit", "qwen-vl-ocr-2025-11-20", EXISTING_FAKE_API_KEY);

        // 更新请求只修改名称，凭证字段提交空字符串。
        String response = mockMvc.perform(put("/api/v1/ocr-nodes/{nodeId}", nodeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(onlineNodeJson("dashscope-edit-renamed", "qwen-vl-ocr-2025-11-20", "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credential_configured").value(true))
                .andExpect(jsonPath("$.name").value("dashscope-edit-renamed"))
                .andExpect(jsonPath("$.api_key").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(response)
                .doesNotContain(EXISTING_FAKE_API_KEY)
                .doesNotContain("api_key");
    }

    /**
     * 离线节点切换为在线节点时空 API Key 应被拒绝。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void updateOfflineNodeToOnlineRequiresApiKey() throws Exception {
        // 离线节点没有历史在线凭证，切换为在线时必须显式提供 API Key。
        String nodeId = createNode("paddle-to-online", "10.100.30.220", 8080);

        mockMvc.perform(put("/api/v1/ocr-nodes/{nodeId}", nodeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(onlineNodeJson("paddle-to-online", "qwen-vl-ocr-2025-11-20", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("ocr online api key is required"));
    }
}
