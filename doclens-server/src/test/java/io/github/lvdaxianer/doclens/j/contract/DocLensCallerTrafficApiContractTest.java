package io.github.lvdaxianer.doclens.j.contract;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * 调用方接口组流量治理契约测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DocLensCallerTrafficApiContractTest extends DocLensOcrApiContractSupport {

    private static final String TRAFFIC_GROUP_HEADER = "X-DocLens-Traffic-Group";
    private static final String RETRY_AFTER_HEADER = "Retry-After";
    private static final String RATE_LIMIT_HEADER = "X-DocLens-RateLimit-Limit";
    private static final String RATE_LIMIT_REMAINING_HEADER = "X-DocLens-RateLimit-Remaining";

    /**
     * 为流量契约测试覆盖更严格的接口组限额。
     *
     * @param registry 动态属性注册表
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @DynamicPropertySource
    static void trafficProperties(DynamicPropertyRegistry registry) {
        registry.add("doclens.traffic.enabled", () -> "true");
        registry.add("doclens.traffic.anonymous-enabled", () -> "false");
        registry.add("doclens.traffic.default-limits.[dashboard-read].qps", () -> "0");
        registry.add("doclens.traffic.default-limits.[dashboard-read].burst", () -> "2");
        registry.add("doclens.traffic.default-limits.[detail-read].qps", () -> "0");
        registry.add("doclens.traffic.default-limits.[detail-read].burst", () -> "1");
        registry.add("doclens.traffic.default-limits.[upload-write].qps", () -> "0");
        registry.add("doclens.traffic.default-limits.[upload-write].burst", () -> "1");
        registry.add("doclens.traffic.default-limits.[ocr-mutation].qps", () -> "0");
        registry.add("doclens.traffic.default-limits.[ocr-mutation].burst", () -> "2");
        registry.add("doclens.traffic.default-limits.[config-mutation].qps", () -> "0");
        registry.add("doclens.traffic.default-limits.[config-mutation].burst", () -> "1");
        registry.add("doclens.traffic.default-limits.[admin-health].qps", () -> "0");
        registry.add("doclens.traffic.default-limits.[admin-health].burst", () -> "2");
    }

    /**
     * dashboard-read 与 detail-read 应拥有独立的 429 配额。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void dashboardReadAndDetailReadReturn429Independently() throws Exception {
        mockMvc.perform(authenticatedGet("/api/v1/dashboard/summary"))
                .andExpect(status().isOk());
        mockMvc.perform(authenticatedGet("/api/v1/dashboard/summary"))
                .andExpect(status().isOk());
        mockMvc.perform(authenticatedGet("/api/v1/dashboard/summary"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(TRAFFIC_GROUP_HEADER, "dashboard-read"))
                .andExpect(header().string(RATE_LIMIT_HEADER, "2"))
                .andExpect(header().string(RATE_LIMIT_REMAINING_HEADER, "0"))
                .andExpect(header().exists(RETRY_AFTER_HEADER));

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
     * upload-write 超限后不应影响 ocr-mutation。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void uploadWriteAndOcrMutationReturn429Independently() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "traffic.md", "text/markdown", "# traffic".getBytes());

        mockMvc.perform(authenticatedMultipart("/api/v1/batches")
                        .file(file)
                        .param("metadata", "{\"bizId\":\"traffic-upload-1\"}"))
                .andExpect(status().isAccepted());
        mockMvc.perform(authenticatedMultipart("/api/v1/batches")
                        .file(file)
                        .param("metadata", "{\"bizId\":\"traffic-upload-2\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(TRAFFIC_GROUP_HEADER, "upload-write"))
                .andExpect(header().string(RATE_LIMIT_HEADER, "1"))
                .andExpect(header().string(RATE_LIMIT_REMAINING_HEADER, "0"))
                .andExpect(header().exists(RETRY_AFTER_HEADER));

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
     * config-mutation 与 admin-health 应拥有独立的 429 配额。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void configMutationAndAdminHealthReturn429Independently() throws Exception {
        mockMvc.perform(authenticatedPut("/api/v1/ocr-governance-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "failure_threshold": 5,
                                  "probe_interval_seconds": 30,
                                  "circuit_open_seconds": 120,
                                  "recovery_success_threshold": 2,
                                  "manual_recovery_attempts": 4
                                }
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(authenticatedPut("/api/v1/ocr-governance-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "failure_threshold": 5,
                                  "probe_interval_seconds": 30,
                                  "circuit_open_seconds": 120,
                                  "recovery_success_threshold": 2,
                                  "manual_recovery_attempts": 4
                                }
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(TRAFFIC_GROUP_HEADER, "config-mutation"))
                .andExpect(header().string(RATE_LIMIT_HEADER, "1"))
                .andExpect(header().string(RATE_LIMIT_REMAINING_HEADER, "0"))
                .andExpect(header().exists(RETRY_AFTER_HEADER));

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
}
