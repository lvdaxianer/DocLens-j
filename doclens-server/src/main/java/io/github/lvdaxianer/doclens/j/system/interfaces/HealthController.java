package io.github.lvdaxianer.doclens.j.system.interfaces;

import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 兼容 Python 服务契约的系统健康 API 控制器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    /** 健康检查路径。 */
    private static final String HEALTH_PATH = "/health";
    /** 心跳检查路径。 */
    private static final String HEARTBEAT_PATH = "/heartbeat";
    /** 状态字段名。 */
    private static final String STATUS_KEY = "status";
    /** 服务字段名。 */
    private static final String SERVICE_KEY = "service";
    /** 时间字段名。 */
    private static final String TIMESTAMP_KEY = "timestamp";
    /** 服务名称。 */
    private static final String SERVICE_NAME = "doclens-j";
    /** 成功状态值。 */
    private static final String STATUS_OK = "ok";

    /**
     * 返回轻量级服务健康信息。
     *
     * @return 健康载荷
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @GetMapping(HEALTH_PATH)
    public Map<String, String> health() {
        return Map.of(STATUS_KEY, STATUS_OK);
    }

    /**
     * 返回给外部服务使用的心跳探测信息。
     *
     * @return 心跳载荷
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    @GetMapping(HEARTBEAT_PATH)
    public Map<String, String> heartbeat() {
        return Map.of(
                STATUS_KEY, STATUS_OK,
                SERVICE_KEY, SERVICE_NAME,
                TIMESTAMP_KEY, OffsetDateTime.now().toString()
        );
    }
}
