package io.github.lvdaxianer.doclens.j.contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.lvdaxianer.doclens.j.system.interfaces.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 服务心跳 API 契约测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-14
 */
class ServiceHeartbeatApiContractTest {

    /** 心跳接口路径。 */
    private static final String HEARTBEAT_PATH = "/api/v1/heartbeat";
    /** 响应状态字段名。 */
    private static final String STATUS_KEY = "status";
    /** 响应服务字段名。 */
    private static final String SERVICE_KEY = "service";
    /** 响应时间字段名。 */
    private static final String TIMESTAMP_KEY = "timestamp";
    /** 状态字段路径。 */
    private static final String STATUS_JSON_PATH = "$." + STATUS_KEY;
    /** 服务字段路径。 */
    private static final String SERVICE_JSON_PATH = "$." + SERVICE_KEY;
    /** 时间字段路径。 */
    private static final String TIMESTAMP_JSON_PATH = "$." + TIMESTAMP_KEY;
    /** 成功状态值。 */
    private static final String STATUS_OK = "ok";
    /** 服务名称。 */
    private static final String SERVICE_NAME = "doclens-j";

    /**
     * 验证服务心跳接口返回固定的轻量级载荷。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    @Test
    void heartbeatEndpointReturnsStableLivenessPayload() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new HealthController()).build();
        mockMvc.perform(get(HEARTBEAT_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath(STATUS_JSON_PATH).value(STATUS_OK))
                .andExpect(jsonPath(SERVICE_JSON_PATH).value(SERVICE_NAME))
                .andExpect(jsonPath(TIMESTAMP_JSON_PATH).isString());
    }
}
