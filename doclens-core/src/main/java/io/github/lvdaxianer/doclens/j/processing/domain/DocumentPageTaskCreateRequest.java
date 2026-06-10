package io.github.lvdaxianer.doclens.j.processing.domain;

import java.time.OffsetDateTime;

/**
 * 文档页 OCR 任务创建请求。
 *
 * @param taskId 任务 ID
 * @param batchId 批次 ID
 * @param documentId 文档 ID
 * @param pageNo 页码
 * @param imageStorageUri 页面图片存储地址
 * @param now 当前时间
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public record DocumentPageTaskCreateRequest(
        String taskId,
        String batchId,
        String documentId,
        int pageNo,
        String imageStorageUri,
        OffsetDateTime now
) {
}
