package io.github.lvdaxianer.doclens.j.integration.interfaces;

import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Open WebUI 集成适配器契约异常。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
public class OpenWebuiIntegrationException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final Map<String, Object> details;

    /**
     * 创建 Open WebUI 适配器契约异常。
     *
     * @param status HTTP 状态
     * @param code 契约错误码
     * @param message 契约错误消息
     * @param details 错误详情
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public OpenWebuiIntegrationException(
            HttpStatus status,
            String code,
            String message,
            Map<String, Object> details
    ) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details == null ? Map.of() : Map.copyOf(details);
    }

    /**
     * 创建未授权异常。
     *
     * @return 未授权异常
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public static OpenWebuiIntegrationException unauthorized() {
        return new OpenWebuiIntegrationException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED_INTERNAL_CALLER",
                "unauthorized internal caller", Map.of());
    }

    /**
     * 创建请求校验异常。
     *
     * @param message 错误消息
     * @return 请求校验异常
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public static OpenWebuiIntegrationException badRequest(String message) {
        return new OpenWebuiIntegrationException(HttpStatus.BAD_REQUEST, "INVALID_OPENWEBUI_REQUEST",
                message, Map.of());
    }

    /**
     * 创建保留异常链的请求校验异常。
     *
     * @param message 错误消息
     * @param cause 原始异常
     * @return 请求校验异常
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public static OpenWebuiIntegrationException badRequest(String message, Throwable cause) {
        OpenWebuiIntegrationException exception = badRequest(message);
        exception.initCause(cause);
        return exception;
    }

    /**
     * 获取 HTTP 状态。
     *
     * @return HTTP 状态
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public HttpStatus status() {
        return status;
    }

    /**
     * 获取契约错误码。
     *
     * @return 契约错误码
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public String code() {
        return code;
    }

    /**
     * 获取错误详情。
     *
     * @return 错误详情
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public Map<String, Object> details() {
        return details;
    }
}
