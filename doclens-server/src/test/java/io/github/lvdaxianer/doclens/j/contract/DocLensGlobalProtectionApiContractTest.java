package io.github.lvdaxianer.doclens.j.contract;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * 调用方全局保护契约测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DocLensGlobalProtectionApiContractTest extends DocLensOcrApiContractSupport {

    private static final String GLOBAL_PROTECTION_HEADER = "X-DocLens-Global-Protection";

    /**
     * 覆盖为零并发全局保护，便于稳定触发 503。
     *
     * @param registry 动态属性注册表
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @DynamicPropertySource
    static void globalProtectionProperties(DynamicPropertyRegistry registry) {
        registry.add("doclens.traffic.enabled", () -> "true");
        registry.add("doclens.traffic.anonymous-enabled", () -> "false");
        registry.add("doclens.traffic.global-protection.enabled", () -> "true");
        registry.add("doclens.traffic.global-protection.max-in-flight", () -> "0");
    }

    /**
     * 全局保护超限时应返回 503。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void returns503WhenGlobalProtectionIsExhausted() throws Exception {
        mockMvc.perform(authenticatedGet("/api/v1/dashboard/summary"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("global protection limit exceeded"))
                .andExpect(header().string(GLOBAL_PROTECTION_HEADER, "enabled"));
    }
}
