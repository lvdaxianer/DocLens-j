package io.github.lvdaxianer.doclens.j.query.interfaces;

import io.github.lvdaxianer.doclens.j.api.DocLensEngine;
import io.github.lvdaxianer.doclens.j.shared.domain.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OCR 查询 API 控制器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@RestController
@RequestMapping("/api/v1")
public class OcrQueryController {

    private final DocLensEngine docLensEngine;
    private final OcrQueryHttpFacade ocrQueryHttpFacade;

    /**
     * 创建 OCR 查询控制器。
     *
     * @param docLensEngine DocLens 引擎
     * @param ocrQueryHttpFacade OCR 查询 HTTP 门面
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public OcrQueryController(
            DocLensEngine docLensEngine,
            OcrQueryHttpFacade ocrQueryHttpFacade
    ) {
        this.docLensEngine = docLensEngine;
        this.ocrQueryHttpFacade = ocrQueryHttpFacade;
    }

    /**
     * 获取批次状态。
     *
     * @param batchId 批次 ID
     * @return 批次状态
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @GetMapping("/batches/{batchId}")
    public Map<String, Object> getBatch(@PathVariable String batchId, HttpServletRequest request) {
        return ocrQueryHttpFacade.getBatch(request, batchId);
    }

    /**
     * 根据幂等键获取批次 reconciliation 视图。
     *
     * @param idempotencyKey 幂等键
     * @return 批次 reconciliation 视图
     * @author lvdaxianerplus
     * @date 2026-06-14
     */
    @GetMapping("/batches/by-idempotency-key/{idempotencyKey}")
    public ResponseEntity<Map<String, Object>> getBatchByIdempotencyKey(
            @PathVariable String idempotencyKey,
            HttpServletRequest request
    ) {
        try {
            return ResponseEntity.ok(ocrQueryHttpFacade.getBatchByIdempotencyKey(request, idempotencyKey));
        } catch (ResourceNotFoundException ex) {
            Map<String, Object> body = new LinkedHashMap<>(3);
            body.put("code", 404);
            body.put("message", "batch not found");
            body.put("data", null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }
    }

    /**
     * 获取文档状态。
     *
     * @param documentId 文档 ID
     * @return 文档状态
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @GetMapping("/documents/{documentId}")
    public Map<String, Object> getDocument(@PathVariable String documentId, HttpServletRequest request) {
        return ocrQueryHttpFacade.getDocument(request, documentId);
    }

    /**
     * 获取文档 OCR 结果。
     *
     * @param documentId 文档 ID
     * @return OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @GetMapping("/documents/{documentId}/result")
    public Map<String, Object> getDocumentResult(@PathVariable String documentId, HttpServletRequest request) {
        return ocrQueryHttpFacade.getDocumentResult(request, documentId);
    }

    /**
     * 重试失败或卡死文档。
     *
     * @param documentId 文档 ID
     * @return 操作结果
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @PostMapping("/documents/{documentId}/retry")
    public Map<String, Object> retryDocument(@PathVariable String documentId, HttpServletRequest request) {
        ocrQueryHttpFacade.assertDocumentVisible(request, documentId);
        return docLensEngine.retryDocument(documentId);
    }

    /**
     * 删除已完成、失败或卡死文档。
     *
     * @param documentId 文档 ID
     * @return 操作结果
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @DeleteMapping("/documents/{documentId}")
    public Map<String, Object> deleteDocument(@PathVariable String documentId, HttpServletRequest request) {
        ocrQueryHttpFacade.assertDocumentVisible(request, documentId);
        return docLensEngine.deleteDocument(documentId);
    }

    /**
     * 删除批次下所有可删除文档。
     *
     * @param batchId 批次 ID
     * @return 操作结果
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @DeleteMapping("/batches/{batchId}")
    public Map<String, Object> deleteBatch(@PathVariable String batchId, HttpServletRequest request) {
        ocrQueryHttpFacade.assertBatchVisible(request, batchId);
        return docLensEngine.deleteBatch(batchId);
    }

    /**
     * 获取批次事件。
     *
     * @param batchId 批次 ID
     * @return 事件时间线
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @GetMapping("/batches/{batchId}/events")
    public Map<String, Object> getEvents(@PathVariable String batchId, HttpServletRequest request) {
        return ocrQueryHttpFacade.getEvents(request, batchId);
    }
}
