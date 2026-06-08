package io.github.lvdaxianer.doclens.j.processing.application.extraction;

import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;

/**
 * 文档纯文本提取请求。
 *
 * @param document 文档任务
 * @param content 原始文件字节
 * @param adapterKey OCR 适配器键
 * @param progressReporter 进度上报器
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public record DocumentTextExtractionRequest(
        DocumentJob document,
        byte[] content,
        String adapterKey,
        DocumentProgressReporter progressReporter
) {
    /**
     * 创建不带进度上报器的提取请求。
     *
     * @param document 文档任务
     * @param content 原始文件字节
     * @param adapterKey OCR 适配器键
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public DocumentTextExtractionRequest(DocumentJob document, byte[] content, String adapterKey) {
        this(document, content, adapterKey, DocumentProgressReporter.noop());
    }

    /**
     * 创建带安全默认值的提取请求。
     *
     * @param document 文档任务
     * @param content 原始文件字节
     * @param adapterKey OCR 适配器键
     * @param progressReporter 进度上报器
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public DocumentTextExtractionRequest {
        progressReporter = progressReporter == null ? DocumentProgressReporter.noop() : progressReporter;
    }
}
