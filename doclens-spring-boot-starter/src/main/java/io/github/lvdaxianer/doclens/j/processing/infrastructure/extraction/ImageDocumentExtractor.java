package io.github.lvdaxianer.doclens.j.processing.infrastructure.extraction;

import io.github.lvdaxianer.doclens.j.adapter.domain.AdapterRegistry;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrResult;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrAdapter;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRouteExecutionResult;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingService;
import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentTextExtractionRequest;
import io.github.lvdaxianer.doclens.j.processing.application.extraction.DocumentTextExtractionResult;
import io.github.lvdaxianer.doclens.j.processing.domain.ProcessingStage;
import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
import java.util.List;
import java.util.Optional;

/**
 * 图片文档 OCR 提取器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public class ImageDocumentExtractor {

    private final AdapterRegistry adapterRegistry;
    private final Optional<OcrRoutingService> routingService;

    /**
     * 创建图片 OCR 提取器。
     *
     * @param adapterRegistry OCR 适配器注册表
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public ImageDocumentExtractor(AdapterRegistry adapterRegistry) {
        this(adapterRegistry, null);
    }

    /**
     * 创建图片 OCR 提取器。
     *
     * @param adapterRegistry OCR 适配器注册表
     * @param routingService OCR 资源路由服务
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public ImageDocumentExtractor(AdapterRegistry adapterRegistry, OcrRoutingService routingService) {
        this.adapterRegistry = adapterRegistry;
        this.routingService = Optional.ofNullable(routingService);
    }

    /**
     * 对单张图片执行 OCR。
     *
     * @param request 提取请求
     * @return 提取结果
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public DocumentTextExtractionResult extract(DocumentTextExtractionRequest request) {
        try {
            request.progressReporter().report(ProcessingStage.OCR_IMAGES, 0, 1);
            ImageOcrResult result = recognize(request, DocLensConstants.DEFAULT_PAGE_NO, request.content());
            request.progressReporter().report(ProcessingStage.OCR_IMAGES, 1, 1);
            return DocumentTextExtractionResult.fromPageResults(request.document().documentId(),
                    request.document().fileName(), List.of(result), List.of());
        } finally {
            releaseDocumentAffinity(request);
        }
    }

    /**
     * 调用指定适配器识别图片页。
     *
     * @param request 提取请求
     * @param pageNo 页码
     * @param imageContent 图片字节
     * @return 图片 OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    ImageOcrResult recognize(DocumentTextExtractionRequest request, int pageNo, byte[] imageContent) {
        ImageOcrRequest imageRequest = new ImageOcrRequest(request.document().batchId(),
                request.document().documentId(), request.document().fileName(), pageNo, imageContent,
                request.document().metadata());
        if (routingService.isPresent()) {
            OcrRouteExecutionResult routeResult = routingService.get()
                    .recognize(imageRequest, request.document().ocrRoutePolicy());
            return routeResult.result();
        } else {
            return recognizeWithAdapter(request, imageRequest);
        }
    }

    /**
     * 使用旧适配器注册表执行图片 OCR。
     *
     * @param request 提取请求
     * @param imageRequest 图片 OCR 请求
     * @return 图片 OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private ImageOcrResult recognizeWithAdapter(DocumentTextExtractionRequest request, ImageOcrRequest imageRequest) {
        OcrAdapter adapter = adapterRegistry.find(request.adapterKey())
                .orElseThrow(() -> new IllegalArgumentException("adapter not found: " + request.adapterKey()));
        return adapter.recognize(imageRequest);
    }

    /**
     * 释放文档级 OCR 模型亲和力。
     *
     * @param request 提取请求
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    void releaseDocumentAffinity(DocumentTextExtractionRequest request) {
        if (routingService.isPresent()) {
            routingService.get().releaseDocumentAffinity(request.document().documentId());
        } else {
            // 未启用路由服务时没有亲和力状态需要释放。
        }
    }
}
