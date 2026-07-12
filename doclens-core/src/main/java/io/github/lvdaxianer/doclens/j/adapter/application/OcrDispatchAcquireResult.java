package io.github.lvdaxianer.doclens.j.adapter.application;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

    private static final String QUEUE_FULL_MESSAGE = "ocr dispatch queue is full";
    private static final String DISPATCH_FAILED_MESSAGE = "ocr dispatch failed";
    private static final String DISPATCH_INTERRUPTED_MESSAGE = "ocr dispatch wait interrupted";
    private static final String DISPATCH_TIMEOUT_MESSAGE = "ocr dispatch wait timed out";

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
     * 创建待派发队列已满的失败结果。
     *
     * @return 待派发队列已满结果
     * @author lvdaxianer@yeah.net
     * @date 2026-07-12
     */
    public static OcrDispatchAcquireResult queueFull() {
        return failedResult(queueFullException());
    }

    /**
     * 创建待派发队列已满异常。
     *
     * @return 待派发队列已满异常
     * @author lvdaxianer@yeah.net
     * @date 2026-07-12
     */
    static OcrRouteExecutionException queueFullException() {
        return new OcrRouteExecutionException(QUEUE_FULL_MESSAGE);
    }

    /**
     * 创建派发失败结果。
     *
     * @param cause 派发失败原因
     * @return 派发失败结果
     * @author lvdaxianer@yeah.net
     * @date 2026-07-12
     */
    public static OcrDispatchAcquireResult failedResult(Throwable cause) {
        return new OcrDispatchAcquireResult(Optional.empty(), false, CompletableFuture.failedFuture(cause));
    }

    /**
     * 同步等待派发完成并返回最终命中的节点。
     *
     * @return 最终命中的节点
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public OcrRuntimeNodeView awaitDispatch() {
        try {
            return dispatchFuture.join();
        } catch (CompletionException exception) {
            throw routeException(exception.getCause());
        }
    }

    /**
     * 在指定超时时间内等待派发完成。
     *
     * @param timeout 派发等待超时时间
     * @return 最终命中的节点
     * @author lvdaxianer@yeah.net
     * @date 2026-07-12
     */
    public OcrRuntimeNodeView awaitDispatch(Duration timeout) {
        try {
            return dispatchFuture.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            throw new OcrRouteExecutionException(DISPATCH_TIMEOUT_MESSAGE, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new OcrRouteExecutionException(DISPATCH_INTERRUPTED_MESSAGE, exception);
        } catch (ExecutionException exception) {
            throw routeException(exception.getCause());
        }
    }

    /**
     * 将异步派发失败统一转换为路由执行异常。
     *
     * @param cause 派发失败原因
     * @return 路由执行异常
     * @author lvdaxianer@yeah.net
     * @date 2026-07-12
     */
    private OcrRouteExecutionException routeException(Throwable cause) {
        if (cause instanceof OcrRouteExecutionException routeException) {
            return routeException;
        } else {
            return new OcrRouteExecutionException(DISPATCH_FAILED_MESSAGE, cause);
        }
    }
}
