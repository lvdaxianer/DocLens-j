package io.github.lvdaxianer.doclens.j.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * OCR 批次幂等键回查契约测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-14
 */
class DocLensOcrBatchIdempotencyKeyContractTest extends DocLensOcrApiContractSupport {

    private static final String BATCH_PATH = "/api/v1/batches/by-idempotency-key/{idempotencyKey}";
    private static final String CREATED_AT = "2026-06-14T12:00:00+08:00";
    private static final String UPDATED_AT = "2026-06-14T12:01:00+08:00";

    /**
     * 验证 queued 批次可以按 idempotency key 回查。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    @Test
    void queuedBatchIsQueryableByIdempotencyKey() throws Exception {
        seedReconciliationBatch("batch-queued", "file-queued", "queued", 1, 0, 0, "doc-queued",
                "queued.pdf", "queued", "doc-queued", null, null, null);

        MvcResult result = performLookup("file-queued").andExpect(status().isOk())
                .andExpect(jsonPath("$.batch_id").value("batch-queued"))
                .andExpect(jsonPath("$.idempotency_key").value("file-queued"))
                .andExpect(jsonPath("$.status").value("queued"))
                .andExpect(jsonPath("$.documents[0].status").value("queued"))
                .andExpect(jsonPath("$.documents[0].stage").value("queued"))
                .andReturn();

        assertSingleDocumentSnapshot(result, "doc-queued", "queued.pdf");
    }

    /**
     * 验证 processing 批次可以按 idempotency key 回查。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    @Test
    void processingBatchIsQueryableByIdempotencyKey() throws Exception {
        seedReconciliationBatch("batch-processing", "file-processing", "processing", 1, 0, 0, "doc-processing",
                "processing.pdf", "ocr_images", "doc-processing", null, null, null);

        performLookup("file-processing").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("processing"))
                .andExpect(jsonPath("$.documents[0].status").value("processing"))
                .andExpect(jsonPath("$.documents[0].stage").value("ocr"));
    }

    /**
     * 验证 completed 批次可以按 idempotency key 回查。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    @Test
    void completedBatchIsQueryableByIdempotencyKey() throws Exception {
        seedReconciliationBatch("batch-completed", "file-completed", "completed", 1, 1, 0, "doc-completed",
                "completed.pdf", "completed", "doc-completed", "result-doc-completed", null, null);

        performLookup("file-completed").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.completed_files").value(1))
                .andExpect(jsonPath("$.documents[0].status").value("completed"))
                .andExpect(jsonPath("$.documents[0].stage").value("completed"))
                .andExpect(jsonPath("$.documents[0].result_id").value("result-doc-completed"));
    }

    /**
     * 验证 failed 批次可以按 idempotency key 回查并返回错误信息。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    @Test
    void failedBatchIsQueryableByIdempotencyKey() throws Exception {
        seedReconciliationBatch("batch-failed", "file-failed", "failed", 1, 0, 1, "doc-failed",
                "failed.pdf", "ocr_failed", "doc-failed", null, "OCR_FAILED", "OCR engine timeout");

        performLookup("file-failed").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.failed_files").value(1))
                .andExpect(jsonPath("$.documents[0].status").value("failed"))
                .andExpect(jsonPath("$.documents[0].stage").value("failed"))
                .andExpect(jsonPath("$.documents[0].error.code").value("OCR_FAILED"))
                .andExpect(jsonPath("$.documents[0].error.message").value("OCR engine timeout"));
    }

    /**
     * 验证缺少对应批次时返回稳定的 404 载荷。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    @Test
    void missingBatchReturnsStable404Payload() throws Exception {
        MvcResult result = performLookup("missing-file")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("batch not found"))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("data").isNull()).isTrue();
    }

    /**
     * 断言单文档回查响应的文档快照。
     *
     * @param result HTTP 结果
     * @param documentId 文档 ID
     * @param fileName 文件名
     * @throws Exception 解析失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    private void assertSingleDocumentSnapshot(MvcResult result, String documentId, String fileName)
            throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("documents")).isNotNull();
        assertThat(body.get("documents").size()).isEqualTo(1);
        assertThat(body.get("documents").get(0).get("document_id").asText()).isEqualTo(documentId);
        assertThat(body.get("documents").get(0).get("file_name").asText()).isEqualTo(fileName);
    }

    /**
     * 查询 reconciliation 批次。
     *
     * @param idempotencyKey 幂等键
     * @return 请求动作
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    private org.springframework.test.web.servlet.ResultActions performLookup(String idempotencyKey)
            throws Exception {
        return mockMvc.perform(get(BATCH_PATH, idempotencyKey));
    }

    /**
     * 写入回查测试批次和文档。
     *
     * @param batchId 批次 ID
     * @param idempotencyKey 幂等键
     * @param batchStatus 批次状态
     * @param totalFiles 总文件数
     * @param completedFiles 已完成文件数
     * @param failedFiles 失败文件数
     * @param documentId 文档 ID
     * @param fileName 文件名
     * @param documentStage 文档阶段
     * @param currentDocumentId 当前文档 ID
     * @param resultId 结果 ID
     * @param errorCode 错误码
     * @param errorMessage 错误消息
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    private void seedReconciliationBatch(
            String batchId,
            String idempotencyKey,
            String batchStatus,
            int totalFiles,
            int completedFiles,
            int failedFiles,
            String documentId,
            String fileName,
            String documentStage,
            String currentDocumentId,
            String resultId,
            String errorCode,
            String errorMessage
    ) {
        seedBatchRow(batchId, idempotencyKey, batchStatus, totalFiles, completedFiles, failedFiles, currentDocumentId,
                fileName, documentStage);
        seedDocumentRow(batchId, documentId, fileName, documentStage, resultId, errorCode, errorMessage);
    }

    /**
     * 写入批次表测试行。
     *
     * @param batchId 批次 ID
     * @param idempotencyKey 幂等键
     * @param batchStatus 批次状态
     * @param totalFiles 总文件数
     * @param completedFiles 已完成文件数
     * @param failedFiles 失败文件数
     * @param currentDocumentId 当前文档 ID
     * @param currentDocumentName 当前文档名
     * @param currentStage 当前阶段
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    private void seedBatchRow(
            String batchId,
            String idempotencyKey,
            String batchStatus,
            int totalFiles,
            int completedFiles,
            int failedFiles,
            String currentDocumentId,
            String currentDocumentName,
            String currentStage
    ) {
        jdbcTemplate.update("""
                INSERT INTO ocr_batches (
                    batch_id, status, total_files, completed_files, failed_files, current_document_id,
                    current_document_name, current_stage, metadata, callback_url, idempotency_key, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, batchId, batchStatus, totalFiles, completedFiles, failedFiles, currentDocumentId,
                currentDocumentName, currentStage, "{}", null, idempotencyKey, CREATED_AT, UPDATED_AT);
    }

    /**
     * 写入文档表测试行。
     *
     * @param batchId 批次 ID
     * @param documentId 文档 ID
     * @param fileName 文件名
     * @param stage 文档阶段
     * @param resultId 结果 ID
     * @param errorCode 错误码
     * @param errorMessage 错误消息
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    private void seedDocumentRow(
            String batchId,
            String documentId,
            String fileName,
            String stage,
            String resultId,
            String errorCode,
            String errorMessage
    ) {
        jdbcTemplate.update("""
                INSERT INTO ocr_documents (
                    document_id, batch_id, file_name, file_type, file_size, page_count, storage_uri, status, stage,
                    progress_percent, current_page, total_pages, adapter_name, pdf_mode, metadata, result_id,
                    error_code, error_message, sort_order, locked_by, locked_until, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, documentId, batchId, fileName, "pdf", 12L, 1, "local://uploads/" + fileName,
                toDocumentStatus(stage, errorCode), stage, progressPercent(stage), currentPage(stage), totalPages(stage),
                "stub_ocr", null, "{}", resultId, errorCode, errorMessage, 0, null, null, CREATED_AT, UPDATED_AT);
    }

    /**
     * 将测试阶段映射成文档状态。
     *
     * @param stage 文档阶段
     * @return 文档状态
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    private String toDocumentStatus(String stage, String errorCode) {
        if (errorCode != null) {
            return "failed";
        } else {
            return switch (stage) {
                case "queued" -> "queued";
                case "ocr_images" -> "processing";
                case "completed" -> "completed";
                default -> "processing";
            };
        }
    }

    /**
     * 返回测试进度百分比。
     *
     * @param stage 文档阶段
     * @return 进度百分比
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    private int progressPercent(String stage) {
        return switch (stage) {
            case "queued" -> 0;
            case "completed" -> 100;
            case "ocr_failed" -> 100;
            default -> 50;
        };
    }

    /**
     * 返回测试当前页数。
     *
     * @param stage 文档阶段
     * @return 当前页数
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    private int currentPage(String stage) {
        return switch (stage) {
            case "queued" -> 0;
            case "completed", "ocr_failed" -> 1;
            default -> 1;
        };
    }

    /**
     * 返回测试总页数。
     *
     * @param stage 文档阶段
     * @return 总页数
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    private int totalPages(String stage) {
        return 1;
    }
}
