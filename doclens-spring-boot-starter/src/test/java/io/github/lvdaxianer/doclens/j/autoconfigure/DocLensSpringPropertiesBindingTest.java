package io.github.lvdaxianer.doclens.j.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

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
