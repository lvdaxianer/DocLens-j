package io.github.lvdaxianer.doclens.j.shared.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensSpringProperties.RateLimitProperties;
import io.github.lvdaxianer.doclens.j.ingestion.domain.CallerIdentity;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 调用方接口组限流器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
class CallerTrafficRateLimiterTest {

    private static final String DASHBOARD_READ_GROUP = "dashboard-read";
    private static final String UPLOAD_WRITE_GROUP = "upload-write";

    /**
     * 同一调用方不同接口组应隔离计算令牌预算。
     *
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void isolatesBudgetsAcrossTrafficGroupsForSameCaller() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-17T00:00:00Z"));
        CallerTrafficRateLimiter limiter = new CallerTrafficRateLimiter(clock::millis);
        CallerIdentity caller = caller("caller-a", "tenant-east");
        RateLimitProperties limit = new RateLimitProperties(1.0D, 1);

        assertThat(limiter.tryAcquire(caller, UPLOAD_WRITE_GROUP, limit)).isTrue();
        assertThat(limiter.tryAcquire(caller, UPLOAD_WRITE_GROUP, limit)).isFalse();
        assertThat(limiter.tryAcquire(caller, DASHBOARD_READ_GROUP, limit)).isTrue();
    }

    /**
     * 不同调用方即使访问同一接口组也应互不影响。
     *
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void isolatesBudgetsAcrossDifferentCallers() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-17T00:00:00Z"));
        CallerTrafficRateLimiter limiter = new CallerTrafficRateLimiter(clock::millis);
        RateLimitProperties limit = new RateLimitProperties(1.0D, 1);

        assertThat(limiter.tryAcquire(caller("caller-a", "tenant-east"), UPLOAD_WRITE_GROUP, limit)).isTrue();
        assertThat(limiter.tryAcquire(caller("caller-a", "tenant-east"), UPLOAD_WRITE_GROUP, limit)).isFalse();
        assertThat(limiter.tryAcquire(caller("caller-b", "tenant-east"), UPLOAD_WRITE_GROUP, limit)).isTrue();
    }

    /**
     * 不同 caller 分区键访问同一接口组时应拥有独立令牌桶。
     *
     * @author lvdaxianerplus
     * @date 2026-06-22
     */
    @Test
    void isolatesInterfaceGroupBucketsAcrossPartitionKeys() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-17T00:00:00Z"));
        CallerTrafficRateLimiter limiter = new CallerTrafficRateLimiter(clock::millis);
        RateLimitProperties limit = new RateLimitProperties(1.0D, 1);

        assertThat(limiter.tryAcquire(caller("tenant-east", "tenant-east"), UPLOAD_WRITE_GROUP, limit)).isTrue();
        assertThat(limiter.tryAcquire(caller("tenant-east", "tenant-east"), UPLOAD_WRITE_GROUP, limit)).isFalse();
        assertThat(limiter.tryAcquire(caller("tenant-west", "tenant-west"), UPLOAD_WRITE_GROUP, limit)).isTrue();
    }

    /**
     * 小数 QPS 应按时间补充令牌，而不是只支持整数频率。
     *
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void refillsTokensForDecimalQps() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-17T00:00:00Z"));
        CallerTrafficRateLimiter limiter = new CallerTrafficRateLimiter(clock::millis);
        CallerIdentity caller = caller("caller-a", "tenant-east");
        RateLimitProperties limit = new RateLimitProperties(0.5D, 1);

        assertThat(limiter.tryAcquire(caller, UPLOAD_WRITE_GROUP, limit)).isTrue();
        assertThat(limiter.tryAcquire(caller, UPLOAD_WRITE_GROUP, limit)).isFalse();

        clock.advanceMillis(1_999L);
        assertThat(limiter.tryAcquire(caller, UPLOAD_WRITE_GROUP, limit)).isFalse();

        clock.advanceMillis(1L);
        assertThat(limiter.tryAcquire(caller, UPLOAD_WRITE_GROUP, limit)).isTrue();
    }

    /**
     * burst 应允许在补充速率之外先消费预热好的突发额度。
     *
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void allowsBurstCapacityBeforeRejecting() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-17T00:00:00Z"));
        CallerTrafficRateLimiter limiter = new CallerTrafficRateLimiter(clock::millis);
        CallerIdentity caller = caller("caller-a", "tenant-east");
        RateLimitProperties limit = new RateLimitProperties(2.0D, 3);

        assertThat(limiter.tryAcquire(caller, DASHBOARD_READ_GROUP, limit)).isTrue();
        assertThat(limiter.tryAcquire(caller, DASHBOARD_READ_GROUP, limit)).isTrue();
        assertThat(limiter.tryAcquire(caller, DASHBOARD_READ_GROUP, limit)).isTrue();
        assertThat(limiter.tryAcquire(caller, DASHBOARD_READ_GROUP, limit)).isFalse();
    }

    /**
     * 创建测试调用方身份。
     *
     * @param clientId 调用方标识
     * @param tenantKey 租户键
     * @return 调用方身份
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private CallerIdentity caller(String clientId, String tenantKey) {
        return new CallerIdentity(clientId, "dashboard", Optional.ofNullable(tenantKey));
    }

    /**
     * 可手动推进时间的测试时钟。
     *
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private static final class MutableClock {

        private Instant now;

        /**
         * 创建测试时钟。
         *
         * @param now 初始时间
         * @author lvdaxianerplus
         * @date 2026-06-17
         */
        private MutableClock(Instant now) {
            this.now = now;
        }

        /**
         * 读取当前毫秒时间戳。
         *
         * @return 当前毫秒时间戳
         * @author lvdaxianerplus
         * @date 2026-06-17
         */
        private long millis() {
            return now.toEpochMilli();
        }

        /**
         * 推进测试时间。
         *
         * @param millis 需要推进的毫秒数
         * @author lvdaxianerplus
         * @date 2026-06-17
         */
        private void advanceMillis(long millis) {
            now = now.plusMillis(millis);
        }
    }
}
