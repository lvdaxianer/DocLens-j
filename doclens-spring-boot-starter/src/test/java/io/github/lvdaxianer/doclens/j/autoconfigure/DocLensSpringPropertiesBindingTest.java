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
