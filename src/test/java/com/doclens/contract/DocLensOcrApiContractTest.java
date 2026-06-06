package com.doclens.contract;

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
 * Public API contract tests for DocLens OCR service.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@SpringBootTest
@AutoConfigureMockMvc
class DocLensOcrApiContractTest {

    @TempDir
    static java.nio.file.Path tempDir;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Configures isolated test storage and database.
     *
     * @param registry dynamic property registry
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:file:" + tempDir.resolve("doclens-test") + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        registry.add("doclens.storage-root", () -> tempDir.resolve("storage").toString());
    }

    /**
     * Verifies upload, query, result, events, adapters, and health contracts.
     *
     * @throws Exception when request execution fails
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
        assertThat(body.get("documents").get(0).get("file_name").asText()).isEqualTo("a.pdf");

        mockMvc.perform(get("/api/v1/batches/{batchId}", batchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.bizId").value("A-1001"))
                .andExpect(jsonPath("$.progress_percent").value(100));

        mockMvc.perform(get("/api/v1/documents/{documentId}", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document_id").value(documentId))
                .andExpect(jsonPath("$.progress_percent").value(100));

        mockMvc.perform(get("/api/v1/documents/{documentId}/result", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.summary.pageCount").value(1))
                .andExpect(jsonPath("$.result.chunks").doesNotExist());

        mockMvc.perform(get("/api/v1/batches/{batchId}/events", batchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batch_id").value(batchId))
                .andExpect(jsonPath("$.events").isArray());

        mockMvc.perform(get("/api/v1/adapters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adapters[0].adapterKey").value("paddle_ocr"));

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    /**
     * Verifies invalid metadata is rejected.
     *
     * @throws Exception when request execution fails
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Test
    void batchUploadRejectsInvalidMetadataJson() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "demo.pdf", "application/pdf", "%PDF-demo".getBytes());

        mockMvc.perform(multipart("/api/v1/batches")
                        .file(file)
                        .param("metadata", "{\"bizId\":\"broken\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("invalid metadata json")));
    }

    private MvcResult uploadBatch() throws Exception {
        MockMultipartFile first = new MockMultipartFile("files", "a.pdf", "application/pdf", "%PDF-a".getBytes());
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
