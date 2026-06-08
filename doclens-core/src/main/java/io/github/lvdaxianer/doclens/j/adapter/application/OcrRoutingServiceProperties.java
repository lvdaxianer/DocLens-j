package io.github.lvdaxianer.doclens.j.adapter.application;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutePolicy;

/**
 * OCR 路由服务配置。
 *
 * @param defaultPolicy 默认路由策略
 * @param requestRetryTimes OCR 请求重试次数
 * @param specificNodeFallbackEnabled 指定节点失败后是否允许回退
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public record OcrRoutingServiceProperties(
        OcrRoutePolicy defaultPolicy,
        int requestRetryTimes,
        boolean specificNodeFallbackEnabled
) {
}
