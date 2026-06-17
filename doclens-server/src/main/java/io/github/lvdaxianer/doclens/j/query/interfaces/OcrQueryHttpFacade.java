package io.github.lvdaxianer.doclens.j.query.interfaces;

import io.github.lvdaxianer.doclens.j.ingestion.domain.CallerIdentity;
import io.github.lvdaxianer.doclens.j.query.application.OcrQueryService;
import io.github.lvdaxianer.doclens.j.shared.web.CallerIdentityRequestResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * OCR 查询 HTTP 层门面。
 *
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
@Component
public class OcrQueryHttpFacade {

    private final OcrQueryService ocrQueryService;
    private final CallerIdentityRequestResolver callerIdentityRequestResolver;

    /**
     * 创建 OCR 查询 HTTP 层门面。
     *
     * @param ocrQueryService OCR 查询服务
     * @param callerIdentityRequestResolver caller 请求解析器
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public OcrQueryHttpFacade(
            OcrQueryService ocrQueryService,
            CallerIdentityRequestResolver callerIdentityRequestResolver
    ) {
        this.ocrQueryService = ocrQueryService;
        this.callerIdentityRequestResolver = callerIdentityRequestResolver;
    }

    /**
     * 获取当前 caller 的批次状态。
     *
     * @param request HTTP 请求
     * @param batchId 批次 ID
     * @return 批次状态
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public Map<String, Object> getBatch(HttpServletRequest request, String batchId) {
        return ocrQueryService.getBatch(caller(request), batchId);
    }

    /**
     * 根据幂等键获取当前 caller 的批次 reconciliation 视图。
     *
     * @param request HTTP 请求
     * @param idempotencyKey 幂等键
     * @return 批次 reconciliation 视图
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public Map<String, Object> getBatchByIdempotencyKey(HttpServletRequest request, String idempotencyKey) {
        return ocrQueryService.getBatchByIdempotencyKey(caller(request), idempotencyKey);
    }

    /**
     * 获取当前 caller 的文档状态。
     *
     * @param request HTTP 请求
     * @param documentId 文档 ID
     * @return 文档状态
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public Map<String, Object> getDocument(HttpServletRequest request, String documentId) {
        return ocrQueryService.getDocument(caller(request), documentId);
    }

    /**
     * 获取当前 caller 的文档 OCR 结果。
     *
     * @param request HTTP 请求
     * @param documentId 文档 ID
     * @return OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public Map<String, Object> getDocumentResult(HttpServletRequest request, String documentId) {
        return ocrQueryService.getDocumentResult(caller(request), documentId);
    }

    /**
     * 校验当前 caller 可访问文档。
     *
     * @param request HTTP 请求
     * @param documentId 文档 ID
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public void assertDocumentVisible(HttpServletRequest request, String documentId) {
        ocrQueryService.getDocument(caller(request), documentId);
    }

    /**
     * 校验当前 caller 可访问批次。
     *
     * @param request HTTP 请求
     * @param batchId 批次 ID
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public void assertBatchVisible(HttpServletRequest request, String batchId) {
        ocrQueryService.getBatch(caller(request), batchId);
    }

    /**
     * 获取当前 caller 的批次事件。
     *
     * @param request HTTP 请求
     * @param batchId 批次 ID
     * @return 事件时间线
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public Map<String, Object> getEvents(HttpServletRequest request, String batchId) {
        return ocrQueryService.getEvents(caller(request), batchId);
    }

    /**
     * 解析当前请求 caller。
     *
     * @param request HTTP 请求
     * @return caller 身份
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private CallerIdentity caller(HttpServletRequest request) {
        return callerIdentityRequestResolver.resolve(request);
    }
}
