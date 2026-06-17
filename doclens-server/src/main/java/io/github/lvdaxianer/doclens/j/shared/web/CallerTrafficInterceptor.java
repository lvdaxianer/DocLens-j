package io.github.lvdaxianer.doclens.j.shared.web;

import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensSpringProperties;
import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensSpringProperties.CallerCredentialProperties;
import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensSpringProperties.RateLimitProperties;
import io.github.lvdaxianer.doclens.j.ingestion.infrastructure.CallerCredentialResolver.ResolvedCallerCredential;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.CallerTrafficLimitPolicy;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.CallerTrafficRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 在 caller 鉴权后执行调用方接口组限流。
 *
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
@Component
public class CallerTrafficInterceptor implements HandlerInterceptor {

    private static final String RATE_LIMIT_EXCEEDED_MESSAGE = "caller traffic limit exceeded";

    private final DocLensSpringProperties properties;
    private final TrafficGroupResolver trafficGroupResolver;
    private final CallerTrafficLimitPolicy callerTrafficLimitPolicy;
    private final CallerTrafficRateLimiter callerTrafficRateLimiter;

    /**
     * 创建调用方流量拦截器。
     *
     * @param properties Spring 绑定配置
     * @param trafficGroupResolver 接口组解析器
     * @param callerTrafficLimitPolicy 限流策略合并器
     * @param callerTrafficRateLimiter 调用方限流器
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public CallerTrafficInterceptor(
            DocLensSpringProperties properties,
            TrafficGroupResolver trafficGroupResolver,
            CallerTrafficLimitPolicy callerTrafficLimitPolicy,
            CallerTrafficRateLimiter callerTrafficRateLimiter
    ) {
        this.properties = properties;
        this.trafficGroupResolver = trafficGroupResolver;
        this.callerTrafficLimitPolicy = callerTrafficLimitPolicy;
        this.callerTrafficRateLimiter = callerTrafficRateLimiter;
    }

    /**
     * 在控制器前按 caller + group 消费限流令牌。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param handler 当前处理器
     * @return 是否继续执行
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (properties.traffic().enabled()) {
            enforceTrafficLimit(request);
            return true;
        } else {
            return true;
        }
    }

    /**
     * 根据当前请求执行限流判断。
     *
     * @param request HTTP 请求
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private void enforceTrafficLimit(HttpServletRequest request) {
        ResolvedCallerCredential resolvedCredential = resolvedCredential(request);
        TrafficGroup trafficGroup = trafficGroupResolver.resolve(request);
        RateLimitProperties limit = resolveLimit(resolvedCredential.credential(), trafficGroup);
        boolean acquired = callerTrafficRateLimiter.tryAcquire(resolvedCredential.callerIdentity(), trafficGroup.value(), limit);
        if (!acquired) {
            throw new RateLimitExceededException(
                    trafficGroup.value(),
                    Math.max(1, limit.burst()),
                    0,
                    retryAfterSeconds(limit),
                    RATE_LIMIT_EXCEEDED_MESSAGE);
        } else {
            // 已成功消费令牌，继续后续业务处理。
        }
    }

    /**
     * 读取请求中已解析的 caller 凭证结果。
     *
     * @param request HTTP 请求
     * @return caller 凭证结果
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private ResolvedCallerCredential resolvedCredential(HttpServletRequest request) {
        Object resolved = request.getAttribute(CallerCredentialInterceptor.RESOLVED_CREDENTIAL_ATTRIBUTE);
        if (resolved instanceof ResolvedCallerCredential credential) {
            return credential;
        } else {
            throw new IllegalStateException("resolved caller credential is required before traffic enforcement");
        }
    }

    /**
     * 合并当前接口组的最终限额。
     *
     * @param credential 调用方配置凭证
     * @param trafficGroup 流量接口组
     * @return 最终限额
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private RateLimitProperties resolveLimit(CallerCredentialProperties credential, TrafficGroup trafficGroup) {
        return callerTrafficLimitPolicy.resolveLimit(credential, properties.traffic(), trafficGroup.value());
    }

    /**
     * 根据限流速率估算建议重试秒数。
     *
     * @param limit 限流配置
     * @return 建议重试秒数
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private int retryAfterSeconds(RateLimitProperties limit) {
        if (limit.qps() > 0.0D) {
            return Math.max(1, (int) Math.ceil(1.0D / limit.qps()));
        } else {
            return 1;
        }
    }
}
