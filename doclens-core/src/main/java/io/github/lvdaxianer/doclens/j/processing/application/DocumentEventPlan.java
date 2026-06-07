package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import java.util.Map;

/**
 * 用于构建 OCR 文档事件的参数对象。
 *
 * @param document 文档任务快照
 * @param eventType OCR 事件类型
 * @param progress 进度载荷
 * @param detail 事件详情载荷
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public record DocumentEventPlan(
        DocumentJob document,
        String eventType,
        Map<String, Object> progress,
        Map<String, Object> detail
) {
}
