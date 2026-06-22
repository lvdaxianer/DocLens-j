package io.github.lvdaxianer.doclens.j.integration.interfaces;

import io.github.lvdaxianer.doclens.j.api.DocLensEngine;
import io.github.lvdaxianer.doclens.j.query.interfaces.OcrQueryHttpFacade;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Open WebUI OCR 文档查询集成适配器控制器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
@RestController
@RequestMapping("/api/v1/integrations/open-webui/ocr")
public class OpenWebuiOcrDocumentIntegrationController {

    private final DocLensEngine docLensEngine;
    private final OcrQueryHttpFacade ocrQueryHttpFacade;
    private final OpenWebuiAuthGuard authGuard;
    private final OpenWebuiResponseMapper responseMapper;

    /**
     * 创建 Open WebUI OCR 文档查询集成适配器控制器。
     *
     * @param docLensEngine DocLens 引擎
     * @param ocrQueryHttpFacade OCR 查询 HTTP 门面
     * @param authGuard Open WebUI 鉴权守卫
     * @param responseMapper 响应映射器
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public OpenWebuiOcrDocumentIntegrationController(
            DocLensEngine docLensEngine,
            OcrQueryHttpFacade ocrQueryHttpFacade,
            OpenWebuiAuthGuard authGuard,
            OpenWebuiResponseMapper responseMapper
    ) {
        this.docLensEngine = docLensEngine;
        this.ocrQueryHttpFacade = ocrQueryHttpFacade;
        this.authGuard = authGuard;
        this.responseMapper = responseMapper;
    }

    /**
     * 查询 Open WebUI 文档状态。
     *
     * @param documentId 文档 ID
     * @param request HTTP 请求
     * @return Open WebUI 文档状态响应
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @GetMapping("/documents/{documentId}")
    public Map<String, Object> getDocument(@PathVariable String documentId, HttpServletRequest request) {
        authGuard.requireIdentity(request);
        return responseMapper.document(ocrQueryHttpFacade.getDocument(request, documentId));
    }

    /**
     * 查询 Open WebUI 文档结果。
     *
     * @param documentId 文档 ID
     * @param request HTTP 请求
     * @return Open WebUI 文档结果响应
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @GetMapping("/documents/{documentId}/result")
    public Map<String, Object> getDocumentResult(@PathVariable String documentId, HttpServletRequest request) {
        authGuard.requireIdentity(request);
        return responseMapper.result(
                ocrQueryHttpFacade.getDocumentResult(request, documentId),
                ocrQueryHttpFacade.getDocument(request, documentId)
        );
    }

    /**
     * 重试 Open WebUI 文档。
     *
     * @param documentId 文档 ID
     * @param request HTTP 请求
     * @return Open WebUI 重试响应
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @PostMapping("/documents/{documentId}/retry")
    public Map<String, Object> retryDocument(@PathVariable String documentId, HttpServletRequest request) {
        authGuard.requireIdentity(request);
        ocrQueryHttpFacade.assertDocumentVisible(request, documentId);
        return responseMapper.retry(docLensEngine.retryDocument(documentId));
    }
}
