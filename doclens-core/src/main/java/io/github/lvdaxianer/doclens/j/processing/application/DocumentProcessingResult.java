package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEvent;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResult;
import java.util.List;
import java.util.Optional;

/**
 * 内存中处理单个文档的结果。
 *
 * @param document 最终文档状态
 * @param result 可选 OCR 结果
 * @param events 生命周期事件集合
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public record DocumentProcessingResult(
        DocumentJob document,
        Optional<OcrResult> result,
        List<OcrEvent> events
) {
}
