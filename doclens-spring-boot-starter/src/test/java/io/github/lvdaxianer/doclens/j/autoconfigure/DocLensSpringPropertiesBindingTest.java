package io.github.lvdaxianer.doclens.j.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * DocLens Spring 配置绑定测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
class DocLensSpringPropertiesBindingTest {

    /** 完整可信网关认证配置。 */
    private static final String[] GATEWAY_AUTH_PROPERTIES = {
            "doclens.gateway-auth.enabled=true",
            "doclens.gateway-auth.trusted-gateway.header-name=X-Doclens-Gateway-Secret",
            "doclens.gateway-auth.trusted-gateway.header-value=local-secret",
            "doclens.gateway-auth.principal-headers.principal=X-Doclens-Principal",
            "doclens.gateway-auth.principal-headers.roles=X-Doclens-Roles",
            "doclens.gateway-auth.principals[0].principal=alice",
            "doclens.gateway-auth.principals[0].roles[0]=user",
            "doclens.gateway-auth.principals[0].allowed-partitions[0]=alice-workspace",
            "doclens.gateway-auth.route-policies[0].pattern=/api/v1/**",
            "doclens.gateway-auth.route-policies[0].required-role=user"
    };
    /** 使用默认嵌套项的可信网关认证配置。 */
    private static final String[] GATEWAY_AUTH_WITH_DEFAULT_NESTED_PROPERTIES = {
            "doclens.gateway-auth.enabled=true",
            "doclens.gateway-auth.trusted-gateway.header-value=local-secret",
            "doclens.gateway-auth.principals[0].principal=alice",
            "doclens.gateway-auth.principals[0].allowed-partitions[0]=alice-workspace"
    };
    /** 缺少网关共享密钥的可信网关认证配置。 */
    private static final String[] GATEWAY_AUTH_WITHOUT_SECRET_PROPERTIES = {
            "doclens.gateway-auth.enabled=true",
            "doclens.gateway-auth.trusted-gateway.header-name=X-Doclens-Gateway-Secret",
            "doclens.gateway-auth.principals[0].principal=alice",
            "doclens.gateway-auth.principals[0].allowed-partitions[0]=alice-workspace"
    };
    /** 缺少 principal allowlist 的可信网关认证配置。 */
    private static final String[] GATEWAY_AUTH_WITHOUT_PRINCIPALS_PROPERTIES = {
            "doclens.gateway-auth.enabled=true",
            "doclens.gateway-auth.trusted-gateway.header-value=local-secret"
    };
    /** principal 条目缺少名称的可信网关认证配置。 */
    private static final String[] GATEWAY_AUTH_WITHOUT_PRINCIPAL_NAME_PROPERTIES = {
            "doclens.gateway-auth.enabled=true",
            "doclens.gateway-auth.trusted-gateway.header-value=local-secret",
            "doclens.gateway-auth.principals[0].roles[0]=user",
            "doclens.gateway-auth.principals[0].allowed-partitions[0]=alice-workspace"
    };
    /** principal 条目缺少分区授权的可信网关认证配置。 */
    private static final String[] GATEWAY_AUTH_WITHOUT_ALLOWED_PARTITIONS_PROPERTIES = {
            "doclens.gateway-auth.enabled=true",
            "doclens.gateway-auth.trusted-gateway.header-value=local-secret",
            "doclens.gateway-auth.principals[0].principal=alice",
            "doclens.gateway-auth.principals[0].roles[0]=user"
    };

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(BindingConfiguration.class)
            .withPropertyValues(
                    "doclens.storage-root=./var/storage",
                    "doclens.auto-process-on-upload=true",
                    "doclens.worker-id=local-worker");

