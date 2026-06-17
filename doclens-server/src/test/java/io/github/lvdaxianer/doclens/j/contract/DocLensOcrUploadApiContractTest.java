package io.github.lvdaxianer.doclens.j.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

/**
 * DocLens OCR 上传 API 契约测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
class DocLensOcrUploadApiContractTest extends DocLensOcrApiContractSupport {

    /** 上传文件表单字段名。 */
    private static final String FILES_PART_NAME = "files";
    /** 非法元数据字段值。 */
    private static final String BROKEN_METADATA_JSON = "{\"bizId\":\"broken\"";

    /*
     * 该类只覆盖上传入口的校验和路由持久化契约。
     * 成功上传后的查询链路留在主契约测试，
     * 删除副作用留在删除契约测试。
     *
     * 这些场景看起来很小，但它们守住两个关键边界：
     * 一个是用户输入校验，一个是 OCR 调度策略落库。
     * 拆出来后可以避免主契约测试继续横向膨胀。
     */

    /**
     * 验证非法元数据会被拒绝。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Test
    void batchUploadRejectsInvalidMetadataJson() throws Exception {
        mockMvc.perform(authenticatedMultipart("/api/v1/batches")
                        .file(invalidMetadataFile())
                        .param("metadata", BROKEN_METADATA_JSON))
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
     * 创建非法元数据场景使用的上传文件。
     *
     * @return 上传文件
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private MockMultipartFile invalidMetadataFile() {
        return new MockMultipartFile(FILES_PART_NAME, "demo.md", "text/markdown", "# Demo".getBytes());
    }
}
