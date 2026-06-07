package io.github.lvdaxianer.doclens.j.query.interfaces;

import io.github.lvdaxianer.doclens.j.api.DocLensEngine;
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

    private final DocLensEngine docLensEngine;

    /**
     * Creates OCR query controller.
     *
     * @param docLensEngine DocLens engine
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public OcrQueryController(DocLensEngine docLensEngine) {
        this.docLensEngine = docLensEngine;
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
        return docLensEngine.getBatch(batchId);
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
        return docLensEngine.getDocument(documentId);
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
        return docLensEngine.getDocumentResult(documentId);
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
        return docLensEngine.getEvents(batchId);
    }
}
