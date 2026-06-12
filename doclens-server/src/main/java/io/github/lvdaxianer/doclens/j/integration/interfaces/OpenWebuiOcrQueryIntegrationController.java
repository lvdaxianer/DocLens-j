package io.github.lvdaxianer.doclens.j.integration.interfaces;

import io.github.lvdaxianer.doclens.j.api.DocLensEngine;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Open WebUI OCR 批次查询集成适配器控制器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
@RestController
@RequestMapping("/api/v1/integrations/open-webui/ocr")
public class OpenWebuiOcrQueryIntegrationController {

    private final DocLensEngine docLensEngine;
    private final OpenWebuiAuthGuard authGuard;
    private final OpenWebuiResponseMapper responseMapper;

    /**
     * 创建 Open WebUI OCR 查询集成适配器控制器。
     *
     * @param docLensEngine DocLens 引擎
     * @param authGuard Open WebUI 鉴权守卫
     * @param responseMapper 响应映射器
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public OpenWebuiOcrQueryIntegrationController(
            DocLensEngine docLensEngine,
            OpenWebuiAuthGuard authGuard,
            OpenWebuiResponseMapper responseMapper
    ) {
        this.docLensEngine = docLensEngine;
        this.authGuard = authGuard;
        this.responseMapper = responseMapper;
    }

    /**
     * 查询 Open WebUI 批次状态。
     *
     * @param batchId 批次 ID
     * @param request HTTP 请求
     * @return Open WebUI 批次状态响应
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @GetMapping("/batches/{batchId}")
    public Map<String, Object> getBatch(@PathVariable String batchId, HttpServletRequest request) {
        authGuard.requireIdentity(request);
        return responseMapper.batch(docLensEngine.getBatch(batchId));
    }

    /**
     * 查询 Open WebUI 批次事件。
     *
     * @param batchId 批次 ID
     * @param request HTTP 请求
     * @return Open WebUI 事件响应
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @GetMapping("/batches/{batchId}/events")
    public Map<String, Object> getEvents(@PathVariable String batchId, HttpServletRequest request) {
        authGuard.requireIdentity(request);
        return responseMapper.events(docLensEngine.getEvents(batchId));
    }
}
