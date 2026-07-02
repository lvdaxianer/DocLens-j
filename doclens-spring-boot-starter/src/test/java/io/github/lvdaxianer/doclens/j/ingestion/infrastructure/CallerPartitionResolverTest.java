package io.github.lvdaxianer.doclens.j.ingestion.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensSpringProperties.ClientsProperties;
import io.github.lvdaxianer.doclens.j.ingestion.domain.CallerIdentity;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * caller 分区键解析器测试。
 *
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
class CallerPartitionResolverTest {

    /** 测试分区键。 */
    private static final String PARTITION_KEY = "tenant-east";
    /** 默认来源应用。 */
    private static final String SOURCE_APP = "dashboard";

    /**
     * 验证新分区解析器不会把分区键当作认证凭证处理。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    @Test
    void resolvesPartitionKeyWithoutCredentialLookup() {
        CallerIdentity caller = new CallerPartitionResolver(new ClientsProperties(List.of()))
                .resolve(PARTITION_KEY);

        assertThat(caller.toMap())
                .containsEntry(CallerIdentity.CLIENT_ID_FIELD, PARTITION_KEY)
                .containsEntry(CallerIdentity.SOURCE_APP_FIELD, SOURCE_APP)
                .containsEntry(CallerIdentity.TENANT_KEY_FIELD, PARTITION_KEY);
    }

    /**
     * 验证旧凭证解析入口仍委托到分区解析逻辑，保障调用方兼容。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    @Test
    void legacyCredentialResolverDelegatesToPartitionResolver() {
        CallerIdentity caller = new CallerCredentialResolver(new ClientsProperties(List.of()))
                .resolve(PARTITION_KEY, "");

        assertThat(caller.clientId()).isEqualTo(PARTITION_KEY);
        assertThat(caller.tenantKey()).contains(PARTITION_KEY);
    }
}
