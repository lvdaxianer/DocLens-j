package io.github.lvdaxianer.doclens.j.shared.web;

import java.util.List;

/**
 * 可信网关注入的 principal 上下文。
 *
 * @param principal principal 名称
 * @param roles principal 角色集合
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
public record TrustedGatewayPrincipal(String principal, List<String> roles) {

    /** Request attribute 中保存可信 principal 的键。 */
    public static final String REQUEST_ATTRIBUTE = TrustedGatewayPrincipal.class.getName() + ".principal";

    /**
     * 规整可信 principal 上下文。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    public TrustedGatewayPrincipal {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
