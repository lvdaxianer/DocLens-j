package io.github.lvdaxianer.doclens.j.adapter.application;

import java.util.concurrent.CompletableFuture;
import java.util.Optional;

/**
 * OCR 派发占槽结果。
 *
 * @param node 命中的运行时节点
 * @param queued 是否已进入等待队列
 * @param dispatchFuture 等待派发完成的结果
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public record OcrDispatchAcquireResult(
        Optional<OcrRuntimeNodeView> node,
        boolean queued,
        CompletableFuture<OcrRuntimeNodeView> dispatchFuture
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
        return new OcrDispatchAcquireResult(Optional.of(node), false, CompletableFuture.completedFuture(node));
    }

    /**
     * 创建入队结果。
     *
     * @param dispatchFuture 等待派发完成的 Future
     * @return 入队结果
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public static OcrDispatchAcquireResult queuedResult(CompletableFuture<OcrRuntimeNodeView> dispatchFuture) {
        return new OcrDispatchAcquireResult(Optional.empty(), true, dispatchFuture);
    }

    /**
     * 创建无可派发候选节点结果。
     *
     * @return 无可派发候选节点结果
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public static OcrDispatchAcquireResult unavailable() {
        return new OcrDispatchAcquireResult(Optional.empty(), false,
                CompletableFuture.failedFuture(new IllegalStateException("no healthy ocr candidates")));
    }

    /**
     * 同步等待派发完成并返回最终命中的节点。
     *
     * @return 最终命中的节点
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public OcrRuntimeNodeView awaitDispatch() {
        return dispatchFuture.join();
    }
}
