package io.github.lvdaxianer.doclens.j.shared.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lvdaxianer.doclens.j.autoconfigure.GatewayAuthProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * 可信网关拦截器测试。
 *
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
class TrustedGatewayInterceptorTest {

    /** 可信网关证明头名称。 */
    private static final String GATEWAY_SECRET_HEADER = "X-Doclens-Gateway-Secret";
    /** 可信网关证明头值。 */
    private static final String GATEWAY_SECRET_VALUE = "local-secret";
    /** principal 请求头名称。 */
    private static final String PRINCIPAL_HEADER = "X-Doclens-Principal";
    /** roles 请求头名称。 */
    private static final String ROLES_HEADER = "X-Doclens-Roles";
    /** 普通用户角色。 */
    private static final String USER_ROLE = "user";
    /** 管理员角色。 */
    private static final String ADMIN_ROLE = "admin";
    /** 测试 principal。 */
    private static final String ALICE_PRINCIPAL = "alice";
    /** 测试管理员 principal。 */
    private static final String ADMIN_PRINCIPAL = "ops-admin";
    /** Alice 允许访问的分区。 */
    private static final String ALICE_PARTITION = "alice-workspace";
    /** Alice 不允许访问的分区。 */
    private static final String BOB_PARTITION = "bob-workspace";
    /** admin 策略覆盖的测试路由。 */
    private static final String ADMIN_ROUTE = "/api/v1/ocr-nodes/node-1";
    /** 缺失网关证明错误消息。 */
    private static final String MISSING_GATEWAY_PROOF_MESSAGE = "trusted gateway proof is missing or invalid";
    /** 缺失 principal 错误消息。 */
    private static final String MISSING_PRINCIPAL_MESSAGE = "trusted principal is missing";
    /** principal 分区授权失败错误消息。 */
    private static final String PARTITION_DENIED_MESSAGE = "trusted principal is not allowed for partition";
    /** principal 角色授权失败错误消息。 */
    private static final String ROLE_DENIED_MESSAGE = "trusted principal does not have required role";