    /**
     * 绑定最小配置时应成功创建属性对象。
     *
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void bindsConfigurationThroughCanonicalConstructor() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(DocLensSpringProperties.class);
            assertThat(context.getBean(DocLensSpringProperties.class).clients().credentials()).isEmpty();
            assertThat(context.getBean(DocLensSpringProperties.class).gatewayAuth().enabled()).isFalse();
        });
    }

    /**
     * 绑定调用方凭证与默认接口组限流配置。
     *
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void bindsCallerTrafficGovernanceConfiguration() {
        contextRunner
                .withPropertyValues(
                        "doclens.clients.credentials[0].client-id=local-demo",
                        "doclens.clients.credentials[0].source-app=dashboard",
                        "doclens.clients.credentials[0].tenant-key=local",
                        "doclens.clients.credentials[0].api-key=local-key",
                        "doclens.clients.credentials[0].rate-limits.[dashboard-read].qps=20",
                        "doclens.clients.credentials[0].rate-limits.[dashboard-read].burst=40",
                        "doclens.traffic.enabled=true",
                        "doclens.traffic.anonymous-enabled=false",
                        "doclens.traffic.default-limits.[upload-write].qps=0.5",
                        "doclens.traffic.default-limits.[upload-write].burst=2",
                        "doclens.traffic.global-protection.enabled=true",
                        "doclens.traffic.global-protection.max-in-flight=100")
                .run(context -> {
                    DocLensSpringProperties properties = context.getBean(DocLensSpringProperties.class);
                    DocLensSpringProperties.CallerCredentialProperties credential =
                            properties.clients().credentials().getFirst();

                    assertThat(properties.traffic().enabled()).isTrue();
                    assertThat(properties.traffic().anonymousEnabled()).isFalse();
                    assertThat(credential.rateLimit("dashboard-read"))
                            .get()
                            .extracting(DocLensSpringProperties.RateLimitProperties::qps,
                                    DocLensSpringProperties.RateLimitProperties::burst)
                            .containsExactly(20.0D, 40);
                    assertThat(properties.traffic().defaultLimit("upload-write"))
                            .get()
                            .extracting(DocLensSpringProperties.RateLimitProperties::qps,
                                    DocLensSpringProperties.RateLimitProperties::burst)
                            .containsExactly(0.5D, 2);
                    assertThat(properties.traffic().globalProtection().enabled()).isTrue();
                    assertThat(properties.traffic().globalProtection().maxInFlight()).isEqualTo(100);
                });
    }

    /**
     * 绑定 OCR 派发背压与等待超时配置。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-12
     */
    @Test
    void bindsOcrDispatchBackpressureConfiguration() {
        contextRunner
                .withPropertyValues(
                        "doclens.ocr.dispatch-wait-timeout=250ms",
                        "doclens.ocr.dispatch-queue-capacity=7")
                .run(context -> {
                    DocLensSpringProperties.OcrProperties ocr = context.getBean(DocLensSpringProperties.class).ocr();

                    assertThat(ocr.dispatchWaitTimeout()).isEqualTo(Duration.ofMillis(250));
                    assertThat(ocr.dispatchQueueCapacity()).isEqualTo(7);
                });
    }

