package io.github.lvdaxianer.doclens.j.system.interfaces;

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

    /**
     * 返回轻量级服务健康信息。
     *
     * @return 健康载荷
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
