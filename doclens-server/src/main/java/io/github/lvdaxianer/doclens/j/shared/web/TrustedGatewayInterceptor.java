package io.github.lvdaxianer.doclens.j.shared.web;

import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensSpringProperties;
import io.github.lvdaxianer.doclens.j.autoconfigure.GatewayAuthProperties;
import io.github.lvdaxianer.doclens.j.autoconfigure.GatewayAuthProperties.PrincipalProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 校验请求是否来自可信网关并提取 principal。
 *
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
@Component
public class TrustedGatewayInterceptor implements HandlerInterceptor {

    private static final String MISSING_GATEWAY_PROOF_MESSAGE = "trusted gateway proof is missing or invalid";
    private static final String MISSING_PRINCIPAL_MESSAGE = "trusted principal is missing";
    private static final String PARTITION_DENIED_MESSAGE = "trusted principal is not allowed for partition";
    private static final String ROLE_DELIMITER = ",";

    private final GatewayAuthProperties gatewayAuth;

    /**
     * 创建可信网关拦截器。
     *
     * @param properties Spring 绑定配置
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    @Autowired
    public TrustedGatewayInterceptor(DocLensSpringProperties properties) {
        this(properties.gatewayAuth());
    }

    /**
     * 创建可信网关拦截器。
     *
     * @param gatewayAuth 可信网关认证配置
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    TrustedGatewayInterceptor(GatewayAuthProperties gatewayAuth) {
        this.gatewayAuth = gatewayAuth;
    }

    /**
     * 在请求进入控制器前校验可信网关证明并提取 principal。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param handler 当前处理器
     * @return 是否继续处理
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 未启用可信网关认证时保持历史行为，不写入 principal 上下文。
        if (!gatewayAuth.enabled()) {
            return true;
        }
        validateGatewayProof(request);
        String principal = principalHeader(request);
        validatePartitionAccess(request, principal);
        request.setAttribute(TrustedGatewayPrincipal.REQUEST_ATTRIBUTE,
                new TrustedGatewayPrincipal(principal, rolesHeader(request)));
        return true;
    }

    /**
     * 校验请求携带的可信网关证明。
     *
     * @param request HTTP 请求
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    private void validateGatewayProof(HttpServletRequest request) {
        String actualProof = request.getHeader(gatewayAuth.trustedGateway().headerName());
        // 网关证明缺失或不匹配时拒绝请求，但不回显密钥值。
        if (!gatewayAuth.trustedGateway().headerValue().equals(actualProof)) {
            throw new TrustedGatewayException(MISSING_GATEWAY_PROOF_MESSAGE);
        }
    }

    /**
     * 读取并校验 principal 请求头。
     *
     * @param request HTTP 请求
     * @return principal 名称
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    private String principalHeader(HttpServletRequest request) {
        String principal = request.getHeader(gatewayAuth.principalHeaders().principal());
        // principal 缺失时拒绝请求，分区授权留给后续任务处理。
        if (!StringUtils.hasText(principal)) {
            throw new TrustedGatewayException(MISSING_PRINCIPAL_MESSAGE);
        }
        return principal;
    }

    /**
     * 校验 principal 是否允许访问请求分区。
     *
     * @param request HTTP 请求
     * @param principal principal 名称
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    private void validatePartitionAccess(HttpServletRequest request, String principal) {
        String partition = request.getHeader(CallerPartitionInterceptor.headerName());
        // 分区键缺失仍交给 caller 分区拦截器输出原有缺失分区错误。
        if (!StringUtils.hasText(partition)) {
            return;
        }
        // principal 未配置或未授权当前分区时拒绝请求。
        if (!isPartitionAllowed(principal, partition)) {
            throw new TrustedGatewayAuthorizationException(PARTITION_DENIED_MESSAGE);
        }
    }

    /**
     * 判断 principal 是否允许访问指定分区。
     *
     * @param principal principal 名称
     * @param partition caller 分区键
     * @return 是否允许访问
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    private boolean isPartitionAllowed(String principal, String partition) {
        return trustedPrincipal(principal)
                .map(properties -> properties.allowedPartitions().contains(partition))
                .orElse(false);
    }

    /**
     * 查找配置中的可信 principal。
     *
     * @param principal principal 名称
     * @return 可信 principal 配置
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    private Optional<PrincipalProperties> trustedPrincipal(String principal) {
        return gatewayAuth.principals().stream()
                .filter(properties -> properties.principal().equals(principal))
                .findFirst();
    }

    /**
     * 读取 roles 请求头。
     *
     * @param request HTTP 请求
     * @return principal 角色集合
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    private List<String> rolesHeader(HttpServletRequest request) {
        String roles = request.getHeader(gatewayAuth.principalHeaders().roles());
        // roles 是可选上下文，缺失时明确使用空集合。
        if (!StringUtils.hasText(roles)) {
            return List.of();
        }
        return Arrays.stream(StringUtils.delimitedListToStringArray(roles, ROLE_DELIMITER))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}
