package io.github.lvdaxianer.doclens.j.processing.application.extraction;

import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;

/**
 * 文档纯文本提取请求。
 *
 * @param document 文档任务
 * @param content 原始文件字节
 * @param adapterKey OCR 适配器键
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public record DocumentTextExtractionRequest(
        DocumentJob document,
        byte[] content,
        String adapterKey
) {
}
