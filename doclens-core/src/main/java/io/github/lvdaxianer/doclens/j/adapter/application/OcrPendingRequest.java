package io.github.lvdaxianer.doclens.j.adapter.application;

import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutePolicy;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * OCR 待派发请求快照。
 *
 * @param request 图片 OCR 请求
 * @param policy OCR 路由策略
 * @param excludedNodeIds 当前已排除节点
 * @param dispatchFuture 等待派发完成的结果
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public record OcrPendingRequest(
        ImageOcrRequest request,
        OcrRoutePolicy policy,
        Set<String> excludedNodeIds,
        CompletableFuture<OcrRuntimeNodeView> dispatchFuture
) {

    /**
     * 从请求和策略创建待派发请求。
     *
     * @param request 图片 OCR 请求
     * @param policy OCR 路由策略
     * @param excludedNodeIds 当前已排除节点
     * @param dispatchFuture 等待派发完成的结果
     * @return 待派发请求
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public static OcrPendingRequest from(
            ImageOcrRequest request,
            OcrRoutePolicy policy,
            Set<String> excludedNodeIds,
            CompletableFuture<OcrRuntimeNodeView> dispatchFuture
    ) {
        return new OcrPendingRequest(request, policy, Set.copyOf(excludedNodeIds), dispatchFuture);
    }
}
