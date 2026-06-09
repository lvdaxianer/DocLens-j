package io.github.lvdaxianer.doclens.j.contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCall;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallCreateRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallStatus;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
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

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-09T10:00:00+08:00");

    @TempDir
    static java.nio.file.Path tempDir;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OcrNodeCallRepository ocrNodeCallRepository;

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
                .andExpect(jsonPath("$.items[0].deployment_type").value("OFFLINE"))
                .andExpect(jsonPath("$.items[0].name").value("paddle-api-1"))
                .andExpect(jsonPath("$.items[0].host").value("10.100.30.215"))
                .andExpect(jsonPath("$.items[0].port").value(8080))
                .andExpect(jsonPath("$.items[0].enabled").value(true));
    }

    /**
     * 在线 DashScope 节点应保存渠道和模型配置但不回显 API Key。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void createOnlineDashScopeNodeDoesNotExposeApiKey() throws Exception {
        String response = mockMvc.perform(post("/api/v1/ocr-models/{modelKey}/nodes", "paddle_ocr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(onlineNodeJson("dashscope-main", "qwen-vl-ocr-2025-11-20", "sk-secret-create")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deployment_type").value("ONLINE"))
                .andExpect(jsonPath("$.channel_key").value("aliyun_bailian_dashscope"))
                .andExpect(jsonPath("$.provider_model").value("qwen-vl-ocr-2025-11-20"))
                .andExpect(jsonPath("$.credential_configured").value(true))
                .andExpect(jsonPath("$.api_key").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        org.assertj.core.api.Assertions.assertThat(response).doesNotContain("sk-secret-create");
    }

    /**
     * 编辑在线节点时空 API Key 应沿用旧密钥配置。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void updateOnlineNodeKeepsCredentialWhenApiKeyBlank() throws Exception {
        String nodeId = createOnlineNode("dashscope-edit", "qwen-vl-ocr-2025-11-20", "sk-secret-old");

        String response = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/v1/ocr-nodes/{nodeId}", nodeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(onlineNodeJson("dashscope-edit-renamed", "qwen-vl-ocr-2025-11-20", "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credential_configured").value(true))
                .andExpect(jsonPath("$.name").value("dashscope-edit-renamed"))
                .andExpect(jsonPath("$.api_key").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        org.assertj.core.api.Assertions.assertThat(response)
                .doesNotContain("sk-secret-old")
                .doesNotContain("api_key");
    }

    /**
     * 离线节点切换为在线节点时空 API Key 应被拒绝。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void updateOfflineNodeToOnlineRequiresApiKey() throws Exception {
        String nodeId = createNode("paddle-to-online", "10.100.30.220", 8080);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/v1/ocr-nodes/{nodeId}", nodeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(onlineNodeJson("paddle-to-online", "qwen-vl-ocr-2025-11-20", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("ocr online api key is required"));
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
     * 节点详情接口应返回最近调用记录，供 Dashboard 详情抽屉展示。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void nodeRecentCallsReturnsLatestCallRecords() throws Exception {
        String nodeId = createNode("paddle-api-calls", "10.100.30.221", 8080);
        saveCall(nodeId, "doc_call_1", 1, OcrNodeCallStatus.SUCCESS, 0, 180L, Optional.empty());
        saveCall(nodeId, "doc_call_2", 2, OcrNodeCallStatus.FAILED, 1, 520L, Optional.of("timeout"));

        mockMvc.perform(get("/api/v1/ocr-nodes/{nodeId}/calls", nodeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].document_id").value("doc_call_2"))
                .andExpect(jsonPath("$.items[0].image_index").value(2))
                .andExpect(jsonPath("$.items[0].status").value("FAILED"))
                .andExpect(jsonPath("$.items[0].retry_count").value(1))
                .andExpect(jsonPath("$.items[0].duration_ms").value(520))
                .andExpect(jsonPath("$.items[0].error_message").value("timeout"))
                .andExpect(jsonPath("$.items[1].document_id").value("doc_call_1"));
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
     * 创建在线 OCR 节点并返回节点 ID。
     *
     * @param name 节点名称
     * @param providerModel 在线模型名称
     * @param apiKey 在线 API Key
     * @return 节点 ID
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private String createOnlineNode(String name, String providerModel, String apiKey) throws Exception {
        String response = mockMvc.perform(post("/api/v1/ocr-models/{modelKey}/nodes", "paddle_ocr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(onlineNodeJson(name, providerModel, apiKey)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
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
                  "deployment_type": "OFFLINE",
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

    /**
     * 创建在线节点请求 JSON。
     *
     * @param name 节点名称
     * @param providerModel 在线模型名称
     * @param apiKey 在线 API Key
     * @return 请求 JSON
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private String onlineNodeJson(String name, String providerModel, String apiKey) {
        return """
                {
                  "deployment_type": "ONLINE",
                  "name": "%s",
                  "channel_key": "aliyun_bailian_dashscope",
                  "provider_model": "%s",
                  "api_key": "%s",
                  "enabled": true,
                  "participate_global": true,
                  "weight": 100,
                  "max_concurrency": 4
                }
                """.formatted(name, providerModel, apiKey);
    }

    /**
     * 保存节点调用记录，供详情接口契约测试复用。
     *
     * @param nodeId 节点 ID
     * @param documentId 文档 ID
     * @param pageNo 页码
     * @param status 调用状态
     * @param retryCount 重试次数
     * @param elapsedMs 耗时
     * @param errorMessage 错误消息
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private void saveCall(
            String nodeId,
            String documentId,
            int pageNo,
            OcrNodeCallStatus status,
            int retryCount,
            long elapsedMs,
            Optional<String> errorMessage
    ) {
        OffsetDateTime startedAt = BASE_TIME.plusSeconds(pageNo);
        OcrNodeCall call = OcrNodeCall.create(new OcrNodeCallCreateRequest(
                "call_" + documentId,
                "batch_contract",
                documentId,
                pageNo,
                "paddle_ocr",
                nodeId,
                OcrRoutingMode.GLOBAL_LOAD_BALANCE,
                status,
                retryCount,
                elapsedMs,
                Optional.empty(),
                errorMessage,
                startedAt,
                Optional.of(startedAt.plus(Duration.ofMillis(elapsedMs)))
        ));
        ocrNodeCallRepository.save(call);
    }
}
