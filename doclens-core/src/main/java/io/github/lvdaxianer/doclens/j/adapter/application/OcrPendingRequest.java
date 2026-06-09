package io.github.lvdaxianer.doclens.j.adapter.application;

import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutePolicy;

/**
 * OCR 待派发请求快照。
 *
 * @param request 图片 OCR 请求
 * @param policy OCR 路由策略
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public record OcrPendingRequest(
        ImageOcrRequest request,
        OcrRoutePolicy policy
) {

    /**
     * 从请求和策略创建待派发请求。
     *
     * @param request 图片 OCR 请求
     * @param policy OCR 路由策略
     * @return 待派发请求
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public static OcrPendingRequest from(ImageOcrRequest request, OcrRoutePolicy policy) {
        return new OcrPendingRequest(request, policy);
    }
}
