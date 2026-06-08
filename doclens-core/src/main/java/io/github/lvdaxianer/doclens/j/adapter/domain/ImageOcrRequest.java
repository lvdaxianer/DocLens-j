package io.github.lvdaxianer.doclens.j.adapter.domain;

import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;

/**
 * 单张图片 OCR 请求。
 *
 * @param batchId 批次 ID
 * @param documentId 文档 ID
 * @param fileName 文件名
 * @param pageNo 页码
 * @param imageContent 图片字节
 * @param metadata 文档元数据
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public record ImageOcrRequest(
        String batchId,
        String documentId,
        String fileName,
        int pageNo,
        byte[] imageContent,
        JsonPayload metadata
) {
}
