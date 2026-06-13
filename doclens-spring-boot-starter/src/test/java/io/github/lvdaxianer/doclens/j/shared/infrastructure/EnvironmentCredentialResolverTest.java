package io.github.lvdaxianer.doclens.j.shared.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 环境变量凭证解析器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-13
 */
class EnvironmentCredentialResolverTest {

    /**
     * 已配置环境变量时应返回真实凭证值。
     *
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    @Test
    void resolvesConfiguredEnvironmentValue() {
        EnvironmentCredentialResolver resolver = new EnvironmentCredentialResolver(Map.of("MINIMAX_API_KEY",
                "sk-test"));

        String credential = resolver.resolve("MINIMAX_API_KEY");

        assertThat(credential).isEqualTo("sk-test");
    }

    /**
     * 环境变量缺失时应返回清晰错误。
     *
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    @Test
    void rejectsMissingEnvironmentValue() {
        EnvironmentCredentialResolver resolver = new EnvironmentCredentialResolver(Map.of());

        assertThatThrownBy(() -> resolver.resolve("MINIMAX_API_KEY"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("credential environment variable MINIMAX_API_KEY is not configured");
    }

    /**
     * 环境变量名为空时应允许无凭证本地模型路径。
     *
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    @Test
    void keepsBlankReferenceAsBlankCredential() {
        EnvironmentCredentialResolver resolver = new EnvironmentCredentialResolver(Map.of());

        String credential = resolver.resolve("");

        assertThat(credential).isBlank();
    }
}
