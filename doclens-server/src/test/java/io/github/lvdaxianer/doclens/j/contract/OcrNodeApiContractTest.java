package io.github.lvdaxianer.doclens.j.contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * OCR 节点管理 API 契约测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
@SpringBootTest
@AutoConfigureMockMvc
class OcrNodeApiContractTest {

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
     * @date 2026-06-09
     */
    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:file:" + tempDir.resolve("ocr-node-api") + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        registry.add("doclens.storage-root", () -> tempDir.resolve("storage").toString());
        registry.add("doclens.paddle-ocr.enabled", () -> "false");
    }

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
                .andExpect(jsonPath("$.items[0].health_path").value("/health"));
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
        createNode("paddle-api-1", "10.100.30.215", 8080);

        mockMvc.perform(get("/api/v1/ocr-models/{modelKey}/nodes", "paddle_ocr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].model_key").value("paddle_ocr"))
                .andExpect(jsonPath("$.items[0].name").value("paddle-api-1"))
                .andExpect(jsonPath("$.items[0].host").value("10.100.30.215"))
                .andExpect(jsonPath("$.items[0].port").value(8080))
                .andExpect(jsonPath("$.items[0].enabled").value(true));
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

    /**
     * 创建 OCR 节点并返回节点 ID。
     *
     * @param name 节点名称
     * @param host 节点主机
     * @param port 节点端口
     * @return 节点 ID JSON 路径占位值
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private String createNode(String name, String host, int port) throws Exception {
        String response = mockMvc.perform(post("/api/v1/ocr-models/{modelKey}/nodes", "paddle_ocr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nodeJson(name, host, port)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    /**
     * 创建节点请求 JSON。
     *
     * @param name 节点名称
     * @param host 节点主机
     * @param port 节点端口
     * @return 请求 JSON
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private String nodeJson(String name, String host, int port) {
        return """
                {
                  "name": "%s",
                  "host": "%s",
                  "port": %d,
                  "enabled": true,
                  "participate_global": true,
                  "weight": 100,
                  "max_concurrency": 4
                }
                """.formatted(name, host, port);
    }
}
