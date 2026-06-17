package io.github.lvdaxianer.doclens.j.shared.infrastructure;

import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensSpringProperties.RateLimitProperties;
import io.github.lvdaxianer.doclens.j.ingestion.domain.CallerIdentity;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.LongSupplier;

/**
 * 按调用方与接口组隔离的内存限流器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
public final class CallerTrafficRateLimiter {

    private static final int MIN_BURST = 1;
    private static final long MILLIS_PER_SECOND = 1_000L;

    private final ConcurrentMap<String, TokenBucketState> buckets = new ConcurrentHashMap<>();
    private final LongSupplier currentTimeMillisSupplier;

    /**
     * 使用系统时间创建限流器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public CallerTrafficRateLimiter() {
        this(System::currentTimeMillis);
    }

    /**
     * 使用自定义时间源创建限流器。
     *
     * @param currentTimeMillisSupplier 当前时间毫秒提供者
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public CallerTrafficRateLimiter(LongSupplier currentTimeMillisSupplier) {
        this.currentTimeMillisSupplier = currentTimeMillisSupplier;
    }

    /**
     * 尝试为调用方当前接口组消费一个令牌。
     *
     * @param callerIdentity 调用方身份
     * @param trafficGroup 接口组
     * @param limitProperties 限流配置
     * @return 获取到令牌时返回 true，否则返回 false
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public boolean tryAcquire(CallerIdentity callerIdentity, String trafficGroup,
                              RateLimitProperties limitProperties) {
        long nowMillis = currentTimeMillisSupplier.getAsLong();
        String bucketKey = bucketKey(callerIdentity, trafficGroup);
        TokenBucketState bucket = buckets.computeIfAbsent(bucketKey,
                ignored -> TokenBucketState.initial(limitProperties, nowMillis));
        synchronized (bucket) {
            bucket.refresh(limitProperties, nowMillis);
            return bucket.tryConsume();
        }
    }

    /**
     * 生成调用方接口组限流桶键。
     *
     * @param callerIdentity 调用方身份
     * @param trafficGroup 接口组
     * @return 限流桶键
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private String bucketKey(CallerIdentity callerIdentity, String trafficGroup) {
        String tenantKey = callerIdentity.tenantKey().orElse("");
        return String.join("|", callerIdentity.clientId(), tenantKey, trafficGroup);
    }

    /**
     * 单个调用方接口组的令牌桶状态。
     *
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private static final class TokenBucketState {

        private double availableTokens;
        private long lastRefillMillis;
        private int burst;
        private double qps;

        /**
         * 创建初始满桶状态。
         *
         * @param limitProperties 限流配置
         * @param nowMillis 当前时间毫秒值
         * @return 初始令牌桶状态
         * @author lvdaxianerplus
         * @date 2026-06-17
         */
        private static TokenBucketState initial(RateLimitProperties limitProperties, long nowMillis) {
            int initialBurst = sanitizeBurst(limitProperties);
            double initialQps = sanitizeQps(limitProperties);
            return new TokenBucketState(initialBurst, nowMillis, initialBurst, initialQps);
        }

        /**
         * 创建令牌桶状态。
         *
         * @param availableTokens 当前可用令牌
         * @param lastRefillMillis 上次补充时间
         * @param burst 最大突发容量
         * @param qps 每秒补充速率
         * @author lvdaxianerplus
         * @date 2026-06-17
         */
        private TokenBucketState(double availableTokens, long lastRefillMillis, int burst, double qps) {
            this.availableTokens = availableTokens;
            this.lastRefillMillis = lastRefillMillis;
            this.burst = burst;
            this.qps = qps;
        }

        /**
         * 刷新当前桶的配置和令牌余额。
         *
         * @param limitProperties 最新限流配置
         * @param nowMillis 当前时间毫秒值
         * @author lvdaxianerplus
         * @date 2026-06-17
         */
        private void refresh(RateLimitProperties limitProperties, long nowMillis) {
            burst = sanitizeBurst(limitProperties);
            qps = sanitizeQps(limitProperties);
            refill(nowMillis);
            availableTokens = Math.min(availableTokens, burst);
        }

        /**
         * 尝试消费一个令牌。
         *
         * @return 消费成功时返回 true
         * @author lvdaxianerplus
         * @date 2026-06-17
         */
        private boolean tryConsume() {
            if (availableTokens >= 1.0D) {
                availableTokens -= 1.0D;
                return true;
            } else {
                return false;
            }
        }

        /**
         * 根据经过时间补充令牌。
         *
         * @param nowMillis 当前时间毫秒值
         * @author lvdaxianerplus
         * @date 2026-06-17
         */
        private void refill(long nowMillis) {
            long elapsedMillis = Math.max(0L, nowMillis - lastRefillMillis);
            if (elapsedMillis > 0L) {
                double refillTokens = elapsedMillis * qps / MILLIS_PER_SECOND;
                availableTokens = Math.min(burst, availableTokens + refillTokens);
                lastRefillMillis = nowMillis;
            } else {
                // 当前请求与上次刷新处于同一毫秒时无需补充令牌。
            }
        }

        /**
         * 规整突发容量下限。
         *
         * @param limitProperties 限流配置
         * @return 规整后的突发容量
         * @author lvdaxianerplus
         * @date 2026-06-17
         */
        private static int sanitizeBurst(RateLimitProperties limitProperties) {
            return Math.max(MIN_BURST, limitProperties.burst());
        }

        /**
         * 规整每秒补充速率下限。
         *
         * @param limitProperties 限流配置
         * @return 规整后的每秒补充速率
         * @author lvdaxianerplus
         * @date 2026-06-17
         */
        private static double sanitizeQps(RateLimitProperties limitProperties) {
            return Math.max(0.0D, limitProperties.qps());
        }
    }
}
