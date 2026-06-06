package com.doclens.system.interfaces;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * System health API controller compatible with the Python service contract.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    /**
     * Returns lightweight service health.
     *
     * @return health payload
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
