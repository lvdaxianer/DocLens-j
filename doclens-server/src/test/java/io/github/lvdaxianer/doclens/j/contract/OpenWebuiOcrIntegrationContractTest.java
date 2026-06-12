package io.github.lvdaxianer.doclens.j.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Open WebUI OCR 集成适配器契约测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
class OpenWebuiOcrIntegrationContractTest extends DocLensOcrApiContractSupport {

    /** Open WebUI OCR 集成创建批次路径。 */
    private static final String OPENWEBUI_BATCHES_PATH = "/api/v1/integrations/open-webui/ocr/batches";
    /** Open WebUI 测试内部 token。 */
    private static final String OPENWEBUI_TOKEN = "test-openwebui-token";

    /**
     * 配置 Open WebUI 集成测试 token。
     *
     * @param registry 动态属性注册表
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @DynamicPropertySource
    static void openWebuiProperties(DynamicPropertyRegistry registry) {
        registry.add("doclens.integrations.open-webui.internal-token", () -> OPENWEBUI_TOKEN);
        registry.add("doclens.auto-process-on-upload", () -> "false");
    }

    /**
     * 验证缺少内部 token 的 Open WebUI 创建请求会被拒绝。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void openWebuiCreateBatchRejectsMissingInternalToken() throws Exception {
        mockMvc.perform(multipart(OPENWEBUI_BATCHES_PATH)
                        .file(openwebuiFile())
                        .param("metadata", openwebuiMetadata())
                        .param("idempotency_key", openwebuiIdempotencyKey())
                        .param("pdf_mode", "page_image_fallback"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED_INTERNAL_CALLER"))
                .andExpect(jsonPath("$.message").value("unauthorized internal caller"))
                .andExpect(jsonPath("$.details").isMap());
    }

    /**
     * 验证 Open WebUI 创建请求会映射身份、元数据和文件响应字段。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void openWebuiCreateBatchMapsIdentityMetadataAndFiles() throws Exception {
        MvcResult created = mockMvc.perform(multipart(OPENWEBUI_BATCHES_PATH)
                        .file(openwebuiFile())
                        .header("Authorization", "Bearer " + OPENWEBUI_TOKEN)
                        .header("X-OpenWebUI-User-Id", "user_123")
                        .header("X-OpenWebUI-User-Email", "user@example.com")
                        .header("X-OpenWebUI-User-Role", "admin")
                        .header("X-OpenWebUI-Request-Id", "req_123")
                        .param("metadata", openwebuiMetadata())
                        .param("idempotency_key", openwebuiIdempotencyKey())
                        .param("pdf_mode", "page_image_fallback"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.batch_id").value(startsWith("batch_")))
                .andExpect(jsonPath("$.status").value("queued"))
                .andExpect(jsonPath("$.documents[0].document_id").value(startsWith("doc_")))
                .andExpect(jsonPath("$.documents[0].filename").value("demo.pdf"))
                .andExpect(jsonPath("$.documents[0].content_type").value("application/pdf"))
                .andExpect(jsonPath("$.documents[0].status").value("queued"))
                .andReturn();

        JsonNode body = objectMapper.readTree(created.getResponse().getContentAsString());

        assertThat(body.get("created_at").asText()).isNotBlank();
    }

    /**
     * 创建 Open WebUI 测试上传文件。
     *
     * @return 上传文件
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private MockMultipartFile openwebuiFile() {
        return new MockMultipartFile("files", "demo.pdf", "application/pdf",
                "%PDF-1.4 demo".getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 创建 Open WebUI metadata JSON。
     *
     * @return metadata JSON
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private String openwebuiMetadata() {
        return """
                {
                  "source": "open-webui",
                  "openwebui_user_id": "user_123",
                  "openwebui_file_id": "file_123",
                  "openwebui_knowledge_id": "knowledge_123",
                  "openwebui_request_id": "req_123"
                }
                """;
    }

    /**
     * 创建 Open WebUI 幂等键。
     *
     * @return 幂等键
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private String openwebuiIdempotencyKey() {
        return "openwebui:file:file_123:hash:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    }
}
