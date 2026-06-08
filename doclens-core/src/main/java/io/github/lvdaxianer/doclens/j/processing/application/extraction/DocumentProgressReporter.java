package io.github.lvdaxianer.doclens.j.processing.application.extraction;

import io.github.lvdaxianer.doclens.j.processing.domain.ProcessingStage;

/**
 * 文档提取过程中的阶段进度上报端口。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
@FunctionalInterface
public interface DocumentProgressReporter {

    /**
     * 上报当前处理阶段和图片页进度。
     *
     * @param stage 当前阶段
     * @param completedImages 已完成图片数量
     * @param totalImages 总图片数量
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    void report(ProcessingStage stage, int completedImages, int totalImages);

    /**
     * 创建不执行任何操作的上报器。
     *
     * @return 空进度上报器
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    static DocumentProgressReporter noop() {
        return (stage, completedImages, totalImages) -> {
            // 默认上报器用于测试或兼容旧调用方。
        };
    }
}
