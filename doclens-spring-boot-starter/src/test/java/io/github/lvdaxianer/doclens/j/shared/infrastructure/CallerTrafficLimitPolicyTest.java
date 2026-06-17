package io.github.lvdaxianer.doclens.j.shared.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensSpringProperties.CallerCredentialProperties;
import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensSpringProperties.GlobalProtectionProperties;
import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensSpringProperties.RateLimitProperties;
import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensSpringProperties.TrafficProperties;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 调用方流量策略合并测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
class CallerTrafficLimitPolicyTest {

    /**
     * caller 覆盖应优先于默认配置，未知接口组应回退到 detail-read。
     *
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void prefersCallerOverrideBeforeTrafficDefaultAndBuiltinFallback() {
        CallerTrafficLimitPolicy policy = new CallerTrafficLimitPolicy();
        CallerCredentialProperties credential = credential();
        TrafficProperties traffic = traffic();

        assertThat(policy.resolveLimit(credential, traffic, "dashboard-read"))
                .isEqualTo(new RateLimitProperties(20.0D, 40));
        assertThat(policy.resolveLimit(credential, traffic, "upload-write"))
                .isEqualTo(new RateLimitProperties(0.5D, 2));
        assertThat(policy.resolveLimit(credential, traffic, "unknown-group"))
                .isEqualTo(new RateLimitProperties(5.0D, 10));
    }

    /**
     * 仅有默认配置时应返回默认接口组额度。
     *
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void fallsBackToTrafficDefaultWhenCallerOverrideIsMissing() {
        CallerTrafficLimitPolicy policy = new CallerTrafficLimitPolicy();

        assertThat(policy.resolveLimit(noOverrideCredential(), traffic(), "upload-write"))
                .isEqualTo(new RateLimitProperties(0.5D, 2));
    }

    /**
     * 创建带 caller 覆盖的测试凭证。
     *
     * @return 测试凭证
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private CallerCredentialProperties credential() {
        return new CallerCredentialProperties(
                "caller-a",
                "dashboard",
                "tenant-east",
                "api-key",
                "bearer",
                Map.of("dashboard-read", new RateLimitProperties(20.0D, 40)));
    }

    /**
     * 创建没有覆盖的测试凭证。
     *
     * @return 测试凭证
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private CallerCredentialProperties noOverrideCredential() {
        return new CallerCredentialProperties("caller-a", "dashboard", "tenant-east", "api-key", "bearer",
                Map.of());
    }

    /**
     * 创建默认流量治理配置。
     *
     * @return 流量治理配置
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private TrafficProperties traffic() {
        return new TrafficProperties(true, false, Map.of(
                "dashboard-read", new RateLimitProperties(10.0D, 20),
                "upload-write", new RateLimitProperties(0.5D, 2)),
                new GlobalProtectionProperties(true, 100));
    }
}
