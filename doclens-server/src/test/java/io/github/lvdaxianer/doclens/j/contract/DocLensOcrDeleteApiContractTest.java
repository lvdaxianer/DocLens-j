package io.github.lvdaxianer.doclens.j.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * DocLens OCR 删除 API 契约测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
class DocLensOcrDeleteApiContractTest extends DocLensOcrApiContractSupport {

    /*
     * 该类集中验证文档和批次删除契约。
     * 删除会影响数据库、对象存储和 Dashboard 查询，
     * 从主查询契约中拆出后更容易审查副作用。
     */

    /**
     * 验证已完成文档可以删除，并同步清理结果、存储与批次统计。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void completedDocumentCanBeDeletedAndBatchSummaryStaysConsistent() throws Exception {
        UploadedDocument document = uploadCompletedFirstDocument();
        Map<String, Object> storageColumns = loadDocumentStorageColumns(document.documentId());
        Path sourcePath = storagePath(String.valueOf(storageColumns.get("STORAGE_URI")));
        Path markdownPath = storagePath(String.valueOf(storageColumns.get("MARKDOWN_STORAGE_URI")));

        deleteDocument(document.documentId());

        assertBatchSummaryAfterSingleDelete(document.batchId());
        assertDocumentNotFound(document.documentId());
        assertDocumentRowsDeleted(document.documentId());
        assertThat(Files.exists(sourcePath)).isFalse();
        assertThat(Files.exists(markdownPath)).isFalse();
    }

    /**
     * 验证删除批次内最后一个文档后，空批次不会继续出现在详情或总览接口中。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void deletingLastCompletedDocumentRemovesBatchFromDashboardApis() throws Exception {
        UploadedDocument document = uploadCompletedSingleDocument();

        deleteDocument(document.documentId());

        assertBatchNotFound(document.batchId());
        assertDashboardDoesNotContainBatch(document.batchId());
    }

    /**
     * 验证可删除状态的整个批次可以通过批次接口一键删除。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void completedBatchCanBeDeletedByBatchEndpoint() throws Exception {
        UploadedBatch batch = uploadCompletedBatch();

        mockMvc.perform(delete("/api/v1/batches/{batchId}", batch.batchId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batch_id").value(batch.batchId()))
                .andExpect(jsonPath("$.status").value("deleted"))
                .andExpect(jsonPath("$.deleted_documents").value(2));

        assertBatchNotFound(batch.batchId());
        assertOnlyDocumentNotFound(batch.firstDocumentId());
        assertOnlyDocumentNotFound(batch.secondDocumentId());
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
        insertProcessingDocument(batchId, documentId);

        mockMvc.perform(delete("/api/v1/documents/{documentId}", documentId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("not deletable")))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("processing")));

        assertThat(countDocuments(documentId)).isEqualTo(1);
    }

    /**
     * 上传并等待完成后返回首个文档。
     *
     * @return 已上传文档
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private UploadedDocument uploadCompletedFirstDocument() throws Exception {
        UploadedBatch batch = uploadCompletedBatch();
        return new UploadedDocument(batch.batchId(), batch.firstDocumentId());
    }

    /**
     * 上传并等待完成后返回单文档批次。
     *
     * @return 已上传文档
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private UploadedDocument uploadCompletedSingleDocument() throws Exception {
        MvcResult created = uploadSingleFileBatch();
        JsonNode body = objectMapper.readTree(created.getResponse().getContentAsString());
        String batchId = body.get("batch_id").asText();
        String documentId = body.get("documents").get(0).get("document_id").asText();
        waitForBatchCompleted(batchId, "A-EMPTY-1");
        return new UploadedDocument(batchId, documentId);
    }

    /**
     * 上传并等待完成后返回双文档批次。
     *
     * @return 已上传批次
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private UploadedBatch uploadCompletedBatch() throws Exception {
        MvcResult created = uploadBatch();
        JsonNode body = objectMapper.readTree(created.getResponse().getContentAsString());
        String batchId = body.get("batch_id").asText();
        waitForBatchCompleted(batchId);
        return new UploadedBatch(batchId, body.get("documents").get(0).get("document_id").asText(),
                body.get("documents").get(1).get("document_id").asText());
    }

    /**
     * 删除文档并断言成功。
     *
     * @param documentId 文档 ID
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void deleteDocument(String documentId) throws Exception {
        mockMvc.perform(delete("/api/v1/documents/{documentId}", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document_id").value(documentId))
                .andExpect(jsonPath("$.status").value("deleted"));
    }

    /**
     * 断言单文档删除后的批次摘要。
     *
     * @param batchId 批次 ID
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void assertBatchSummaryAfterSingleDelete(String batchId) throws Exception {
        mockMvc.perform(get("/api/v1/batches/{batchId}", batchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_files").value(1))
                .andExpect(jsonPath("$.completed_files").value(1))
                .andExpect(jsonPath("$.failed_files").value(0))
                .andExpect(jsonPath("$.status").value("completed"));
    }

    /**
     * 断言文档和结果接口都返回未找到。
     *
     * @param documentId 文档 ID
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void assertDocumentNotFound(String documentId) throws Exception {
        assertOnlyDocumentNotFound(documentId);
        mockMvc.perform(get("/api/v1/documents/{documentId}/result", documentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("not found")));
    }

    /**
     * 断言文档接口返回未找到。
     *
     * @param documentId 文档 ID
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void assertOnlyDocumentNotFound(String documentId) throws Exception {
        mockMvc.perform(get("/api/v1/documents/{documentId}", documentId))
                .andExpect(status().isNotFound());
    }

    /**
     * 断言批次接口返回未找到。
     *
     * @param batchId 批次 ID
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void assertBatchNotFound(String batchId) throws Exception {
        mockMvc.perform(get("/api/v1/batches/{batchId}", batchId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("not found")));
    }

    /**
     * 断言 Dashboard 最近批次不包含目标批次。
     *
     * @param batchId 批次 ID
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void assertDashboardDoesNotContainBatch(String batchId) throws Exception {
        MvcResult summaryResult = mockMvc.perform(get("/api/v1/dashboard/summary"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode summary = objectMapper.readTree(summaryResult.getResponse().getContentAsString());
        assertThat(summary.path("recent_batches"))
                .allSatisfy(batchNode -> assertThat(batchNode.path("batch_id").asText()).isNotEqualTo(batchId));
    }

    /**
     * 断言文档和结果数据库行已删除。
     *
     * @param documentId 文档 ID
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void assertDocumentRowsDeleted(String documentId) {
        assertThat(countDocuments(documentId)).isZero();
        assertThat(countResults(documentId)).isZero();
    }

    /**
     * 已上传文档。
     *
     * @param batchId 批次 ID
     * @param documentId 文档 ID
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private record UploadedDocument(String batchId, String documentId) {
    }

    /**
     * 已上传双文档批次。
     *
     * @param batchId 批次 ID
     * @param firstDocumentId 首个文档 ID
     * @param secondDocumentId 第二个文档 ID
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private record UploadedBatch(String batchId, String firstDocumentId, String secondDocumentId) {
    }
}
