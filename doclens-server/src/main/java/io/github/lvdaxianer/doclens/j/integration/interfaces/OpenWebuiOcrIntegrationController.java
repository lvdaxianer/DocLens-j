package io.github.lvdaxianer.doclens.j.integration.interfaces;

import io.github.lvdaxianer.doclens.j.api.DocLensEngine;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Open WebUI OCR 集成适配器控制器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
@RestController
@RequestMapping("/api/v1/integrations/open-webui/ocr")
public class OpenWebuiOcrIntegrationController {

    private static final String FILES_PARAM = "files";
    private static final String METADATA_PARAM = "metadata";
    private static final String IDEMPOTENCY_KEY_PARAM = "idempotency_key";

    private final DocLensEngine docLensEngine;
    private final OpenWebuiAuthGuard authGuard;
    private final OpenWebuiMetadataMapper metadataMapper;
    private final OpenWebuiCreateBatchRequestMapper requestMapper;
    private final OpenWebuiResponseMapper responseMapper;

    /**
     * 创建 Open WebUI OCR 集成适配器控制器。
     *
     * @param docLensEngine DocLens 引擎
     * @param authGuard Open WebUI 鉴权守卫
     * @param metadataMapper metadata 映射器
     * @param requestMapper 创建批次请求映射器
     * @param responseMapper 响应映射器
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public OpenWebuiOcrIntegrationController(
            DocLensEngine docLensEngine,
            OpenWebuiAuthGuard authGuard,
            OpenWebuiMetadataMapper metadataMapper,
            OpenWebuiCreateBatchRequestMapper requestMapper,
            OpenWebuiResponseMapper responseMapper
    ) {
        this.docLensEngine = docLensEngine;
        this.authGuard = authGuard;
        this.metadataMapper = metadataMapper;
        this.requestMapper = requestMapper;
        this.responseMapper = responseMapper;
    }

    /**
     * 创建 Open WebUI OCR 批次。
     *
     * @param files 上传文件集合
     * @param request HTTP 请求
     * @return Open WebUI 创建响应
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @PostMapping("/batches")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> createBatch(
            @RequestParam(FILES_PARAM) List<MultipartFile> files,
            HttpServletRequest request
    ) {
        OpenWebuiIdentity identity = authGuard.requireIdentity(request);
        OpenWebuiMetadata metadata = metadataMapper.toMetadata(request.getParameter(METADATA_PARAM), identity,
                request.getParameter(IDEMPOTENCY_KEY_PARAM));
        return responseMapper.createdBatch(docLensEngine.createBatch(requestMapper.toRequest(files, request, metadata)));
    }
}
