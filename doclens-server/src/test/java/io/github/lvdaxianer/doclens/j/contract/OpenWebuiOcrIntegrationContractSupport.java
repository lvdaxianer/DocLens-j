package io.github.lvdaxianer.doclens.j.contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Open WebUI OCR 集成适配器契约测试支持。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
abstract class OpenWebuiOcrIntegrationContractSupport extends DocLensOcrApiContractSupport {

    /** Open WebUI OCR 集成创建批次路径。 */
    protected static final String OPENWEBUI_BATCHES_PATH = "/api/v1/integrations/open-webui/ocr/batches";
    /** Open WebUI OCR 集成根路径。 */
    protected static final String OPENWEBUI_OCR_PATH = "/api/v1/integrations/open-webui/ocr";
    /** Open WebUI 测试内部 token。 */
    protected static final String OPENWEBUI_TOKEN = "test-openwebui-token";
    /** Open WebUI 鉴权头名称。 */
    private static final String AUTHORIZATION_HEADER = "Authorization";
    /** Open WebUI 用户 ID 头名称。 */
    private static final String OPENWEBUI_USER_ID_HEADER = "X-OpenWebUI-User-Id";
    /** Open WebUI 请求 ID 头名称。 */
    private static final String OPENWEBUI_REQUEST_ID_HEADER = "X-OpenWebUI-Request-Id";
    /** Open WebUI 测试用户 ID。 */
    private static final String OPENWEBUI_USER_ID = "user_123";
    /** Open WebUI 测试请求 ID。 */
    private static final String OPENWEBUI_REQUEST_ID = "req_123";
    /** Open WebUI 后台处理等待次数。 */
    private static final int OPENWEBUI_WAIT_ATTEMPTS = 20;
    /** Open WebUI 后台处理单次等待毫秒数。 */
    private static final int OPENWEBUI_WAIT_MILLIS = 100;

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
    }

    /**
     * 创建 Open WebUI PDF 上传文件。
     *
     * @return 上传文件
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    protected MockMultipartFile openwebuiPdfFile() {
        return new MockMultipartFile("files", "demo.pdf", "application/pdf",
                "%PDF-1.4 demo".getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 创建 Open WebUI Markdown 上传文件。
     *
     * @return 上传文件
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    protected MockMultipartFile openwebuiMarkdownFile() {
        return new MockMultipartFile("files", "demo.md", "text/markdown",
                "# Demo\ncontent".getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 创建 Open WebUI 批次。
     *
     * @param file 上传文件
     * @return MockMvc 结果动作
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    protected org.springframework.test.web.servlet.ResultActions createOpenwebuiBatch(MockMultipartFile file)
            throws Exception {
        return mockMvc.perform(multipart(OPENWEBUI_BATCHES_PATH).file(file)
                        .header(CALLER_PARTITION_HEADER, TEST_CALLER_PARTITION_KEY)
                        .header("Authorization", "Bearer " + OPENWEBUI_TOKEN)
                        .header("X-OpenWebUI-User-Id", "user_123")
                        .header("X-OpenWebUI-User-Email", "user@example.com")
                        .header("X-OpenWebUI-User-Role", "admin")
                        .header("X-OpenWebUI-Request-Id", "req_123")
                        .param("metadata", openwebuiMetadata())
                        .param("idempotency_key", openwebuiIdempotencyKey())
                        .param("pdf_mode", "page_image_fallback"))
                .andExpect(status().isAccepted());
    }

    /**
     * 创建 Open WebUI metadata JSON。
     *
     * @return metadata JSON
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    protected String openwebuiMetadata() {
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
    protected String openwebuiIdempotencyKey() {
        return "openwebui:file:file_123:hash:" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 等待 Open WebUI 批次完成。
     *
     * @param batchId 批次 ID
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    protected void waitForOpenwebuiBatchCompleted(String batchId) throws Exception {
        for (int attempt = 0; attempt < OPENWEBUI_WAIT_ATTEMPTS; attempt++) {
            if (isOpenwebuiBatchCompleted(batchId)) {
                return;
            } else {
                Thread.sleep(OPENWEBUI_WAIT_MILLIS);
            }
        }
        mockMvc.perform(authenticatedGet("/api/v1/batches/{batchId}", batchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progress_percent").value(100));
    }

    /**
     * 判断 Open WebUI 批次是否完成。
     *
     * @param batchId 批次 ID
     * @return 是否完成
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private boolean isOpenwebuiBatchCompleted(String batchId) throws Exception {
        MvcResult result = mockMvc.perform(authenticatedGet("/api/v1/batches/{batchId}", batchId))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("progress_percent").asInt() == 100;
    }

    /**
     * 插入可被 Open WebUI 重试的失败文档。
     *
     * @param batchId 批次 ID
     * @param documentId 文档 ID
     * @throws IOException 写入测试文件失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    protected void insertFailedOpenwebuiDocument(String batchId, String documentId) throws IOException {
        OffsetDateTime now = OffsetDateTime.parse("2026-06-10T11:00:00+08:00");
        writeOpenwebuiRetrySourceFile();
        insertFailedOpenwebuiBatch(batchId, documentId, now);
        insertFailedOpenwebuiDocumentRow(batchId, documentId, now);
    }

    /**
     * 写入重试测试需要的本地存储源文件。
     *
     * @throws IOException 写入测试文件失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private void writeOpenwebuiRetrySourceFile() throws IOException {
        Path retryPath = tempDir.resolve("storage").resolve("uploads").resolve("retry.md");
        Files.createDirectories(retryPath.getParent());
        Files.writeString(retryPath, "# Retry\ncontent", StandardCharsets.UTF_8);
    }

    /**
     * 插入 Open WebUI 失败批次。
     *
     * @param batchId 批次 ID
     * @param documentId 文档 ID
     * @param now 当前时间
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private void insertFailedOpenwebuiBatch(String batchId, String documentId, OffsetDateTime now) {
        jdbcTemplate.update("""
                INSERT INTO ocr_batches (
                    batch_id, status, total_files, completed_files, failed_files, current_document_id,
                    current_document_name, current_stage, metadata, callback_url, idempotency_key, created_at, updated_at,
                    client_id, source_app, tenant_key
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, batchId, "failed", 1, 0, 1, documentId, "retry.md", "ocr_images", "{}",
                null, null, now, now, TEST_CLIENT_ID, TEST_SOURCE_APP, TEST_TENANT_KEY);
    }

    /**
     * 插入 Open WebUI 失败文档行。
     *
     * @param batchId 批次 ID
     * @param documentId 文档 ID
     * @param now 当前时间
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private void insertFailedOpenwebuiDocumentRow(String batchId, String documentId, OffsetDateTime now) {
        jdbcTemplate.update("""
                INSERT INTO ocr_documents (
                    document_id, batch_id, file_name, file_type, file_size, page_count, storage_uri, status, stage,
                    progress_percent, current_page, total_pages, adapter_name, pdf_mode, metadata, result_id,
                    error_code, error_message, sort_order, locked_by, locked_until, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, documentId, batchId, "retry.md", "markdown", 12L, 1, "local://uploads/retry.md",
                "failed", "ocr_images", 100, 1, 1, "stub_ocr", null, openwebuiMetadata(), null,
                "OCR_FAILED", "ocr failed", 0, null, null, now, now);
    }

    /**
     * 创建携带 Open WebUI 鉴权头的 GET 请求。
     *
     * @param uriTemplate URI 模板
     * @param uriVars URI 参数
     * @return GET 请求构建器
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder authenticatedGet(
            String uriTemplate,
            Object... uriVars
    ) {
        return attachOpenwebuiHeaders(get(uriTemplate, uriVars), TEST_CALLER_PARTITION_KEY);
    }

    /**
     * 创建携带 Open WebUI 鉴权头的 POST 请求。
     *
     * @param uriTemplate URI 模板
     * @param uriVars URI 参数
     * @return POST 请求构建器
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder authenticatedPost(
            String uriTemplate,
            Object... uriVars
    ) {
        return attachOpenwebuiHeaders(post(uriTemplate, uriVars), TEST_CALLER_PARTITION_KEY);
    }

    /**
     * 创建携带外部 caller 分区键和 Open WebUI 鉴权头的 GET 请求。
     *
     * @param uriTemplate URI 模板
     * @param uriVars URI 参数
     * @return GET 请求构建器
     * @author lvdaxianerplus
     * @date 2026-06-22
     */
    public org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder foreignAuthenticatedGet(
            String uriTemplate,
            Object... uriVars
    ) {
        return attachOpenwebuiHeaders(get(uriTemplate, uriVars), FOREIGN_CALLER_PARTITION_KEY);
    }

    /**
     * 创建携带外部 caller 分区键和 Open WebUI 鉴权头的 POST 请求。
     *
     * @param uriTemplate URI 模板
     * @param uriVars URI 参数
     * @return POST 请求构建器
     * @author lvdaxianerplus
     * @date 2026-06-22
     */
    public org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder foreignAuthenticatedPost(
            String uriTemplate,
            Object... uriVars
    ) {
        return attachOpenwebuiHeaders(post(uriTemplate, uriVars), FOREIGN_CALLER_PARTITION_KEY);
    }

    /**
     * 追加 Open WebUI 鉴权与分区请求头。
     *
     * @param builder 请求构建器
     * @param callerPartitionKey caller 分区键
     * @return 请求构建器
     * @author lvdaxianerplus
     * @date 2026-06-22
     */
    private MockHttpServletRequestBuilder attachOpenwebuiHeaders(
            MockHttpServletRequestBuilder builder,
            String callerPartitionKey
    ) {
        return builder.header(CALLER_PARTITION_HEADER, callerPartitionKey)
                .header(AUTHORIZATION_HEADER, "Bearer " + OPENWEBUI_TOKEN)
                .header(OPENWEBUI_USER_ID_HEADER, OPENWEBUI_USER_ID)
                .header(OPENWEBUI_REQUEST_ID_HEADER, OPENWEBUI_REQUEST_ID);
    }
}
