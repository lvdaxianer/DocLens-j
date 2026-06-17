package io.github.lvdaxianer.doclens.j.shared.web;

/**
 * 调用方接口组限流超额异常。
 *
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
public class RateLimitExceededException extends RuntimeException {

    private final String trafficGroup;
    private final int limit;
    private final int remaining;
    private final int retryAfterSeconds;

    /**
     * 创建限流超额异常。
     *
     * @param trafficGroup 超限接口组
     * @param limit 当前接口组额度
     * @param remaining 当前剩余额度
     * @param retryAfterSeconds 建议重试秒数
     * @param message 异常消息
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public RateLimitExceededException(
            String trafficGroup,
            int limit,
            int remaining,
            int retryAfterSeconds,
            String message
    ) {
        super(message);
        this.trafficGroup = trafficGroup;
        this.limit = limit;
        this.remaining = remaining;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /**
     * 返回超限接口组。
     *
     * @return 接口组名称
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public String trafficGroup() {
        return trafficGroup;
    }

    /**
     * 返回当前接口组额度。
     *
     * @return 接口组额度
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public int limit() {
        return limit;
    }

    /**
     * 返回当前剩余额度。
     *
     * @return 剩余额度
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public int remaining() {
        return remaining;
    }

    /**
     * 返回建议重试秒数。
     *
     * @return 建议重试秒数
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public int retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
