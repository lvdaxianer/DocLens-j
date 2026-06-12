package io.github.lvdaxianer.doclens.j.adapter.application;

import java.util.Optional;

/**
 * OCR 文档级模型亲和力跟踪器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
public interface OcrDocumentAffinityTracker {

    /**
     * 查询文档已绑定的 OCR 模型。
     *
     * @param documentId 文档 ID
     * @return 已绑定模型标识
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    Optional<String> boundModelKey(String documentId);

    /**
     * 在文档尚未绑定时绑定模型。
     *
     * @param documentId 文档 ID
     * @param modelKey OCR 模型标识
     * @return 最终绑定的 OCR 模型标识
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    String bindIfAbsent(String documentId, String modelKey);

    /**
     * 释放文档模型亲和力。
     *
     * @param documentId 文档 ID
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    void release(String documentId);
}
