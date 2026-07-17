package io.github.lvdaxianer.doclens.j.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * caller 范围读模型 API 契约测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
class DocLensCallerScopeApiContractTest extends DocLensOcrApiContractSupport {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-06-17T10:00:00+08:00");
    private static final OffsetDateTime UPDATED_AT = OffsetDateTime.parse("2026-06-17T10:01:00+08:00");

    /**
     * 清理 caller 范围契约测试数据。
     *
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @BeforeEach
    void cleanCallerScopeRows() {
        jdbcTemplate.update("DELETE FROM ocr_documents");
        jdbcTemplate.update("DELETE FROM ocr_batches");
    }

    /**
     * Dashboard 总览只能统计当前 caller 的批次和文档。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void dashboardSummaryOnlyIncludesCurrentCallerResources() throws Exception {
        seedScopedResource(ScopedResourceSeed.owned("scope-owned-batch", "scope-owned-doc"));
        seedScopedResource(ScopedResourceSeed.foreign("scope-foreign-batch", "scope-foreign-doc"));

        MvcResult result = mockMvc.perform(authenticatedGet("/api/v1/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overview.batch_count").value(1))
                .andExpect(jsonPath("$.overview.document_count").value(1))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("recent_batches").findValuesAsText("batch_id"))
                .containsExactly("scope-owned-batch");
    }

    /**
     * OCR 批次查询访问 foreign caller 资源时按不存在处理。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void ocrBatchQueryReturnsNotFoundForForeignCallerBatch() throws Exception {
        seedScopedResource(ScopedResourceSeed.foreign("scope-foreign-batch-only", "scope-foreign-doc-only"));

        mockMvc.perform(authenticatedGet("/api/v1/batches/{batchId}", "scope-foreign-batch-only"))
                .andExpect(status().isNotFound());
    }

    /**
     * OCR 文档查询访问 foreign caller 资源时按不存在处理。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void ocrDocumentQueryReturnsNotFoundForForeignCallerDocument() throws Exception {
        seedScopedResource(ScopedResourceSeed.foreign("scope-foreign-document-batch", "scope-foreign-document"));

        mockMvc.perform(authenticatedGet("/api/v1/documents/{documentId}", "scope-foreign-document"))
                .andExpect(status().isNotFound());
    }

    /**
     * 写入带 caller 归属的批次和文档。
     *
     * @param seed 测试资源定义
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private void seedScopedResource(ScopedResourceSeed seed) {
        seedScopedBatch(seed);
        seedScopedDocument(seed);
    }

    /**
     * 写入带 caller 归属的批次。
     *
     * @param seed 测试资源定义
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private void seedScopedBatch(ScopedResourceSeed seed) {
        jdbcTemplate.update("""
                INSERT INTO ocr_batches (
                    batch_id, status, total_files, completed_files, failed_files, current_document_id,
                    current_document_name, current_stage, metadata, callback_url, idempotency_key, created_at,
                    updated_at, client_id, source_app, tenant_key
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, seed.batchId(), "completed", 1, 1, 0, seed.documentId(), seed.documentId() + ".pdf",
                "completed", "{}", null, seed.batchId() + "-idem", CREATED_AT, UPDATED_AT, seed.clientId(),
                seed.sourceApp(), seed.tenantKey());
    }

    /**
     * 写入带 caller 归属的文档。
     *
     * @param seed 测试资源定义
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private void seedScopedDocument(ScopedResourceSeed seed) {
        jdbcTemplate.update("""
                INSERT INTO ocr_documents (
                    document_id, batch_id, file_name, file_type, file_size, page_count, storage_uri, status, stage,
                    progress_percent, current_page, total_pages, adapter_name, pdf_mode, metadata, result_id,
                    error_code, error_message, sort_order, locked_by, locked_until, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, seed.documentId(), seed.batchId(), seed.documentId() + ".pdf", "pdf", 12L, 1,
                "local://uploads/" + seed.documentId() + ".pdf", "completed", "completed", 100, 1, 1,
                "stub_ocr", null, "{}", null, null, null, 0, null, null, CREATED_AT, UPDATED_AT);
    }

    /**
     * caller 范围测试资源定义。
     *
     * @param batchId 批次 ID
     * @param documentId 文档 ID
     * @param clientId caller client id
     * @param sourceApp caller source app
     * @param tenantKey caller tenant key
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private record ScopedResourceSeed(
            String batchId,
            String documentId,
            String clientId,
            String sourceApp,
            String tenantKey
    ) {

        /**
         * 创建当前 caller 的测试资源。
         *
         * @param batchId 批次 ID
         * @param documentId 文档 ID
         * @return 测试资源定义
         * @author lvdaxianerplus
         * @date 2026-06-17
         */
        private static ScopedResourceSeed owned(String batchId, String documentId) {
            return new ScopedResourceSeed(batchId, documentId, TEST_CLIENT_ID, TEST_SOURCE_APP, TEST_TENANT_KEY);
        }

        /**
         * 创建 foreign caller 的测试资源。
         *
         * @param batchId 批次 ID
         * @param documentId 文档 ID
         * @return 测试资源定义
         * @author lvdaxianerplus
         * @date 2026-06-17
         */
        private static ScopedResourceSeed foreign(String batchId, String documentId) {
            return new ScopedResourceSeed(batchId, documentId, FOREIGN_CLIENT_ID, FOREIGN_SOURCE_APP,
                    FOREIGN_TENANT_KEY);
        }
    }
}
