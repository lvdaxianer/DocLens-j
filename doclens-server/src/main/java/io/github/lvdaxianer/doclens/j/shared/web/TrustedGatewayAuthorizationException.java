package io.github.lvdaxianer.doclens.j.shared.web;

/**
 * 可信网关 principal 授权失败异常。
 *
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
public class TrustedGatewayAuthorizationException extends RuntimeException {

    /**
     * 创建可信网关 principal 授权失败异常。
     *
     * @param message 错误消息
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    public TrustedGatewayAuthorizationException(String message) {
        super(message);
    }
}
