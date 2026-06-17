package io.github.lvdaxianer.doclens.j.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Open WebUI OCR 集成适配器契约测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
class OpenWebuiOcrIntegrationContractTest extends OpenWebuiOcrIntegrationContractSupport {

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
                        .file(openwebuiPdfFile())
                        .header(API_KEY_HEADER, TEST_API_KEY)
                        .param("metadata", openwebuiMetadata())
                        .param("idempotency_key", openwebuiIdempotencyKey())
                        .param("pdf_mode", "page_image_fallback"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED_INTERNAL_CALLER"))
                .andExpect(jsonPath("$.message").value("unauthorized internal caller"))
                .andExpect(jsonPath("$.details").isMap());
    }

    /**
     * 验证非 multipart 创建请求也会先执行内部 token 鉴权。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void openWebuiCreateBatchRejectsPlainPostBeforeMultipartParsing() throws Exception {
        mockMvc.perform(post(OPENWEBUI_BATCHES_PATH)
                        .header(API_KEY_HEADER, TEST_API_KEY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED_INTERNAL_CALLER"))
                .andExpect(jsonPath("$.message").value("unauthorized internal caller"));
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
        MvcResult created = createOpenwebuiBatch(openwebuiMarkdownFile())
                .andExpect(jsonPath("$.batch_id").value(startsWith("batch_")))
                .andExpect(jsonPath("$.status").value("queued"))
                .andExpect(jsonPath("$.documents[0].document_id").value(startsWith("doc_")))
                .andExpect(jsonPath("$.documents[0].filename").value("demo.md"))
                .andExpect(jsonPath("$.documents[0].content_type").value("text/markdown"))
                .andExpect(jsonPath("$.documents[0].status").value("queued"))
                .andReturn();
        JsonNode body = objectMapper.readTree(created.getResponse().getContentAsString());

        assertThat(body.get("created_at").asText()).isNotBlank();
    }

    /**
     * 验证 Open WebUI 查询链路会映射批次、文档、结果、事件和健康响应。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void openWebuiQueryResultEventsAndHealthAreMapped() throws Exception {
        MvcResult created = createOpenwebuiBatch(openwebuiMarkdownFile()).andReturn();
        JsonNode body = objectMapper.readTree(created.getResponse().getContentAsString());
        String batchId = body.get("batch_id").asText();
        String documentId = body.get("documents").get(0).get("document_id").asText();

        waitForOpenwebuiBatchCompleted(batchId);

        assertOpenwebuiBatch(batchId);
        assertOpenwebuiDocument(documentId);
        assertOpenwebuiResult(documentId);
        assertOpenwebuiEvents(batchId);
        assertOpenwebuiHealth();
    }

    /**
     * 验证 Open WebUI 文档重试响应会重置状态与错误字段。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void openWebuiRetryFailedDocumentIsMapped() throws Exception {
        String documentId = "doc-openwebui-retry";

        insertFailedOpenwebuiDocument("batch-openwebui-retry", documentId);

        mockMvc.perform(authenticatedPost(OPENWEBUI_OCR_PATH + "/documents/{documentId}/retry", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document_id").value(documentId))
                .andExpect(jsonPath("$.status").value("queued"))
                .andExpect(jsonPath("$.stage").value("QUEUED"))
                .andExpect(jsonPath("$.error_code").value(""))
                .andExpect(jsonPath("$.error_message").value(""));
    }

    /**
     * 断言 Open WebUI 批次查询响应。
     *
     * @param batchId 批次 ID
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private void assertOpenwebuiBatch(String batchId) throws Exception {
        mockMvc.perform(authenticatedGet(OPENWEBUI_OCR_PATH + "/batches/{batchId}", batchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batch_id").value(batchId))
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.total_documents").value(1))
                .andExpect(jsonPath("$.completed_documents").value(1))
                .andExpect(jsonPath("$.failed_documents").value(0));
    }

    /**
     * 断言 Open WebUI 文档查询响应。
     *
     * @param documentId 文档 ID
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private void assertOpenwebuiDocument(String documentId) throws Exception {
        mockMvc.perform(authenticatedGet(OPENWEBUI_OCR_PATH + "/documents/{documentId}", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document_id").value(documentId))
                .andExpect(jsonPath("$.filename").value("demo.md"))
                .andExpect(jsonPath("$.content_type").value("text/markdown"))
                .andExpect(jsonPath("$.page_count").value(1))
                .andExpect(jsonPath("$.completed_pages").value(1));
    }

    /**
     * 断言 Open WebUI 结果查询响应。
     *
     * @param documentId 文档 ID
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private void assertOpenwebuiResult(String documentId) throws Exception {
        mockMvc.perform(authenticatedGet(OPENWEBUI_OCR_PATH + "/documents/{documentId}/result", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content_type").value("text/markdown"))
                .andExpect(jsonPath("$.text").value("# Demo\ncontent"))
                .andExpect(jsonPath("$.metadata.openwebui_user_id").value("user_123"));
    }

    /**
     * 断言 Open WebUI 事件查询响应。
     *
     * @param batchId 批次 ID
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private void assertOpenwebuiEvents(String batchId) throws Exception {
        mockMvc.perform(authenticatedGet(OPENWEBUI_OCR_PATH + "/batches/{batchId}/events", batchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batch_id").value(batchId))
                .andExpect(jsonPath("$.events").isArray());
    }

    /**
     * 断言 Open WebUI 健康检查响应。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private void assertOpenwebuiHealth() throws Exception {
        mockMvc.perform(authenticatedGet(OPENWEBUI_OCR_PATH + "/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("doclens-j"))
                .andExpect(jsonPath("$.time").isString());
    }
}
