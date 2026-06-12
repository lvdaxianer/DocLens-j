package io.github.lvdaxianer.doclens.j.adapter.application;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 进程内 OCR 文档级模型亲和力跟踪器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
public class InMemoryOcrDocumentAffinityTracker implements OcrDocumentAffinityTracker {

    private static final int INITIAL_DOCUMENT_CAPACITY = 64;

    private final ConcurrentMap<String, String> documentModelKeys =
            new ConcurrentHashMap<>(INITIAL_DOCUMENT_CAPACITY);

    /**
     * 查询文档已绑定的 OCR 模型。
     *
     * @param documentId 文档 ID
     * @return 已绑定模型标识
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Override
    public Optional<String> boundModelKey(String documentId) {
        return Optional.ofNullable(documentModelKeys.get(documentId));
    }

    /**
     * 在文档尚未绑定时绑定模型。
     *
     * @param documentId 文档 ID
     * @param modelKey OCR 模型标识
     * @return 最终绑定的 OCR 模型标识
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Override
    public String bindIfAbsent(String documentId, String modelKey) {
        return documentModelKeys.computeIfAbsent(documentId, ignored -> modelKey);
    }

    /**
     * 释放文档模型亲和力。
     *
     * @param documentId 文档 ID
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Override
    public void release(String documentId) {
        documentModelKeys.remove(documentId);
    }
}
