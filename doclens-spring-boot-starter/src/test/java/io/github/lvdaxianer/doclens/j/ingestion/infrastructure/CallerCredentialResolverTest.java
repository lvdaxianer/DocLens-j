package io.github.lvdaxianer.doclens.j.ingestion.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensSpringProperties.ClientsProperties;
import io.github.lvdaxianer.doclens.j.ingestion.domain.CallerIdentity;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 接入方凭证解析器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
class CallerCredentialResolverTest {

    /** 测试 caller 分区键。 */
    private static final String PARTITION_KEY = "tenant-east";
    /** 默认来源应用。 */
    private static final String SOURCE_APP = "dashboard";

    /**
     * 验证空 caller 分区键会被拒绝，避免落入匿名共享空间。
     *
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void rejectsBlankCallerPartitionKey() {
        assertThatThrownBy(() -> new CallerCredentialResolver(new ClientsProperties(List.of()))
                .resolve("  ", ""))
                .isInstanceOf(CallerCredentialException.class)
                .hasMessage("missing caller partition key");
    }

    /**
     * 验证任意非空 caller 分区键都能解析为隔离身份。
     *
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void resolvesAnyNonBlankRawKeyAsPartitionKey() {
        CallerIdentity caller = new CallerCredentialResolver(new ClientsProperties(List.of()))
                .resolve(PARTITION_KEY, "");

        assertThat(caller.toMap())
                .containsEntry(CallerIdentity.CLIENT_ID_FIELD, PARTITION_KEY)
                .containsEntry(CallerIdentity.SOURCE_APP_FIELD, SOURCE_APP)
                .containsEntry(CallerIdentity.TENANT_KEY_FIELD, PARTITION_KEY);
    }

    /**
     * 验证 caller 分区键会被清理空白字符后使用。
     *
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void trimsCallerPartitionKey() {
        CallerIdentity caller = new CallerCredentialResolver(new ClientsProperties(List.of()))
                .resolve("  " + PARTITION_KEY + "  ", "");

        assertThat(caller.clientId()).isEqualTo(PARTITION_KEY);
        assertThat(caller.tenantKey()).contains(PARTITION_KEY);
    }
}
