package io.github.lvdaxianer.doclens.j.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * DocLens OCR 查询 API 契约测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
class DocLensOcrApiContractTest extends DocLensOcrApiContractSupport {

    /*
     * 该类只保留上传成功后的查询链路契约。
     * 删除契约拆到 DocLensOcrDeleteApiContractTest，
     * 上传校验和路由契约拆到 DocLensOcrUploadApiContractTest。
     */

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

        assertCreatedBatch(body, batchId, documentId);
        waitForBatchCompleted(batchId);

        assertDocumentProgress(documentId);
        assertDocumentResult(documentId);
        assertBatchEvents(batchId);
        assertAdaptersAndHealth();
    }

    /**
     * 断言批次创建响应。
     *
     * @param body 响应体
     * @param batchId 批次 ID
     * @param documentId 文档 ID
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void assertCreatedBatch(JsonNode body, String batchId, String documentId) {
        assertThat(batchId).startsWith("batch_");
        assertThat(documentId).startsWith("doc_");
        assertThat(body.get("total_files").asInt()).isEqualTo(2);
        assertThat(body.get("documents").get(0).get("file_name").asText()).isEqualTo("a.md");
    }

    /**
     * 断言文档处理进度。
     *
     * @param documentId 文档 ID
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void assertDocumentProgress(String documentId) throws Exception {
        mockMvc.perform(authenticatedGet("/api/v1/documents/{documentId}", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document_id").value(documentId))
                .andExpect(jsonPath("$.progress_percent").value(100));
    }

    /**
     * 断言文档 OCR 结果。
     *
     * @param documentId 文档 ID
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void assertDocumentResult(String documentId) throws Exception {
        mockMvc.perform(authenticatedGet("/api/v1/documents/{documentId}/result", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.summary.pageCount").value(1))
                .andExpect(jsonPath("$.result.finalText").value("# A\n正文"))
                .andExpect(jsonPath("$.result.llm_markdown_applied").value(false))
                .andExpect(jsonPath("$.result.markdownStorageUri").isString())
                .andExpect(jsonPath("$.result.chunks").doesNotExist());
    }

    /**
     * 断言批次事件响应。
     *
     * @param batchId 批次 ID
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void assertBatchEvents(String batchId) throws Exception {
        mockMvc.perform(authenticatedGet("/api/v1/batches/{batchId}/events", batchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batch_id").value(batchId))
                .andExpect(jsonPath("$.events").isArray());
    }

    /**
     * 断言适配器和健康检查响应。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void assertAdaptersAndHealth() throws Exception {
        mockMvc.perform(authenticatedGet("/api/v1/adapters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adapters[0].adapterKey").value("stub_ocr"));

        mockMvc.perform(authenticatedGet("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }
}
