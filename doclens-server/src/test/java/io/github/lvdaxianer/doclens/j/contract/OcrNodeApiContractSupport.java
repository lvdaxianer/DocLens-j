package io.github.lvdaxianer.doclens.j.contract;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCall;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallCreateRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallStatus;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutingMode;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.OcrRuntimeNodePool;
import io.github.lvdaxianer.doclens.j.testsupport.PostgreSqlTestContainerSupport;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Iterator;
import java.util.Optional;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * OCR 节点 API 契约测试共享支持。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class OcrNodeApiContractSupport implements CallerCredentialContractSupport {

    /** 契约测试基准时间。 */
    protected static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-09T10:00:00+08:00");
    /** PostgreSQL 契约测试数据库。 */
    private static final PostgreSQLContainer<?> POSTGRESQL =
            PostgreSqlTestContainerSupport.createStartedContainer("ocr_node_api");
    /** 临时目录用于隔离数据库和对象存储。 */
    @TempDir
    static java.nio.file.Path tempDir;

    /** MockMvc 测试客户端。 */
    @Autowired
    protected MockMvc mockMvc;
    /** JSON 解析器。 */
    @Autowired
    protected ObjectMapper objectMapper;
    /** 节点调用记录仓储。 */
    @Autowired
    protected OcrNodeCallRepository ocrNodeCallRepository;
    /** 节点仓储。 */
    @Autowired
    protected OcrNodeRepository nodeRepository;
    /** 运行时节点池。 */
    @Autowired
    protected OcrRuntimeNodePool nodePool;

    /**
     * 配置隔离的测试存储与数据库。
     *
     * @param registry 动态属性注册表
     * @author lvdaxianerplus
     * @date 2026-06-09
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
    protected String createNode(String name, String host, int port) throws Exception {
        String response = mockMvc.perform(authenticatedPost("/api/v1/ocr-models/{modelKey}/nodes", "paddle_ocr")
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
     * @param credentialEnvVar 在线凭证环境变量名
     * @return 节点 ID
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    protected String createOnlineNode(String name, String providerModel, String credentialEnvVar) throws Exception {
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
     * 创建节点请求 JSON。
     *
     * @param name 节点名称
     * @param host 节点主机
     * @param port 节点端口
     * @return 请求 JSON
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    protected String nodeJson(String name, String host, int port) {
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
     * @param credentialEnvVar 在线凭证环境变量名
     * @return 请求 JSON
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    protected String onlineNodeJson(String name, String providerModel, String credentialEnvVar) {
        return """
                {
                  "deployment_type": "ONLINE",
                  "name": "%s",
                  "channel_key": "aliyun_bailian_dashscope",
                  "provider_model": "%s",
                  "credential_env_var": "%s",
                  "enabled": true,
                  "participate_global": true,
                  "weight": 100,
                  "max_concurrency": 4
                }
                """.formatted(name, providerModel, credentialEnvVar);
    }

    /**
     * 保存节点调用记录，供详情接口契约测试复用。
     *
     * @param seed 调用记录参数
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    protected void saveCall(OcrCallSeed seed) {
        OffsetDateTime startedAt = BASE_TIME.plusSeconds(seed.pageNo());
        OcrNodeCall call = OcrNodeCall.create(callRequest(seed, startedAt));
        ocrNodeCallRepository.save(call);
    }

    /**
     * 查询节点列表响应中的 items 数组。
     *
     * @return 节点列表数组
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    protected JsonNode listNodeItems() throws Exception {
        String response = mockMvc.perform(authenticatedGet("/api/v1/ocr-models/{modelKey}/nodes", "paddle_ocr"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(response).get("items");
    }

    /**
     * 按节点名称在节点列表响应中查找目标节点。
     *
     * @param items 节点数组
     * @param nodeName 节点名称
     * @return 目标节点 JSON
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    protected JsonNode findNodeByName(JsonNode items, String nodeName) {
        Iterator<JsonNode> iterator = items.elements();
        while (iterator.hasNext()) {
            JsonNode node = iterator.next();
            if (nodeName.equals(node.get("name").asText())) {
                return node;
            } else {
                // 继续查找后续节点，直到命中目标名称。
            }
        }
        throw new IllegalStateException("ocr node response item not found");
    }

    /**
     * 创建节点调用记录请求。
     *
     * @param seed 调用记录参数
     * @param startedAt 开始时间
     * @return 节点调用记录请求
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private OcrNodeCallCreateRequest callRequest(OcrCallSeed seed, OffsetDateTime startedAt) {
        return new OcrNodeCallCreateRequest("call_" + seed.documentId(), "batch_contract", seed.documentId(),
                seed.pageNo(), "paddle_ocr", seed.nodeId(), OcrRoutingMode.GLOBAL_LOAD_BALANCE, seed.status(),
                seed.retryCount(), seed.elapsedMs(), Optional.empty(), seed.errorMessage(), startedAt,
                Optional.of(startedAt.plus(Duration.ofMillis(seed.elapsedMs()))));
    }

    /**
     * 节点调用记录参数。
     *
     * @param nodeId 节点 ID
     * @param documentId 文档 ID
     * @param pageNo 页码
     * @param status 调用状态
     * @param retryCount 重试次数
     * @param elapsedMs 耗时
     * @param errorMessage 错误消息
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    protected record OcrCallSeed(
            String nodeId,
            String documentId,
            int pageNo,
            OcrNodeCallStatus status,
            int retryCount,
            long elapsedMs,
            Optional<String> errorMessage
    ) {
    }
}
