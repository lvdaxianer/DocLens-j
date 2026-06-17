package io.github.lvdaxianer.doclens.j.shared.web;

import io.github.lvdaxianer.doclens.j.ingestion.domain.CallerIdentity;
import io.github.lvdaxianer.doclens.j.ingestion.infrastructure.CallerCredentialException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * 从当前 HTTP 请求读取已解析 caller 身份。
 *
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
@Component
public class CallerIdentityRequestResolver {

    private static final String MISSING_CALLER_MESSAGE = "unauthorized caller credential";

    /**
     * 读取当前请求的 caller 身份。
     *
     * @param request HTTP 请求
     * @return caller 身份
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public CallerIdentity resolve(HttpServletRequest request) {
        Object caller = request.getAttribute(CallerCredentialInterceptor.CALLER_IDENTITY_ATTRIBUTE);
        if (caller instanceof CallerIdentity callerIdentity) {
            // 拦截器已完成凭证解析时直接复用可信身份。
            return callerIdentity;
        } else {
            // 缺少拦截器结果时按未授权处理，避免落回匿名访问。
            throw new CallerCredentialException(MISSING_CALLER_MESSAGE);
        }
    }
}
