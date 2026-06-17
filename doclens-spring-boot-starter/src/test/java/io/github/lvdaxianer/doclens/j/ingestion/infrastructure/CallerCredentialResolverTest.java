package io.github.lvdaxianer.doclens.j.ingestion.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensSpringProperties.CallerCredentialProperties;
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

    /** API Key 测试值。 */
    private static final String API_KEY = "api-secret";
    /** Bearer Token 测试值。 */
    private static final String BEARER_TOKEN = "bearer-secret";
    /** 测试接入方标识。 */
    private static final String CLIENT_ID = "rag-flow";
    /** 测试来源应用。 */
    private static final String SOURCE_APP = "knowledge-base";
    /** 测试租户键。 */
    private static final String TENANT_KEY = "tenant-east";
    /** Bearer 认证头测试值。 */
    private static final String AUTHORIZATION = "Bearer " + BEARER_TOKEN;

    /**
     * 验证未配置凭证时会拒绝访问，而不是回退到匿名调用方。
     *
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void rejectsMissingCredentialWhenNoCredentialsConfigured() {
        assertThatThrownBy(() -> new CallerCredentialResolver(new ClientsProperties(List.of()))
                .resolve("", ""))
                .isInstanceOf(CallerCredentialException.class)
                .hasMessage("unauthorized caller credential");
    }

    /**
     * 验证 API Key 匹配时返回配置调用方。
     *
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void resolvesCallerByApiKeyCredential() {
        CallerIdentity caller = resolver().resolve(API_KEY, "");

        assertThat(caller.toMap())
                .containsEntry(CallerIdentity.CLIENT_ID_FIELD, CLIENT_ID)
                .containsEntry(CallerIdentity.SOURCE_APP_FIELD, SOURCE_APP)
                .containsEntry(CallerIdentity.TENANT_KEY_FIELD, TENANT_KEY);
    }

    /**
     * 验证 Bearer Token 匹配时返回配置调用方。
     *
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void resolvesCallerByBearerCredential() {
        CallerIdentity caller = resolver().resolve("", AUTHORIZATION);

        assertThat(caller.toMap())
                .containsEntry(CallerIdentity.CLIENT_ID_FIELD, CLIENT_ID)
                .containsEntry(CallerIdentity.SOURCE_APP_FIELD, SOURCE_APP)
                .containsEntry(CallerIdentity.TENANT_KEY_FIELD, TENANT_KEY);
    }

    /**
     * 验证配置凭证后缺失匹配凭证会拒绝调用。
     *
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void rejectsMissingCredentialWhenCredentialsConfigured() {
        assertThatThrownBy(() -> resolver().resolve("", ""))
                .isInstanceOf(CallerCredentialException.class)
                .hasMessage("unauthorized caller credential");
    }

    /**
     * 创建测试解析器。
     *
     * @return 接入方凭证解析器
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private CallerCredentialResolver resolver() {
        return new CallerCredentialResolver(new ClientsProperties(List.of(
                new CallerCredentialProperties(CLIENT_ID, SOURCE_APP, TENANT_KEY, API_KEY, BEARER_TOKEN)
        )));
    }
}