    /**
     * 缺少可信网关证明时应拒绝请求。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    @Test
    void rejectsRequestWhenGatewayProofIsMissing() {
        TrustedGatewayInterceptor interceptor = interceptor();

        assertThatThrownBy(() -> interceptor.preHandle(request(), new MockHttpServletResponse(), new Object()))
                .isInstanceOf(TrustedGatewayException.class)
                .hasMessage(MISSING_GATEWAY_PROOF_MESSAGE);
    }

    /**
     * 缺少 principal 请求头时应拒绝请求。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    @Test
    void rejectsRequestWhenPrincipalHeaderIsMissing() {
        TrustedGatewayInterceptor interceptor = interceptor();
        MockHttpServletRequest request = request();
        request.addHeader(GATEWAY_SECRET_HEADER, GATEWAY_SECRET_VALUE);

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(TrustedGatewayException.class)
                .hasMessage(MISSING_PRINCIPAL_MESSAGE);
    }

    /**
     * 校验通过时应写入可信 principal 上下文。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    @Test
    void storesTrustedPrincipalWhenGatewayProofAndPrincipalArePresent() {
        TrustedGatewayInterceptor interceptor = interceptor();
        MockHttpServletRequest request = request();
        request.addHeader(GATEWAY_SECRET_HEADER, GATEWAY_SECRET_VALUE);
        request.addHeader(PRINCIPAL_HEADER, ALICE_PRINCIPAL);
        request.addHeader(ROLES_HEADER, USER_ROLE + "," + ADMIN_ROLE);

        boolean shouldContinue = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(shouldContinue).isTrue();
        assertThat(request.getAttribute(TrustedGatewayPrincipal.REQUEST_ATTRIBUTE))
                .isEqualTo(new TrustedGatewayPrincipal(ALICE_PRINCIPAL, List.of(USER_ROLE, ADMIN_ROLE)));
    }

    /**
     * principal 访问已授权分区时应继续处理请求。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    @Test
    void continuesRequestWhenPrincipalIsAllowedForPartition() {
        TrustedGatewayInterceptor interceptor = interceptor();
        MockHttpServletRequest request = request();
        request.addHeader(GATEWAY_SECRET_HEADER, GATEWAY_SECRET_VALUE);
        request.addHeader(PRINCIPAL_HEADER, ALICE_PRINCIPAL);
        request.addHeader(CallerPartitionInterceptor.headerName(), ALICE_PARTITION);

        boolean shouldContinue = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(shouldContinue).isTrue();
        assertThat(request.getAttribute(TrustedGatewayPrincipal.REQUEST_ATTRIBUTE))
                .isEqualTo(new TrustedGatewayPrincipal(ALICE_PRINCIPAL, List.of()));
    }

    /**
     * roles 请求头缺失时应写入空角色集合。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    @Test
    void storesEmptyRolesWhenRolesHeaderIsMissing() {
        TrustedGatewayInterceptor interceptor = interceptor();
        MockHttpServletRequest request = request();
        request.addHeader(GATEWAY_SECRET_HEADER, GATEWAY_SECRET_VALUE);
        request.addHeader(PRINCIPAL_HEADER, ALICE_PRINCIPAL);

        boolean shouldContinue = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(shouldContinue).isTrue();
        assertThat(request.getAttribute(TrustedGatewayPrincipal.REQUEST_ATTRIBUTE))
                .isEqualTo(new TrustedGatewayPrincipal(ALICE_PRINCIPAL, List.of()));
    }

    /**
     * principal 访问未授权分区时应拒绝请求。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    @Test
    void rejectsRequestWhenPrincipalIsNotAllowedForPartition() {
        TrustedGatewayInterceptor interceptor = interceptor();
        MockHttpServletRequest request = request();
        request.addHeader(GATEWAY_SECRET_HEADER, GATEWAY_SECRET_VALUE);
        request.addHeader(PRINCIPAL_HEADER, ALICE_PRINCIPAL);
        request.addHeader(CallerPartitionInterceptor.headerName(), BOB_PARTITION);

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(TrustedGatewayAuthorizationException.class)
                .hasMessage(PARTITION_DENIED_MESSAGE);
    }

    /**
     * 普通 principal 访问 admin 路由时应拒绝请求。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    @Test
    void rejectsAdminRouteWhenPrincipalDoesNotHaveAdminRole() {
        TrustedGatewayInterceptor interceptor = interceptor();
        MockHttpServletRequest request = request(ADMIN_ROUTE);
        request.addHeader(GATEWAY_SECRET_HEADER, GATEWAY_SECRET_VALUE);
        request.addHeader(PRINCIPAL_HEADER, ALICE_PRINCIPAL);
        request.addHeader(CallerPartitionInterceptor.headerName(), ALICE_PARTITION);

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(TrustedGatewayAuthorizationException.class)
                .hasMessage(ROLE_DENIED_MESSAGE);
    }

    /**
     * admin principal 访问 admin 路由时应继续处理请求。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    @Test
    void continuesAdminRouteWhenPrincipalHasAdminRole() {
        TrustedGatewayInterceptor interceptor = interceptor();
        MockHttpServletRequest request = request(ADMIN_ROUTE);
        request.addHeader(GATEWAY_SECRET_HEADER, GATEWAY_SECRET_VALUE);
        request.addHeader(PRINCIPAL_HEADER, ADMIN_PRINCIPAL);
        request.addHeader(CallerPartitionInterceptor.headerName(), ALICE_PARTITION);

        boolean shouldContinue = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(shouldContinue).isTrue();
    }

    /**
     * 创建可信网关拦截器。
     *
     * @return 可信网关拦截器
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    private TrustedGatewayInterceptor interceptor() {
        return new TrustedGatewayInterceptor(gatewayAuth());
    }

    /**
     * 创建启用状态的可信网关认证配置。
     *
     * @return 可信网关认证配置
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    private GatewayAuthProperties gatewayAuth() {
        return new GatewayAuthProperties(true,
                new GatewayAuthProperties.TrustedGatewayProperties(GATEWAY_SECRET_HEADER, GATEWAY_SECRET_VALUE),
                new GatewayAuthProperties.PrincipalHeadersProperties(PRINCIPAL_HEADER, ROLES_HEADER),
                List.of(new GatewayAuthProperties.PrincipalProperties(ALICE_PRINCIPAL, List.of(USER_ROLE),
                        List.of(ALICE_PARTITION)),
                        new GatewayAuthProperties.PrincipalProperties(ADMIN_PRINCIPAL, List.of(USER_ROLE, ADMIN_ROLE),
                                List.of(ALICE_PARTITION))),
                GatewayAuthProperties.RoutePolicyProperties.defaults());
    }

    /**
     * 创建测试请求。
     *
     * @return 测试请求
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    private MockHttpServletRequest request() {
        return request("/api/v1/batches");
    }

    /**
     * 创建指定路径的测试请求。
     *
     * @param requestUri 请求路径
     * @return 测试请求
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    private MockHttpServletRequest request(String requestUri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(requestUri);
        return request;
    }
}
