package io.github.lvdaxianer.doclens.j.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * OCR 节点基础管理 API 契约测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
class OcrNodeApiContractTest extends OcrNodeApiContractSupport {

    /*
     * 该测试类只保留离线 OCR 节点的基础管理契约。
     * 在线凭证契约已移动到 OcrNodeCredentialApiContractTest，
     * 健康治理和调用记录契约已移动到 OcrNodeGovernanceApiContractTest。
     * 这里的断言刻意保持在 HTTP 响应层，
     * 避免契约测试穿透到应用服务内部实现。
     */

    /**
     * 支持模型接口应只返回系统已注册的 OCR 类型。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void listSupportedModelsReturnsPaddleOcr() throws Exception {
        mockMvc.perform(get("/api/v1/ocr-models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].model_key").value("paddle_ocr"))
                .andExpect(jsonPath("$.items[0].ocr_path").value("/ocr"))
                .andExpect(jsonPath("$.items[0].health_path").value("/ocr"));
    }

    /**
     * 创建节点后应可按模型查询并展示节点配置。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void createPaddleOcrNodeAndListNodes() throws Exception {
        // 先通过公开 API 创建离线节点，再从列表接口校验回显字段。
        createNode("paddle-api-1", "10.100.30.215", 8080);

        JsonNode node = findNodeByName(listNodeItems(), "paddle-api-1");

        assertThat(node.get("model_key").asText()).isEqualTo("paddle_ocr");
        assertThat(node.get("deployment_type").asText()).isEqualTo("OFFLINE");
        assertThat(node.get("name").asText()).isEqualTo("paddle-api-1");
        assertThat(node.get("host").asText()).isEqualTo("10.100.30.215");
        assertThat(node.get("port").asInt()).isEqualTo(8080);
        assertThat(node.get("enabled").asBoolean()).isTrue();
    }

    /**
     * 未注册 OCR 类型不能创建节点。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void createRejectsUnknownModelKey() throws Exception {
        mockMvc.perform(post("/api/v1/ocr-models/{modelKey}/nodes", "unknown_ocr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nodeJson("bad-node", "10.100.30.216", 8080)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("unsupported ocr model key"));
    }

    /**
     * 同模型下重复 host 和 port 会被拒绝。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void createRejectsDuplicateModelHostPort() throws Exception {
        // 同模型同地址先创建成功，第二次创建应由唯一性规则拒绝。
        createNode("paddle-api-dup-1", "10.100.30.217", 8080);

        mockMvc.perform(post("/api/v1/ocr-models/{modelKey}/nodes", "paddle_ocr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nodeJson("paddle-api-dup-2", "10.100.30.217", 8080)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("duplicate ocr node host and port"));
    }

    /**
     * 节点启停接口应更新节点可用状态。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void updateNodeEnabledState() throws Exception {
        String nodeId = createNode("paddle-api-toggle", "10.100.30.218", 8080);

        mockMvc.perform(patch("/api/v1/ocr-nodes/{nodeId}/enabled", nodeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.status").value("DISABLED"));
    }

    /**
     * 手动测试节点接口应返回健康检查结果。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void testNodeReturnsHealthResult() throws Exception {
        String nodeId = createNode("paddle-api-test", "10.100.30.219", 8080);

        mockMvc.perform(post("/api/v1/ocr-nodes/{nodeId}/test", nodeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.healthy").value(false))
                .andExpect(jsonPath("$.message").isString());
    }
}
