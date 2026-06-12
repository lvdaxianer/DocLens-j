package io.github.lvdaxianer.doclens.j.integration.interfaces;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Open WebUI 内部调用鉴权守卫。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
@Component
public class OpenWebuiAuthGuard {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String USER_ID_HEADER = "X-OpenWebUI-User-Id";
    private static final String USER_EMAIL_HEADER = "X-OpenWebUI-User-Email";
    private static final String USER_ROLE_HEADER = "X-OpenWebUI-User-Role";
    private static final String REQUEST_ID_HEADER = "X-OpenWebUI-Request-Id";
    private static final String BEARER_PREFIX = "Bearer ";

    private final OpenWebuiIntegrationProperties properties;

    /**
     * 创建 Open WebUI 鉴权守卫。
     *
     * @param properties Open WebUI 集成配置
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public OpenWebuiAuthGuard(OpenWebuiIntegrationProperties properties) {
        this.properties = properties;
    }

    /**
     * 校验内部 token 与必需身份头。
     *
     * @param request HTTP 请求
     * @return Open WebUI 请求身份
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public OpenWebuiIdentity requireIdentity(HttpServletRequest request) {
        requireToken(request);
        String userId = requiredHeader(request, USER_ID_HEADER);
        String requestId = requiredHeader(request, REQUEST_ID_HEADER);
        return new OpenWebuiIdentity(userId, request.getHeader(USER_EMAIL_HEADER),
                request.getHeader(USER_ROLE_HEADER), requestId);
    }

    /**
     * 校验内部 Bearer token。
     *
     * @param request HTTP 请求
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private void requireToken(HttpServletRequest request) {
        String configuredToken = properties.getInternalToken();
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (!StringUtils.hasText(configuredToken) || !StringUtils.hasText(authorization)
                || !authorization.equals(BEARER_PREFIX + configuredToken)) {
            throw OpenWebuiIntegrationException.unauthorized();
        } else {
            // token 已匹配，继续校验身份头。
        }
    }

    /**
     * 获取必填请求头。
     *
     * @param request HTTP 请求
     * @param name 请求头名称
     * @return 请求头值
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private String requiredHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (StringUtils.hasText(value)) {
            return value;
        } else {
            throw OpenWebuiIntegrationException.badRequest("missing required header: " + name);
        }
    }
}
