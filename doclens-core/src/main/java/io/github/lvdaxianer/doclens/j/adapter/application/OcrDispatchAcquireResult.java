package io.github.lvdaxianer.doclens.j.adapter.application;

import java.util.Optional;

/**
 * OCR 派发占槽结果。
 *
 * @param node 命中的运行时节点
 * @param queued 是否已进入等待队列
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public record OcrDispatchAcquireResult(
        Optional<OcrRuntimeNodeView> node,
        boolean queued
) {

    /**
     * 创建已派发结果。
     *
     * @param node 命中的运行时节点
     * @return 已派发结果
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public static OcrDispatchAcquireResult dispatched(OcrRuntimeNodeView node) {
        return new OcrDispatchAcquireResult(Optional.of(node), false);
    }

    /**
     * 创建入队结果。
     *
     * @return 入队结果
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public static OcrDispatchAcquireResult queuedResult() {
        return new OcrDispatchAcquireResult(Optional.empty(), true);
    }
}
