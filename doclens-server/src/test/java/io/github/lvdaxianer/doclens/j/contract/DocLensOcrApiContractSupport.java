package io.github.lvdaxianer.doclens.j.contract;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.testsupport.PostgreSqlTestContainerSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * DocLens OCR API 契约测试共享支持。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class DocLensOcrApiContractSupport implements CallerCredentialContractSupport {

    /** 后台处理等待次数。 */
    private static final int PROCESSING_WAIT_ATTEMPTS = 20;
    /** 后台处理单次等待毫秒数。 */
    private static final int PROCESSING_WAIT_MILLIS = 100;
    /** PostgreSQL 契约测试数据库。 */
    private static final PostgreSQLContainer<?> POSTGRESQL =
            PostgreSqlTestContainerSupport.createStartedContainer("doclens_contract");
    /** 临时目录用于隔离数据库和对象存储。 */
    @TempDir
    static java.nio.file.Path tempDir;

    /** MockMvc 测试客户端。 */
    @Autowired
    protected MockMvc mockMvc;
    /** JSON 解析器。 */
    @Autowired
    protected ObjectMapper objectMapper;
    /** JDBC 查询工具。 */
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    /**
     * 配置隔离的测试存储与数据库。
     *
     * @param registry 动态属性注册表
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry registry) {
        PostgreSqlTestContainerSupport.registerDatasource(registry, POSTGRESQL);
        registry.add("doclens.storage-root", () -> tempDir.resolve("storage").toString());
        registry.add("doclens.adapter.default-key", () -> "stub_ocr");
        registry.add("doclens.paddle-ocr.enabled", () -> "false");
        registry.add("doclens.clients.credentials[0].client-id", () -> TEST_CLIENT_ID);
        registry.add("doclens.clients.credentials[0].source-app", () -> TEST_SOURCE_APP);
        registry.add("doclens.clients.credentials[0].tenant-key", () -> TEST_TENANT_KEY);
        registry.add("doclens.clients.credentials[0].api-key", () -> TEST_API_KEY);
        registry.add("doclens.clients.credentials[1].client-id", () -> FOREIGN_CLIENT_ID);
        registry.add("doclens.clients.credentials[1].source-app", () -> FOREIGN_SOURCE_APP);
        registry.add("doclens.clients.credentials[1].tenant-key", () -> FOREIGN_TENANT_KEY);
        registry.add("doclens.clients.credentials[1].api-key", () -> FOREIGN_API_KEY);
    }

    /**
     * 等待后台批次处理完成。
     *
     * @param batchId 批次 ID
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    protected void waitForBatchCompleted(String batchId) throws Exception {
        waitForBatchCompleted(batchId, "A-1001");
    }

    /**
     * 等待后台批次处理完成。
     *
     * @param batchId 批次 ID
     * @param expectedBizId 预期业务 ID
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    protected void waitForBatchCompleted(String batchId, String expectedBizId) throws Exception {
        for (int attempt = 0; attempt < PROCESSING_WAIT_ATTEMPTS; attempt++) {
            MvcResult result = mockMvc.perform(authenticatedGet("/api/v1/batches/{batchId}", batchId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.metadata.bizId").value(expectedBizId))
                    .andReturn();
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            if (body.get("progress_percent").asInt() == 100) {
                return;
            } else {
                sleepBeforeNextAttempt();
            }
        }
        mockMvc.perform(authenticatedGet("/api/v1/batches/{batchId}", batchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progress_percent").value(100));
    }

    /**
     * 上传默认双文档测试批次。
     *
     * @return 创建批次响应
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    protected MvcResult uploadBatch() throws Exception {
        MockMultipartFile first = new MockMultipartFile("files", "a.md", "text/markdown", "# A\n正文".getBytes());
        MockMultipartFile second = new MockMultipartFile("files", "b.md", "text/markdown", "# B\n正文".getBytes());
        return mockMvc.perform(authenticatedMultipart("/api/v1/batches")
                        .file(first)
                        .file(second)
                        .param("metadata", "{\"bizId\":\"A-1001\",\"source\":\"frontend-upload\",\"operator\":\"u123\"}")
                        .param("callback_url", "https://frontend.example.com/ocr-callback")
                        .param("idempotency_key", "idem-doclens-contract-" + UUID.randomUUID())
                        .param("pdf_mode", "page_image_fallback"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("queued"))
                .andReturn();
    }

    /**
     * 上传仅包含一个文档的测试批次。
     *
     * @return 创建批次响应
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    protected MvcResult uploadSingleFileBatch() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "single.md", "text/markdown", "# Single\n正文".getBytes());
        return mockMvc.perform(authenticatedMultipart("/api/v1/batches")
                        .file(file)
                        .param("metadata", "{\"bizId\":\"A-EMPTY-1\",\"source\":\"frontend-upload\"}")
                        .param("idempotency_key", "idem-doclens-single-" + UUID.randomUUID()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("queued"))
                .andReturn();
    }

    /**
     * 上传携带 OCR 路由策略的测试批次。
     *
     * @return 创建批次响应
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    protected MvcResult uploadBatchWithOcrRoutePolicy() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "route.png", "image/png", "png-bytes".getBytes());
        return mockMvc.perform(authenticatedMultipart("/api/v1/batches")
                        .file(file)
                        .param("metadata", "{\"bizId\":\"OCR-ROUTE\"}")
                        .param("idempotency_key", "idem-ocr-route-" + UUID.randomUUID())
                        .param("ocrRoutingMode", "MODEL_LOAD_BALANCE")
                        .param("ocrModelKey", "paddle_ocr")
                        .param("ocrLoadBalanceStrategy", "least-inflight"))
                .andExpect(status().isAccepted())
                .andReturn();
    }

    /**
     * 查询文档任务 OCR 路由字段。
     *
     * @param documentId 文档 ID
     * @return OCR 路由字段
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    protected Map<String, Object> loadRouteColumns(String documentId) {
        return jdbcTemplate.queryForMap("""
                SELECT ocr_routing_mode, ocr_model_key, ocr_load_balance_strategy
                FROM ocr_documents
                WHERE document_id = ?
                LIMIT 1
                """, documentId);
    }

    /**
     * 查询文档和结果存储字段。
     *
     * @param documentId 文档 ID
     * @return 存储字段
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    protected Map<String, Object> loadDocumentStorageColumns(String documentId) {
        return jdbcTemplate.queryForMap("""
                SELECT d.storage_uri, r.markdown_storage_uri
                FROM ocr_documents d
                JOIN ocr_results r ON r.document_id = d.document_id
                WHERE d.document_id = ?
                LIMIT 1
                """, documentId);
    }

    /**
     * 将 local:// 存储 URI 转换为测试文件路径。
     *
     * @param storageUri 存储 URI
     * @return 存储文件路径
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    protected Path storagePath(String storageUri) {
        String objectKey = storageUri.replace("local://", "");
        return tempDir.resolve("storage").resolve(objectKey);
    }

    /**
     * 插入 processing 状态文档。
     *
     * @param batchId 批次 ID
     * @param documentId 文档 ID
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    protected void insertProcessingDocument(String batchId, String documentId) {
        OffsetDateTime now = OffsetDateTime.parse("2026-06-10T11:00:00+08:00");
        insertProcessingBatch(batchId, documentId, now);
        insertProcessingDocumentRow(batchId, documentId, now);
    }

    /**
     * 查询文档行数。
     *
     * @param documentId 文档 ID
     * @return 文档行数
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    protected int countDocuments(String documentId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ocr_documents WHERE document_id = ?", Integer.class, documentId);
    }

    /**
     * 查询结果行数。
     *
     * @param documentId 文档 ID
     * @return 结果行数
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    protected int countResults(String documentId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ocr_results WHERE document_id = ?", Integer.class, documentId);
    }

    /**
     * 等待下一次状态查询。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private void sleepBeforeNextAttempt() {
        try {
            Thread.sleep(PROCESSING_WAIT_MILLIS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 插入 processing 批次。
     *
     * @param batchId 批次 ID
     * @param documentId 文档 ID
     * @param now 当前时间
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void insertProcessingBatch(String batchId, String documentId, OffsetDateTime now) {
        jdbcTemplate.update("""
                INSERT INTO ocr_batches (
                    batch_id, status, total_files, completed_files, failed_files, current_document_id,
                    current_document_name, current_stage, metadata, callback_url, idempotency_key, created_at, updated_at,
                    client_id, source_app, tenant_key
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, batchId, "processing", 1, 0, 0, documentId, "processing.pdf", "ocr_images", "{}",
                null, null, now, now, TEST_CLIENT_ID, TEST_SOURCE_APP, TEST_TENANT_KEY);
    }

    /**
     * 插入 processing 文档行。
     *
     * @param batchId 批次 ID
     * @param documentId 文档 ID
     * @param now 当前时间
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void insertProcessingDocumentRow(String batchId, String documentId, OffsetDateTime now) {
        jdbcTemplate.update("""
                INSERT INTO ocr_documents (
                    document_id, batch_id, file_name, file_type, file_size, page_count, storage_uri, status, stage,
                    progress_percent, current_page, total_pages, adapter_name, pdf_mode, metadata, result_id,
                    error_code, error_message, sort_order, locked_by, locked_until, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, documentId, batchId, "processing.pdf", "pdf", 12L, 1, "local://uploads/processing.pdf",
                "processing", "ocr_images", 50, 1, 2, "stub_ocr", null, "{}", null, null, null, 0, null, null,
                now, now);
    }
}
