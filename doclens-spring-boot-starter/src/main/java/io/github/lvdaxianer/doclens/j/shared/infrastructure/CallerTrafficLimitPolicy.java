package io.github.lvdaxianer.doclens.j.shared.infrastructure;

import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensSpringProperties.CallerCredentialProperties;
import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensSpringProperties.RateLimitProperties;
import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensSpringProperties.TrafficProperties;
import java.util.Map;
import java.util.Optional;

/**
 * 调用方流量限额合并策略。
 *
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
public final class CallerTrafficLimitPolicy {

    private static final String DEFAULT_TRAFFIC_GROUP = "detail-read";
    private static final RateLimitProperties BUILTIN_DEFAULT_LIMIT = new RateLimitProperties(5.0D, 10);

    /**
     * 解析指定接口组的最终限额。
     *
     * @param credential 调用方限流配置载体
     * @param trafficProperties 全局流量配置
     * @param trafficGroup 接口组
     * @return 最终限额
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public RateLimitProperties resolveLimit(CallerCredentialProperties credential,
                                            TrafficProperties trafficProperties,
                                            String trafficGroup) {
        Optional<RateLimitProperties> callerLimit = credential.rateLimit(trafficGroup);
        // 调用方配置了当前接口组限额时，优先使用调用方自己的限流配置。
        if (callerLimit.isPresent()) {
            return callerLimit.get();
        }
        Optional<RateLimitProperties> defaultLimit = trafficProperties.defaultLimit(trafficGroup);
        // 全局配置了当前接口组默认限额时，使用当前接口组默认限流配置。
        if (defaultLimit.isPresent()) {
            return defaultLimit.get();
        }
        Optional<RateLimitProperties> fallbackLimit = trafficProperties.defaultLimit(DEFAULT_TRAFFIC_GROUP);
        // 当前接口组未配置默认限额时，回退到详情读接口组默认限流配置。
        if (fallbackLimit.isPresent()) {
            return fallbackLimit.get();
        }
        return BUILTIN_DEFAULT_LIMIT;
    }
}
