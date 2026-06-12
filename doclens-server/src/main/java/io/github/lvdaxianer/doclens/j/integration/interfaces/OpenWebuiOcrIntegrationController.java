package io.github.lvdaxianer.doclens.j.integration.interfaces;

import io.github.lvdaxianer.doclens.j.api.CreateBatchRequest;
import io.github.lvdaxianer.doclens.j.api.DocLensEngine;
import io.github.lvdaxianer.doclens.j.api.DocumentInput;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
    private static final String CALLBACK_URL_PARAM = "callback_url";
    private static final String ADAPTER_OVERRIDE_PARAM = "adapter_override";
    private static final String PDF_MODE_PARAM = "pdf_mode";
    private static final String DEFAULT_FILE_NAME = "uploaded.bin";

    private final DocLensEngine docLensEngine;
    private final OpenWebuiAuthGuard authGuard;
    private final OpenWebuiMetadataMapper metadataMapper;
    private final OpenWebuiResponseMapper responseMapper;

    /**
     * 创建 Open WebUI OCR 集成适配器控制器。
     *
     * @param docLensEngine DocLens 引擎
     * @param authGuard Open WebUI 鉴权守卫
     * @param metadataMapper metadata 映射器
     * @param responseMapper 响应映射器
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public OpenWebuiOcrIntegrationController(
            DocLensEngine docLensEngine,
            OpenWebuiAuthGuard authGuard,
            OpenWebuiMetadataMapper metadataMapper,
            OpenWebuiResponseMapper responseMapper
    ) {
        this.docLensEngine = docLensEngine;
        this.authGuard = authGuard;
        this.metadataMapper = metadataMapper;
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
        return responseMapper.createdBatch(docLensEngine.createBatch(toCreateBatchRequest(files, request, metadata)));
    }

    /**
     * 处理 Open WebUI 适配器契约异常。
     *
     * @param ex 适配器契约异常
     * @return 契约错误响应
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @ExceptionHandler(OpenWebuiIntegrationException.class)
    public ResponseEntity<Map<String, Object>> handleOpenWebuiException(OpenWebuiIntegrationException ex) {
        return ResponseEntity.status(ex.status()).body(Map.of(
                "code", ex.code(),
                "message", ex.getMessage(),
                "details", ex.details()
        ));
    }

    /**
     * 组装 DocLens 批次创建请求。
     *
     * @param files 上传文件集合
     * @param request HTTP 请求
     * @param metadata Open WebUI metadata
     * @return DocLens 批次创建请求
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private CreateBatchRequest toCreateBatchRequest(
            List<MultipartFile> files,
            HttpServletRequest request,
            OpenWebuiMetadata metadata
    ) {
        return new CreateBatchRequest(toDocumentInputs(files), metadata.values(), request.getParameter(CALLBACK_URL_PARAM),
                request.getParameter(IDEMPOTENCY_KEY_PARAM), request.getParameter(ADAPTER_OVERRIDE_PARAM),
                request.getParameter(PDF_MODE_PARAM));
    }

    /**
     * 将 multipart 文件转换为 DocLens 文档输入。
     *
     * @param files 上传文件集合
     * @return DocLens 文档输入集合
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private List<DocumentInput> toDocumentInputs(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw OpenWebuiIntegrationException.badRequest("files is required");
        } else {
            return files.stream().map(this::toDocumentInput).toList();
        }
    }

    /**
     * 将单个 multipart 文件转换为 DocLens 文档输入。
     *
     * @param file 上传文件
     * @return DocLens 文档输入
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private DocumentInput toDocumentInput(MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename() == null ? DEFAULT_FILE_NAME : file.getOriginalFilename();
            return new DocumentInput(fileName, file.getBytes());
        } catch (IOException ex) {
            throw OpenWebuiIntegrationException.badRequest("failed to read uploaded file");
        }
    }
}
