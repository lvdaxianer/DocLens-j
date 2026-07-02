package io.github.lvdaxianer.doclens.j.autoconfigure;

import java.util.List;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.util.StringUtils;

/**
 * 可信网关认证配置属性。
 *
 * @param enabled 是否启用可信网关认证
 * @param trustedGateway 可信网关证明配置
 * @param principalHeaders principal 请求头配置
 * @param principals principal allowlist 配置
 * @param routePolicies 路由角色策略配置
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
public record GatewayAuthProperties(
        boolean enabled,
        TrustedGatewayProperties trustedGateway,
        PrincipalHeadersProperties principalHeaders,
        List<PrincipalProperties> principals,
        List<RoutePolicyProperties> routePolicies
) {
    private static final String DEFAULT_GATEWAY_SECRET_HEADER = "X-Doclens-Gateway-Secret";
    private static final String DEFAULT_PRINCIPAL_HEADER = "X-Doclens-Principal";
    private static final String DEFAULT_ROLES_HEADER = "X-Doclens-Roles";
    private static final String USER_ROLE = "user";
    private static final String ADMIN_ROLE = "admin";
    private static final String OCR_GOVERNANCE_CONFIG_ROUTE_PATTERN = "/api/v1/ocr-governance-config/**";
    private static final String LLM_MARKDOWN_CONFIG_ROUTE_PATTERN = "/api/v1/llm-markdown-config/**";
    private static final String OCR_MODELS_ROUTE_PATTERN = "/api/v1/ocr-models/**";
    private static final String OCR_NODES_ROUTE_PATTERN = "/api/v1/ocr-nodes/**";
    private static final String CALLBACK_JOB_RETRY_ROUTE_PATTERN = "/api/v1/dashboard/callback-jobs/*/retry";
    private static final String API_V1_ROUTE_PATTERN = "/api/v1/**";
    private static final String GATEWAY_HEADER_VALUE_REQUIRED_MESSAGE =
            "doclens.gateway-auth.trusted-gateway.header-value is required when gateway auth is enabled";
    private static final String GATEWAY_PRINCIPALS_REQUIRED_MESSAGE =
            "doclens.gateway-auth.principals is required when gateway auth is enabled";
    private static final String GATEWAY_PRINCIPAL_REQUIRED_MESSAGE_PREFIX =
            "doclens.gateway-auth.principals[";
    private static final String GATEWAY_PRINCIPAL_NAME_REQUIRED_MESSAGE_SUFFIX =
            "].principal is required when gateway auth is enabled";
    private static final String GATEWAY_PRINCIPAL_PARTITIONS_REQUIRED_MESSAGE_SUFFIX =
            "].allowed-partitions is required when gateway auth is enabled";

    /**
     * 规整可信网关认证属性。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    @ConstructorBinding
    public GatewayAuthProperties {
        trustedGateway = trustedGateway == null ? TrustedGatewayProperties.defaults() : trustedGateway;
        principalHeaders = principalHeaders == null ? PrincipalHeadersProperties.defaults() : principalHeaders;
        principals = principals == null ? List.of() : List.copyOf(principals);
        routePolicies = routePolicies == null ? RoutePolicyProperties.defaults() : List.copyOf(routePolicies);
        // 启用可信网关认证时执行启动期 fail-fast 校验。
        if (enabled) {
            validateGatewayAuth(trustedGateway, principals);
        }
    }

    /**
     * 创建默认可信网关认证属性。
     *
     * @return 默认可信网关认证属性
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    public static GatewayAuthProperties defaults() {
        return new GatewayAuthProperties(false, TrustedGatewayProperties.defaults(),
                PrincipalHeadersProperties.defaults(), List.of(), RoutePolicyProperties.defaults());
    }

    /**
     * 校验启用状态下的可信网关认证配置。
     *
     * @param trustedGateway 可信网关证明配置
     * @param principals principal allowlist 配置
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    private static void validateGatewayAuth(TrustedGatewayProperties trustedGateway,
                                            List<PrincipalProperties> principals) {
        // 启用可信网关认证时，网关共享密钥必须显式配置。
        if (!StringUtils.hasText(trustedGateway.headerValue())) {
            throw new IllegalArgumentException(GATEWAY_HEADER_VALUE_REQUIRED_MESSAGE);
        }
        // 启用可信网关认证时，必须配置可信 principal allowlist。
        if (principals.isEmpty()) {
            throw new IllegalArgumentException(GATEWAY_PRINCIPALS_REQUIRED_MESSAGE);
        }
        validateGatewayPrincipals(principals);
    }

    /**
     * 校验 principal allowlist 中每个条目的必填字段。
     *
     * @param principals principal allowlist 配置
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    private static void validateGatewayPrincipals(List<PrincipalProperties> principals) {
        for (int principalIndex = 0; principalIndex < principals.size(); principalIndex++) {
            validateGatewayPrincipal(principals.get(principalIndex), principalIndex);
        }
    }

    /**
     * 校验单个 principal 条目的名称与分区授权。
     *
     * @param principal principal 授权配置
     * @param principalIndex principal 配置下标
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    private static void validateGatewayPrincipal(PrincipalProperties principal, int principalIndex) {
        // principal 名称是网关注入身份与 allowlist 匹配的必填键。
        if (!StringUtils.hasText(principal.principal())) {
            throw new IllegalArgumentException(principalRequiredMessage(principalIndex,
                    GATEWAY_PRINCIPAL_NAME_REQUIRED_MESSAGE_SUFFIX));
        }
        // allowed-partitions 是后续分区授权校验的必填边界。
        if (principal.allowedPartitions().isEmpty()) {
            throw new IllegalArgumentException(principalRequiredMessage(principalIndex,
                    GATEWAY_PRINCIPAL_PARTITIONS_REQUIRED_MESSAGE_SUFFIX));
        }
    }

    /**
     * 创建 principal 配置项必填错误消息。
     *
     * @param principalIndex principal 配置下标
     * @param messageSuffix 错误消息后缀
     * @return principal 配置项必填错误消息
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    private static String principalRequiredMessage(int principalIndex, String messageSuffix) {
        return GATEWAY_PRINCIPAL_REQUIRED_MESSAGE_PREFIX + principalIndex + messageSuffix;
    }

    /**
     * 可信网关证明请求头属性。
     *
     * @param headerName 可信网关证明请求头名称
     * @param headerValue 可信网关共享密钥值
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    public record TrustedGatewayProperties(String headerName, String headerValue) {

        /**
         * 规整可信网关证明请求头属性。
         *
         * @author lvdaxianer@yeah.net
         * @date 2026-07-02
         */
        public TrustedGatewayProperties {
            headerName = StringUtils.hasText(headerName) ? headerName : DEFAULT_GATEWAY_SECRET_HEADER;
            headerValue = headerValue == null ? "" : headerValue;
        }

        /**
         * 创建默认可信网关证明请求头属性。
         *
         * @return 默认可信网关证明请求头属性
         * @author lvdaxianer@yeah.net
         * @date 2026-07-02
         */
        public static TrustedGatewayProperties defaults() {
            return new TrustedGatewayProperties(DEFAULT_GATEWAY_SECRET_HEADER, "");
        }
    }

    /**
     * principal 请求头属性。
     *
     * @param principal principal 请求头名称
     * @param roles roles 请求头名称
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    public record PrincipalHeadersProperties(String principal, String roles) {

        /**
         * 规整 principal 请求头属性。
         *
         * @author lvdaxianer@yeah.net
         * @date 2026-07-02
         */
        public PrincipalHeadersProperties {
            principal = StringUtils.hasText(principal) ? principal : DEFAULT_PRINCIPAL_HEADER;
            roles = StringUtils.hasText(roles) ? roles : DEFAULT_ROLES_HEADER;
        }

        /**
         * 创建默认 principal 请求头属性。
         *
         * @return 默认 principal 请求头属性
         * @author lvdaxianer@yeah.net
         * @date 2026-07-02
         */
        public static PrincipalHeadersProperties defaults() {
            return new PrincipalHeadersProperties(DEFAULT_PRINCIPAL_HEADER, DEFAULT_ROLES_HEADER);
        }
    }

    /**
     * principal 授权属性。
     *
     * @param principal 可信 principal 名称
     * @param roles principal 角色集合
     * @param allowedPartitions 允许访问的数据分区集合
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    public record PrincipalProperties(String principal, List<String> roles, List<String> allowedPartitions) {

        /**
         * 规整 principal 授权属性。
         *
         * @author lvdaxianer@yeah.net
         * @date 2026-07-02
         */
        public PrincipalProperties {
            roles = roles == null ? List.of() : List.copyOf(roles);
            allowedPartitions = allowedPartitions == null ? List.of() : List.copyOf(allowedPartitions);
        }
    }

    /**
     * 路由角色策略属性。
     *
     * @param pattern 路由匹配模式
     * @param requiredRole 访问该路由需要的角色
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    public record RoutePolicyProperties(String pattern, String requiredRole) {

        /**
         * 创建默认路由角色策略。
         *
         * @return 默认路由角色策略
         * @author lvdaxianer@yeah.net
         * @date 2026-07-02
         */
        public static List<RoutePolicyProperties> defaults() {
            return List.of(
                    new RoutePolicyProperties(OCR_GOVERNANCE_CONFIG_ROUTE_PATTERN, ADMIN_ROLE),
                    new RoutePolicyProperties(LLM_MARKDOWN_CONFIG_ROUTE_PATTERN, ADMIN_ROLE),
                    new RoutePolicyProperties(OCR_MODELS_ROUTE_PATTERN, ADMIN_ROLE),
                    new RoutePolicyProperties(OCR_NODES_ROUTE_PATTERN, ADMIN_ROLE),
                    new RoutePolicyProperties(CALLBACK_JOB_RETRY_ROUTE_PATTERN, ADMIN_ROLE),
                    new RoutePolicyProperties(API_V1_ROUTE_PATTERN, USER_ROLE)
            );
        }
    }
}
