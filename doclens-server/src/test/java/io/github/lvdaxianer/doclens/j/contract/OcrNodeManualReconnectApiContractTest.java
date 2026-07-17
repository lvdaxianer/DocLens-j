package io.github.lvdaxianer.doclens.j.contract;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.OcrHealthClient;
import io.github.lvdaxianer.doclens.j.testsupport.PostgreSqlTestContainerSupport;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * OCR 节点手动重连 API 契约测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
@SpringBootTest
@AutoConfigureMockMvc
class OcrNodeManualReconnectApiContractTest implements CallerCredentialContractSupport {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-10T10:00:00+08:00");
    /** PostgreSQL 契约测试数据库。 */
    private static final PostgreSQLContainer<?> POSTGRESQL =
            PostgreSqlTestContainerSupport.createStartedContainer("ocr_node_reconnect");

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
        PostgreSqlTestContainerSupport.registerDatasource(registry, POSTGRESQL);
        registry.add("doclens.storage-root", () -> tempDir.resolve("storage").toString());
        registry.add("doclens.paddle-ocr.enabled", () -> "false");
        registry.add("doclens.clients.credentials[0].client-id", () -> TEST_CLIENT_ID);
        registry.add("doclens.clients.credentials[0].source-app", () -> TEST_SOURCE_APP);
        registry.add("doclens.clients.credentials[0].tenant-key", () -> TEST_TENANT_KEY);
        registry.add("doclens.clients.credentials[0].api-key", () -> TEST_API_KEY);
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
        String nodeId = createOnlineNode("dashscope-reconnect", "qwen-vl-ocr-2025-11-20", "DASHSCOPE_API_KEY");
        markNodeDownWithOpenCircuit(nodeId);

        mockMvc.perform(authenticatedPost("/api/v1/ocr-nodes/{nodeId}/reconnect", nodeId))
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
     * @param credentialEnvVar 在线凭证环境变量名
     * @return 节点 ID
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private String createOnlineNode(String name, String providerModel, String credentialEnvVar) throws Exception {
        String response = mockMvc.perform(authenticatedPost("/api/v1/ocr-models/{modelKey}/nodes", "paddle_ocr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(onlineNodeJson(name, providerModel, credentialEnvVar)))
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
     * @param credentialEnvVar 在线凭证环境变量名
     * @return 请求 JSON
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private String onlineNodeJson(String name, String providerModel, String credentialEnvVar) {
        return """
                {
                  "deployment_type": "ONLINE",
                  "name": "%s",
                  "channel_key": "aliyun_bailian_dashscope",
                  "provider_model": "%s",
                  "credential_env_var": "%s",
                  "enabled": true,
                  "participate_global": true,
                  "weight": 50,
                  "max_concurrency": 10
                }
                """.formatted(name, providerModel, credentialEnvVar);
    }

    /**
     * 隔离 OCR 健康探测，避免契约测试调用真实第三方服务。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @TestConfiguration
    static class TestOcrHealthConfiguration {

        /**
         * 创建固定成功的 DashScope 在线 OCR 测试客户端。
         *
         * @return 在线 OCR 测试客户端
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Bean
        @Primary
        OcrHealthClient ocrHealthClient() {
            return node -> true;
        }
    }
}
