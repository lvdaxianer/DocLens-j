package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Future;

/**
 * 文档线程池任务批次。
 *
 * @author lvdaxianerplus
 * @date 2026-06-16
 */
class DocumentProcessingTaskBatch {

    private final CompletionService<DocumentProcessingResult> completionService;
    private final Map<Future<DocumentProcessingResult>, DocumentJob> documentsByFuture;

    /**
     * 创建文档线程池任务批次。
     *
     * @param completionService 文档完成服务
     * @param expectedSize 预期任务数量
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    DocumentProcessingTaskBatch(CompletionService<DocumentProcessingResult> completionService, int expectedSize) {
        this.completionService = completionService;
        this.documentsByFuture = new LinkedHashMap<>(expectedSize);
    }

    /**
     * 添加文档 Future 映射。
     *
     * @param future 文档处理 Future
     * @param document 文档任务
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    void add(Future<DocumentProcessingResult> future, DocumentJob document) {
        documentsByFuture.put(future, document);
    }

    /**
     * 读取文档完成服务。
     *
     * @return 文档完成服务
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    CompletionService<DocumentProcessingResult> completionService() {
        return completionService;
    }

    /**
     * 获取提交的文档任务数量。
     *
     * @return 文档任务数量
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    int size() {
        return documentsByFuture.size();
    }

    /**
     * 移除并获取指定 Future 对应文档。
     *
     * @param completedFuture 已完成 Future
     * @return Future 对应文档
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    DocumentJob removeDocumentOf(Future<DocumentProcessingResult> completedFuture) {
        return Optional.ofNullable(documentsByFuture.remove(completedFuture))
                .orElseThrow(() -> new IllegalStateException("document task not found"));
    }
}
