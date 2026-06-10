package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import java.util.List;

/**
 * 文档页图片准备器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public interface PageImagePreparation {

    /**
     * 将文档准备为可独立 OCR 的页图片引用。
     *
     * @param document 文档任务
     * @return 页图片引用集合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    List<PageImageRef> prepare(DocumentJob document);
}
