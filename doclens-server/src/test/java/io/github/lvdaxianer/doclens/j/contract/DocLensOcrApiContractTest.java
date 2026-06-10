package io.github.lvdaxianer.doclens.j.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * DocLens OCR 服务的公开 API 契约测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@SpringBootTest
@AutoConfigureMockMvc
class DocLensOcrApiContractTest {

    private static final int PROCESSING_WAIT_ATTEMPTS = 20;
    private static final int PROCESSING_WAIT_MILLIS = 100;

    @TempDir
    static java.nio.file.Path tempDir;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 配置隔离的测试存储与数据库。
     *
     * @param registry 动态属性注册表
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:file:" + tempDir.resolve("doclens-test") + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        registry.add("doclens.storage-root", () -> tempDir.resolve("storage").toString());
        registry.add("doclens.adapter.default-key", () -> "stub_ocr");
        registry.add("doclens.paddle-ocr.enabled", () -> "false");
    }

    /**
     * 验证上传、查询、结果、事件、适配器和健康检查契约。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Test
    void batchDocumentResultAndEventsAreQueryable() throws Exception {
        MvcResult created = uploadBatch();
        JsonNode body = objectMapper.readTree(created.getResponse().getContentAsString());
        String batchId = body.get("batch_id").asText();
        String documentId = body.get("documents").get(0).get("document_id").asText();

        assertThat(batchId).startsWith("batch_");
        assertThat(documentId).startsWith("doc_");
        assertThat(body.get("total_files").asInt()).isEqualTo(2);
        assertThat(body.get("documents").get(0).get("file_name").asText()).isEqualTo("a.md");

        waitForBatchCompleted(batchId);

        mockMvc.perform(get("/api/v1/documents/{documentId}", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document_id").value(documentId))
                .andExpect(jsonPath("$.progress_percent").value(100));

        mockMvc.perform(get("/api/v1/documents/{documentId}/result", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.summary.pageCount").value(1))
                .andExpect(jsonPath("$.result.finalText").value("# A\n正文"))
                .andExpect(jsonPath("$.result.llm_markdown_applied").value(false))
                .andExpect(jsonPath("$.result.markdownStorageUri").isString())
                .andExpect(jsonPath("$.result.chunks").doesNotExist());

        mockMvc.perform(get("/api/v1/batches/{batchId}/events", batchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batch_id").value(batchId))
                .andExpect(jsonPath("$.events").isArray());

        mockMvc.perform(get("/api/v1/adapters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adapters[0].adapterKey").value("stub_ocr"));

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    /**
     * 验证已完成文档可以删除，并同步清理结果、存储与批次统计。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void completedDocumentCanBeDeletedAndBatchSummaryStaysConsistent() throws Exception {
        MvcResult created = uploadBatch();
        JsonNode body = objectMapper.readTree(created.getResponse().getContentAsString());
        String batchId = body.get("batch_id").asText();
        String documentId = body.get("documents").get(0).get("document_id").asText();

        waitForBatchCompleted(batchId);

        Map<String, Object> storageColumns = loadDocumentStorageColumns(documentId);
        Path sourcePath = storagePath(String.valueOf(storageColumns.get("STORAGE_URI")));
        Path markdownPath = storagePath(String.valueOf(storageColumns.get("MARKDOWN_STORAGE_URI")));

        mockMvc.perform(delete("/api/v1/documents/{documentId}", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document_id").value(documentId))
                .andExpect(jsonPath("$.status").value("deleted"));

        mockMvc.perform(get("/api/v1/batches/{batchId}", batchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_files").value(1))
                .andExpect(jsonPath("$.completed_files").value(1))
                .andExpect(jsonPath("$.failed_files").value(0))
                .andExpect(jsonPath("$.status").value("completed"));

        mockMvc.perform(get("/api/v1/documents/{documentId}", documentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("not found")));

        mockMvc.perform(get("/api/v1/documents/{documentId}/result", documentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("not found")));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ocr_documents WHERE document_id = ?", Integer.class, documentId))
                .isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ocr_results WHERE document_id = ?", Integer.class, documentId))
                .isZero();
        assertThat(Files.exists(sourcePath)).isFalse();
        assertThat(Files.exists(markdownPath)).isFalse();
    }

    /**
     * 验证 processing 文档删除会被明确拒绝。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void processingDocumentDeleteIsRejected() throws Exception {
        String batchId = "batch-processing-delete";
        String documentId = "doc-processing-delete";
        String now = "2026-06-10T11:00:00+08:00";
        jdbcTemplate.update("""
                INSERT INTO ocr_batches (
                    batch_id, status, total_files, completed_files, failed_files, current_document_id,
                    current_document_name, current_stage, metadata, callback_url, idempotency_key, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, batchId, "processing", 1, 0, 0, documentId, "processing.pdf", "ocr_images", "{}",
                null, null, now, now);
        jdbcTemplate.update("""
                INSERT INTO ocr_documents (
                    document_id, batch_id, file_name, file_type, file_size, page_count, storage_uri, status, stage,
                    progress_percent, current_page, total_pages, adapter_name, pdf_mode, metadata, result_id,
                    error_code, error_message, sort_order, locked_by, locked_until, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, documentId, batchId, "processing.pdf", "pdf", 12L, 1, "local://uploads/processing.pdf",
                "processing", "ocr_images", 50, 1, 2, "stub_ocr", null, "{}", null, null, null, 0, null, null,
                now, now);

        mockMvc.perform(delete("/api/v1/documents/{documentId}", documentId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("not deletable")))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("processing")));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ocr_documents WHERE document_id = ?", Integer.class, documentId))
                .isEqualTo(1);
    }

    /**
     * 等待后台批次处理完成。
     *
     * @param batchId 批次 ID
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private void waitForBatchCompleted(String batchId) throws Exception {
        for (int attempt = 0; attempt < PROCESSING_WAIT_ATTEMPTS; attempt++) {
            MvcResult result = mockMvc.perform(get("/api/v1/batches/{batchId}", batchId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.metadata.bizId").value("A-1001"))
                    .andReturn();
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            if (body.get("progress_percent").asInt() == 100) {
                return;
            } else {
                sleepBeforeNextAttempt();
            }
        }
        mockMvc.perform(get("/api/v1/batches/{batchId}", batchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progress_percent").value(100));
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
     * 验证非法元数据会被拒绝。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Test
    void batchUploadRejectsInvalidMetadataJson() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "demo.md", "text/markdown", "# Demo".getBytes());

        mockMvc.perform(multipart("/api/v1/batches")
                        .file(file)
                        .param("metadata", "{\"bizId\":\"broken\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("invalid metadata json")));
    }

    /**
     * 验证 multipart OCR 路由字段会持久化到文档任务快照。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void batchUploadPersistsOcrRoutePolicy() throws Exception {
        MvcResult created = uploadBatchWithOcrRoutePolicy();
        JsonNode body = objectMapper.readTree(created.getResponse().getContentAsString());
        String documentId = body.get("documents").get(0).get("document_id").asText();

        Map<String, Object> routeColumns = loadRouteColumns(documentId);

        assertThat(routeColumns)
                .containsEntry("OCR_ROUTING_MODE", "MODEL_LOAD_BALANCE")
                .containsEntry("OCR_MODEL_KEY", "paddle_ocr")
                .containsEntry("OCR_LOAD_BALANCE_STRATEGY", "least-inflight");
    }

    /**
     * 查询文档任务 OCR 路由字段。
     *
     * @param documentId 文档 ID
     * @return OCR 路由字段
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private Map<String, Object> loadRouteColumns(String documentId) {
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
    private Map<String, Object> loadDocumentStorageColumns(String documentId) {
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
    private Path storagePath(String storageUri) {
        String objectKey = storageUri.replace("local://", "");
        return tempDir.resolve("storage").resolve(objectKey);
    }

    private MvcResult uploadBatch() throws Exception {
        MockMultipartFile first = new MockMultipartFile("files", "a.md", "text/markdown", "# A\n正文".getBytes());
        MockMultipartFile second = new MockMultipartFile("files", "b.png", "image/png", "png-bytes".getBytes());
        return mockMvc.perform(multipart("/api/v1/batches")
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
     * 上传携带 OCR 路由策略的测试批次。
     *
     * @return 创建批次响应
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private MvcResult uploadBatchWithOcrRoutePolicy() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "route.png", "image/png", "png-bytes".getBytes());
        return mockMvc.perform(multipart("/api/v1/batches")
                        .file(file)
                        .param("metadata", "{\"bizId\":\"OCR-ROUTE\"}")
                        .param("idempotency_key", "idem-ocr-route-" + UUID.randomUUID())
                        .param("ocrRoutingMode", "MODEL_LOAD_BALANCE")
                        .param("ocrModelKey", "paddle_ocr")
                        .param("ocrLoadBalanceStrategy", "least-inflight"))
                .andExpect(status().isAccepted())
                .andReturn();
    }
}
