package io.github.lvdaxianer.doclens.j.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
}
