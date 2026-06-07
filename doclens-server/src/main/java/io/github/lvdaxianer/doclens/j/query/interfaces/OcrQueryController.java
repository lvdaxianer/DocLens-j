package io.github.lvdaxianer.doclens.j.query.interfaces;

import io.github.lvdaxianer.doclens.j.api.DocLensEngine;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    /**
     * 创建 OCR 查询控制器。
     *
     * @param docLensEngine DocLens 引擎
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public OcrQueryController(DocLensEngine docLensEngine) {
        this.docLensEngine = docLensEngine;
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
    public Map<String, Object> getBatch(@PathVariable String batchId) {
        return docLensEngine.getBatch(batchId);
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
    public Map<String, Object> getDocument(@PathVariable String documentId) {
        return docLensEngine.getDocument(documentId);
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
    public Map<String, Object> getDocumentResult(@PathVariable String documentId) {
        return docLensEngine.getDocumentResult(documentId);
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
    public Map<String, Object> getEvents(@PathVariable String batchId) {
        return docLensEngine.getEvents(batchId);
    }
}
