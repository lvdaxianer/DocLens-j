package io.github.lvdaxianer.doclens.j.contract;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * 调用方接口组流量治理契约测试。
 * <p>
 * 覆盖 HTTP 拦截链上的 caller 分区限流、接口组限流与响应头，
 * 确保单元层令牌桶语义能在真实 MVC 请求中保持一致。
 * <p>
 * 本测试故意使用 0 QPS 配置，让请求结果只由初始 burst 令牌决定。
 * 这样即使测试机执行速度变化，也不会因为时间推进而补充令牌。
 * <p>
 * 分区隔离场景使用专用 caller key，不复用默认契约测试 key，
 * 避免同一个 Spring 上下文内的令牌桶状态影响其它断言。
 *
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DocLensCallerTrafficApiContractTest extends DocLensOcrApiContractSupport {

    /** 限流响应中的接口组响应头。 */
    private static final String TRAFFIC_GROUP_HEADER = "X-DocLens-Traffic-Group";
    /** 限流响应中的建议重试等待响应头。 */
    private static final String RETRY_AFTER_HEADER = "Retry-After";
    /** 限流响应中的当前接口组突发额度响应头。 */
    private static final String RATE_LIMIT_HEADER = "X-DocLens-RateLimit-Limit";
    /** 限流响应中的剩余可用令牌响应头。 */
    private static final String RATE_LIMIT_REMAINING_HEADER = "X-DocLens-RateLimit-Remaining";
    /** 分区限流测试使用的第一个 caller key，避免消耗默认测试 key 的桶。 */
    private static final String RATE_LIMIT_PARTITION_A = "traffic-partition-a";
    /** 分区限流测试使用的第二个 caller key，用于证明跨 key 不共享桶。 */
    private static final String RATE_LIMIT_PARTITION_B = "traffic-partition-b";
    /** 测试限流配置使用 0 QPS，避免执行耗时补充令牌。 */
    private static final String TEST_QPS = "0";
    /** 治理配置写入请求体，用于稳定触发 config-mutation 限流。 */
    private static final String GOVERNANCE_CONFIG_BODY = """
            {
              "failure_threshold": 5,
              "probe_interval_seconds": 30,
              "circuit_open_seconds": 120,
              "recovery_success_threshold": 2,
              "manual_recovery_attempts": 4
            }
            """;

    /**
     * 为流量契约测试覆盖更严格的接口组限额。
     * <p>
     * 所有接口组使用 0 QPS，使测试只依赖初始 burst 令牌，
     * 避免因执行时间差异导致令牌自动补充。
     *
     * @param registry 动态属性注册表
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @DynamicPropertySource
    static void trafficProperties(DynamicPropertyRegistry registry) {
        // 启用 caller 流量治理，并关闭匿名分区，保持与生产调用路径一致。
        registry.add("doclens.traffic.enabled", () -> "true");
        registry.add("doclens.traffic.anonymous-enabled", () -> "false");
        registerLimit(registry, "dashboard-read", "2");
        registerLimit(registry, "detail-read", "1");
        registerLimit(registry, "upload-write", "1");
        registerLimit(registry, "ocr-mutation", "2");
        registerLimit(registry, "config-mutation", "1");
        registerLimit(registry, "admin-health", "2");
    }

    /**
     * dashboard-read 与 detail-read 应拥有独立的 429 配额。
     * <p>
     * 该场景验证同一 caller key 下，不同接口组不会共享令牌桶。
     * 若实现把 bucket 只按 caller key 建模，则 detail-read 会被提前限流。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void dashboardReadAndDetailReadReturn429Independently() throws Exception {
        consumeDashboardReadBurst();
        assertDashboardReadLimited();
        assertDetailReadLimitedIndependently();
    }

    /**
     * 同一接口组内不同 caller 分区键应拥有独立的 429 配额。
     * <p>
     * 该场景直接对应 OpenSpec 的 stop-loss 要求：一个分区耗尽
     * dashboard-read 桶后，另一个分区仍能使用自己的同组桶。
     * 若实现只按 traffic group 分桶，最后一次请求会返回 429。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-22
     */
    @Test
    void dashboardReadLimitIsPartitionedByCallerKey() throws Exception {
        // partition A 先消耗 dashboard-read 接口组的两枚初始令牌。
        mockMvc.perform(partitionAuthenticatedGet(RATE_LIMIT_PARTITION_A, "/api/v1/dashboard/summary"))
                .andExpect(status().isOk());
        mockMvc.perform(partitionAuthenticatedGet(RATE_LIMIT_PARTITION_A, "/api/v1/dashboard/summary"))
                .andExpect(status().isOk());
        // 第三次请求是同一分区 burst 已耗尽的可观察信号。
        // partition A 第三次请求应在自己的 caller bucket 内收到 429。
        mockMvc.perform(partitionAuthenticatedGet(RATE_LIMIT_PARTITION_A, "/api/v1/dashboard/summary"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(TRAFFIC_GROUP_HEADER, "dashboard-read"));

        // partition B 使用同一接口组时仍有独立 bucket，可继续访问。
        // 这个断言会暴露“只按接口组分桶”的错误实现。
        mockMvc.perform(partitionAuthenticatedGet(RATE_LIMIT_PARTITION_B, "/api/v1/dashboard/summary"))
                .andExpect(status().isOk());
    }

    /**
     * upload-write 超限后不应影响 ocr-mutation。
     * <p>
     * 该场景覆盖写入与变更接口组之间的隔离，避免上传流量耗尽后
     * 误伤重试等独立 mutation 操作。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void uploadWriteAndOcrMutationReturn429Independently() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "traffic.md", "text/markdown", "# traffic".getBytes());

        consumeUploadWriteBurst(file);
        assertUploadWriteLimited(file);
        assertOcrMutationLimitedIndependently();
    }

    /**
     * config-mutation 与 admin-health 应拥有独立的 429 配额。
     * <p>
     * 该场景覆盖配置写入与健康检查接口组之间的隔离，确保管理写入
     * 超限时不会阻断只读健康探测。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void configMutationAndAdminHealthReturn429Independently() throws Exception {
        consumeConfigMutationBurst();
        assertConfigMutationLimited();
        assertAdminHealthLimitedIndependently();
    }

    /**
     * 注册一个接口组的 0 QPS 测试限额。
     *
     * @param registry 动态属性注册表
     * @param trafficGroup 接口组名称
     * @param burst 初始突发令牌数
     * @author lvdaxianerplus
     * @date 2026-06-22
     */
    private static void registerLimit(DynamicPropertyRegistry registry, String trafficGroup, String burst) {
        registry.add("doclens.traffic.default-limits.[" + trafficGroup + "].qps", () -> TEST_QPS);
        registry.add("doclens.traffic.default-limits.[" + trafficGroup + "].burst", () -> burst);
    }

    /**
     * 消耗 dashboard-read 的初始突发令牌。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-22
     */
    private void consumeDashboardReadBurst() throws Exception {
        mockMvc.perform(authenticatedGet("/api/v1/dashboard/summary"))
                .andExpect(status().isOk());
        mockMvc.perform(authenticatedGet("/api/v1/dashboard/summary"))
                .andExpect(status().isOk());
    }

    /**
     * 断言 dashboard-read 第三次请求触发 429。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-22
     */
    private void assertDashboardReadLimited() throws Exception {
        mockMvc.perform(authenticatedGet("/api/v1/dashboard/summary"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(TRAFFIC_GROUP_HEADER, "dashboard-read"))
                .andExpect(header().string(RATE_LIMIT_HEADER, "2"))
                .andExpect(header().string(RATE_LIMIT_REMAINING_HEADER, "0"))
                .andExpect(header().exists(RETRY_AFTER_HEADER));
    }

    /**
     * 断言 detail-read 拥有独立于 dashboard-read 的配额。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-22
     */
    private void assertDetailReadLimitedIndependently() throws Exception {
        mockMvc.perform(authenticatedGet("/api/v1/batches/missing-batch"))
                .andExpect(status().isNotFound());
        mockMvc.perform(authenticatedGet("/api/v1/batches/missing-batch"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(TRAFFIC_GROUP_HEADER, "detail-read"))
                .andExpect(header().string(RATE_LIMIT_HEADER, "1"))
                .andExpect(header().string(RATE_LIMIT_REMAINING_HEADER, "0"))
                .andExpect(header().exists(RETRY_AFTER_HEADER));
    }

    /**
     * 消耗 upload-write 的初始突发令牌。
     *
     * @param file 上传文件
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-22
     */
    private void consumeUploadWriteBurst(MockMultipartFile file) throws Exception {
        mockMvc.perform(authenticatedMultipart("/api/v1/batches")
                        .file(file)
                        .param("metadata", "{\"bizId\":\"traffic-upload-1\"}"))
                .andExpect(status().isAccepted());
    }

    /**
     * 断言 upload-write 第二次请求触发 429。
     *
     * @param file 上传文件
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-22
     */
    private void assertUploadWriteLimited(MockMultipartFile file) throws Exception {
        mockMvc.perform(authenticatedMultipart("/api/v1/batches")
                        .file(file)
                        .param("metadata", "{\"bizId\":\"traffic-upload-2\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(TRAFFIC_GROUP_HEADER, "upload-write"))
                .andExpect(header().string(RATE_LIMIT_HEADER, "1"))
                .andExpect(header().string(RATE_LIMIT_REMAINING_HEADER, "0"))
                .andExpect(header().exists(RETRY_AFTER_HEADER));
    }

    /**
     * 断言 ocr-mutation 拥有独立于 upload-write 的配额。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-22
     */
    private void assertOcrMutationLimitedIndependently() throws Exception {
        mockMvc.perform(authenticatedPost("/api/v1/documents/{documentId}/retry", "missing-document"))
                .andExpect(status().isNotFound());
        mockMvc.perform(authenticatedPost("/api/v1/documents/{documentId}/retry", "missing-document"))
                .andExpect(status().isNotFound());
        mockMvc.perform(authenticatedPost("/api/v1/documents/{documentId}/retry", "missing-document"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(TRAFFIC_GROUP_HEADER, "ocr-mutation"))
                .andExpect(header().string(RATE_LIMIT_HEADER, "2"))
                .andExpect(header().string(RATE_LIMIT_REMAINING_HEADER, "0"))
                .andExpect(header().exists(RETRY_AFTER_HEADER));
    }

    /**
     * 消耗 config-mutation 的初始突发令牌。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-22
     */
    private void consumeConfigMutationBurst() throws Exception {
        mockMvc.perform(authenticatedPut("/api/v1/ocr-governance-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GOVERNANCE_CONFIG_BODY))
                .andExpect(status().isOk());
    }

    /**
     * 断言 config-mutation 第二次请求触发 429。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-22
     */
    private void assertConfigMutationLimited() throws Exception {
        mockMvc.perform(authenticatedPut("/api/v1/ocr-governance-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GOVERNANCE_CONFIG_BODY))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(TRAFFIC_GROUP_HEADER, "config-mutation"))
                .andExpect(header().string(RATE_LIMIT_HEADER, "1"))
                .andExpect(header().string(RATE_LIMIT_REMAINING_HEADER, "0"))
                .andExpect(header().exists(RETRY_AFTER_HEADER));
    }

    /**
     * 断言 admin-health 拥有独立于 config-mutation 的配额。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-22
     */
    private void assertAdminHealthLimitedIndependently() throws Exception {
        mockMvc.perform(authenticatedGet("/api/v1/health"))
                .andExpect(status().isOk());
        mockMvc.perform(authenticatedGet("/api/v1/health"))
                .andExpect(status().isOk());
        mockMvc.perform(authenticatedGet("/api/v1/health"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(TRAFFIC_GROUP_HEADER, "admin-health"))
                .andExpect(header().string(RATE_LIMIT_HEADER, "2"))
                .andExpect(header().string(RATE_LIMIT_REMAINING_HEADER, "0"))
                .andExpect(header().exists(RETRY_AFTER_HEADER));
    }

    /**
     * 创建指定 caller 分区键的 GET 请求。
     * <p>
     * 仅用于当前测试类的分区限流断言，避免把额外 helper 暴露到
     * 通用契约测试支持接口里。
     *
     * @param partitionKey caller 分区键
     * @param uriTemplate URI 模板
     * @return GET 请求构建器
     * @author lvdaxianerplus
     * @date 2026-06-22
     */
    private MockHttpServletRequestBuilder partitionAuthenticatedGet(
            String partitionKey,
            String uriTemplate
    ) {
        return MockMvcRequestBuilders.get(uriTemplate).header(CALLER_PARTITION_HEADER, partitionKey);
    }
}
