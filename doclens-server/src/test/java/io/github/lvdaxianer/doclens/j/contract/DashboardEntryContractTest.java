package io.github.lvdaxianer.doclens.j.contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.lvdaxianer.doclens.j.dashboard.interfaces.DashboardEntryController;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Dashboard 静态入口契约测试。
 *
 * @author lvdaxianer@yeah.net
 * @date 2026-07-19
 */
class DashboardEntryContractTest {

    private static final String DASHBOARD_INDEX_PATH = "/dashboard/index.html";

    /**
     * 验证不带尾斜杠的 dashboard 根路径会转发到静态入口页。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianer@yeah.net
     * @date 2026-07-19
     */
    @Test
    void dashboardRootWithoutTrailingSlashForwardsToBundledIndex() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new DashboardEntryController()).build();
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(handler().methodName("dashboardRoot"))
                .andExpect(forwardedUrl(DASHBOARD_INDEX_PATH));
    }

    /**
     * 验证带尾斜杠的 dashboard 根路径会转发到静态入口页。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianer@yeah.net
     * @date 2026-07-19
     */
    @Test
    void dashboardRootWithTrailingSlashForwardsToBundledIndex() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new DashboardEntryController()).build();
        mockMvc.perform(get("/dashboard/"))
                .andExpect(status().isOk())
                .andExpect(handler().methodName("dashboardRoot"))
                .andExpect(forwardedUrl(DASHBOARD_INDEX_PATH));
    }
}
