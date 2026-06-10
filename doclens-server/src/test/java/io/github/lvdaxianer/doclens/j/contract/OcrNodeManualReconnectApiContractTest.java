package io.github.lvdaxianer.doclens.j.contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.lvdaxianer.doclens.j.adapter.infrastructure.OcrHealthClient;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * OCR 节点手动重连 API 契约测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
@SpringBootTest
@AutoConfigureMockMvc
class OcrNodeManualReconnectApiContractTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-10T10:00:00+08:00");

    @TempDir
    static java.nio.file.Path tempDir;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Autowired
    private OcrNodeRepository nodeRepository;

    @MockBean
    private OcrHealthClient healthClient;

    /**
     * 配置隔离的测试数据库与文件存储。
     *
     * @param registry 动态属性注册表
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:file:" + tempDir.resolve("ocr-node-reconnect")
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        registry.add("doclens.storage-root", () -> tempDir.resolve("storage").toString());
        registry.add("doclens.paddle-ocr.enabled", () -> "false");
    }

    /**
     * 手动重连应执行有限恢复探测，并在满足恢复阈值后返回最新状态。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void manualReconnectRunsBoundedRecoveryAttemptsAndReturnsUpdatedState() throws Exception {
        Mockito.when(healthClient.isHealthy(Mockito.any())).thenReturn(true);
        String nodeId = createOnlineNode("dashscope-reconnect", "qwen-vl-ocr-2025-11-20", "sk-secret-reconnect");
        markNodeDownWithOpenCircuit(nodeId);

        mockMvc.perform(post("/api/v1/ocr-nodes/{nodeId}/reconnect", nodeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attempts").value(3))
                .andExpect(jsonPath("$.healthy").value(true))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.circuit_open_until").value(""));
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
     * @date 2026-06-10
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
     * 将节点置为熔断中的 DOWN 状态，验证手动重连不会被熔断窗口直接拦截。
     *
     * @param nodeId 节点 ID
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private void markNodeDownWithOpenCircuit(String nodeId) {
        OcrNode current = nodeRepository.findById(nodeId).orElseThrow();
        OcrNode downNode = new OcrNode(current.id(), current.modelKey(), current.deploymentType(), current.name(),
                current.host(), current.port(), current.channelKey(), current.providerModel(), current.credentialRef(),
                current.credentialConfigured(), current.enabled(), current.participateGlobal(), current.weight(),
                current.maxConcurrency(), OcrNodeStatus.DOWN, 3L, 0L, current.avgLatencyMs(), current.p95LatencyMs(),
                Optional.of(BASE_TIME), Optional.empty(), Optional.of(BASE_TIME), Optional.of("timeout"),
                Optional.of(BASE_TIME.plusDays(1)), Optional.empty(), current.createdAt(), BASE_TIME);
        nodeRepository.update(downNode);
    }

    /**
     * 创建在线节点请求 JSON。
     *
     * @param name 节点名称
     * @param providerModel 在线模型名称
     * @param apiKey 在线 API Key
     * @return 请求 JSON
     * @author lvdaxianerplus
     * @date 2026-06-10
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
                  "weight": 50,
                  "max_concurrency": 10
                }
                """.formatted(name, providerModel, apiKey);
    }
}