    /**
     * 绑定可信网关认证配置。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    @Test
    void bindsGatewayAuthenticationConfiguration() {
        contextRunner.withPropertyValues(GATEWAY_AUTH_PROPERTIES).run(context -> {
            GatewayAuthProperties gatewayAuth = context.getBean(DocLensSpringProperties.class).gatewayAuth();

            assertThat(gatewayAuth.enabled()).isTrue();
            assertThat(gatewayAuth.trustedGateway().headerValue()).isEqualTo("local-secret");
            assertThat(gatewayAuth.principals()).singleElement()
                    .extracting(GatewayAuthProperties.PrincipalProperties::principal)
                    .isEqualTo("alice");
            assertThat(gatewayAuth.routePolicies()).singleElement()
                    .extracting(GatewayAuthProperties.RoutePolicyProperties::requiredRole)
                    .isEqualTo("user");
        });
    }

    /**
     * 部分绑定可信网关认证配置时应补齐嵌套默认项。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    @Test
    void appliesGatewayAuthNestedDefaultsWhenPartiallyConfigured() {
        contextRunner.withPropertyValues(GATEWAY_AUTH_WITH_DEFAULT_NESTED_PROPERTIES).run(context -> {
            GatewayAuthProperties gatewayAuth = context.getBean(DocLensSpringProperties.class).gatewayAuth();

            assertThat(gatewayAuth.trustedGateway().headerName()).isEqualTo("X-Doclens-Gateway-Secret");
            assertThat(gatewayAuth.principalHeaders().principal()).isEqualTo("X-Doclens-Principal");
            assertThat(gatewayAuth.routePolicies()).hasSize(6);
        });
    }

    /**
     * 启用可信网关认证时缺少网关密钥应启动失败。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    @Test
    void failsFastWhenGatewayAuthEnabledWithoutGatewaySecret() {
        assertGatewayAuthStartupFailure(GATEWAY_AUTH_WITHOUT_SECRET_PROPERTIES,
                "doclens.gateway-auth.trusted-gateway.header-value is required when gateway auth is enabled");
    }

    /**
     * 启用可信网关认证时缺少 principal allowlist 应启动失败。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    @Test
    void failsFastWhenGatewayAuthEnabledWithoutPrincipals() {
        assertGatewayAuthStartupFailure(GATEWAY_AUTH_WITHOUT_PRINCIPALS_PROPERTIES,
                "doclens.gateway-auth.principals is required when gateway auth is enabled");
    }

    /**
     * 启用可信网关认证时 principal 条目缺少名称应启动失败。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    @Test
    void failsFastWhenGatewayAuthPrincipalNameIsMissing() {
        assertGatewayAuthStartupFailure(GATEWAY_AUTH_WITHOUT_PRINCIPAL_NAME_PROPERTIES,
                "doclens.gateway-auth.principals[0].principal is required when gateway auth is enabled");
    }

    /**
     * 启用可信网关认证时 principal 条目缺少分区授权应启动失败。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    @Test
    void failsFastWhenGatewayAuthPrincipalPartitionsAreMissing() {
        assertGatewayAuthStartupFailure(GATEWAY_AUTH_WITHOUT_ALLOWED_PARTITIONS_PROPERTIES,
                "doclens.gateway-auth.principals[0].allowed-partitions is required when gateway auth is enabled");
    }

    /**
     * 断言可信网关认证配置会触发启动失败。
     *
     * @param properties 配置属性集合
     * @param rootCauseMessage 根因错误消息
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    private void assertGatewayAuthStartupFailure(String[] properties, String rootCauseMessage) {
        contextRunner.withPropertyValues(properties).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseMessage(rootCauseMessage);
        });
    }

    /**
     * 绑定页任务 worker 运行时配置。
     *
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    @Test
    void bindsPageTaskWorkerRuntimeConfiguration() {
        contextRunner
                .withPropertyValues(
                        "doclens.page-task-worker.batch-size=10",
                        "doclens.page-task-worker.lock-seconds=900",
                        "doclens.page-task-worker.pool-size=10",
                        "doclens.page-task-worker.queue-capacity=500",
                        "doclens.page-task-worker.recovery-limit=64",
                        "doclens.page-task-worker.interval-millis=250")
                .run(context -> {
                    DocLensSpringProperties.PageTaskWorkerProperties worker =
                            context.getBean(DocLensSpringProperties.class).pageTaskWorker();

                    assertThat(worker.batchSize()).isEqualTo(10);
                    assertThat(worker.lockSeconds()).isEqualTo(900);
                    assertThat(worker.poolSize()).isEqualTo(10);
                    assertThat(worker.queueCapacity()).isEqualTo(500);
                    assertThat(worker.recoveryLimit()).isEqualTo(64);
                    assertThat(worker.intervalMillis()).isEqualTo(250);
                });
    }

    /**
     * 配置属性绑定测试配置。
     *
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(DocLensSpringProperties.class)
    static class BindingConfiguration {
    }
}
