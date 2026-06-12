package io.github.lvdaxianer.doclens.j.integration.interfaces;

import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Open WebUI OCR 健康检查集成适配器控制器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
@RestController
@RequestMapping("/api/v1/integrations/open-webui/ocr")
public class OpenWebuiOcrHealthIntegrationController {

    private static final String STATUS_FIELD = "status";
    private static final String SERVICE_FIELD = "service";
    private static final String TIME_FIELD = "time";
    private static final String UP_STATUS = "UP";
    private static final String SERVICE_NAME = "doclens-j";

    /**
     * 查询 Open WebUI OCR 适配器健康状态。
     *
     * @return 健康检查响应
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(STATUS_FIELD, UP_STATUS, SERVICE_FIELD, SERVICE_NAME, TIME_FIELD, OffsetDateTime.now().toString());
    }
}
