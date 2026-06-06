package com.doclens.query.interfaces;

import com.doclens.query.application.OcrQueryService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OCR query API controller.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@RestController
@RequestMapping("/api/v1")
public class OcrQueryController {

    private final OcrQueryService queryService;

    /**
     * Creates OCR query controller.
     *
     * @param queryService OCR query service
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public OcrQueryController(OcrQueryService queryService) {
        this.queryService = queryService;
    }

    /**
     * Gets batch status.
     *
     * @param batchId batch id
     * @return batch status
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @GetMapping("/batches/{batchId}")
    public Map<String, Object> getBatch(@PathVariable String batchId) {
        return queryService.getBatch(batchId);
    }

    /**
     * Gets document status.
     *
     * @param documentId document id
     * @return document status
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @GetMapping("/documents/{documentId}")
    public Map<String, Object> getDocument(@PathVariable String documentId) {
        return queryService.getDocument(documentId);
    }

    /**
     * Gets document OCR result.
     *
     * @param documentId document id
     * @return OCR result
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @GetMapping("/documents/{documentId}/result")
    public Map<String, Object> getDocumentResult(@PathVariable String documentId) {
        return queryService.getDocumentResult(documentId);
    }

    /**
     * Gets batch events.
     *
     * @param batchId batch id
     * @return event timeline
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @GetMapping("/batches/{batchId}/events")
    public Map<String, Object> getEvents(@PathVariable String batchId) {
        return queryService.getEvents(batchId);
    }
}
