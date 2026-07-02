package io.github.lvdaxianer.doclens.j.shared.web;

/**
 * 可信网关认证失败异常。
 *
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
public class TrustedGatewayException extends RuntimeException {

    /**
     * 创建可信网关认证失败异常。
     *
     * @param message 错误消息
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    public TrustedGatewayException(String message) {
        super(message);
    }
}
