package io.github.lvdaxianer.doclens.j.integration.interfaces;

import io.github.lvdaxianer.doclens.j.query.interfaces.OcrQueryHttpFacade;
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

    private final OcrQueryHttpFacade ocrQueryHttpFacade;
    private final OpenWebuiAuthGuard authGuard;
    private final OpenWebuiResponseMapper responseMapper;

    /**
     * 创建 Open WebUI OCR 查询集成适配器控制器。
     *
     * @param ocrQueryHttpFacade OCR 查询 HTTP 门面
     * @param authGuard Open WebUI 鉴权守卫
     * @param responseMapper 响应映射器
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public OpenWebuiOcrQueryIntegrationController(
            OcrQueryHttpFacade ocrQueryHttpFacade,
            OpenWebuiAuthGuard authGuard,
            OpenWebuiResponseMapper responseMapper
    ) {
        this.ocrQueryHttpFacade = ocrQueryHttpFacade;
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
        return responseMapper.batch(ocrQueryHttpFacade.getBatch(request, batchId));
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
        return responseMapper.events(ocrQueryHttpFacade.getEvents(request, batchId));
    }
}
