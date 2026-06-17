package io.github.lvdaxianer.doclens.j.shared.web;

import io.github.lvdaxianer.doclens.j.ingestion.domain.CallerIdentity;
import io.github.lvdaxianer.doclens.j.ingestion.infrastructure.CallerCredentialResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 为所有 API 请求解析并校验 caller 凭证。
 *
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
@Component
public class CallerCredentialInterceptor implements HandlerInterceptor {

    /** Request attribute 中保存的 caller identity 键。 */
    public static final String CALLER_IDENTITY_ATTRIBUTE = CallerCredentialInterceptor.class.getName()
            + ".callerIdentity";

    private static final String API_KEY_HEADER = "X-DocLens-Api-Key";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final CallerCredentialResolver callerCredentialResolver;

    /**
     * 创建 caller 凭证拦截器。
     *
     * @param callerCredentialResolver caller 凭证解析器
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public CallerCredentialInterceptor(CallerCredentialResolver callerCredentialResolver) {
        this.callerCredentialResolver = callerCredentialResolver;
    }

    /**
     * 在请求进入控制器前解析 caller 并写入 request attribute。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param handler 当前处理器
     * @return 是否继续处理
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        CallerIdentity caller = callerCredentialResolver.resolve(request.getHeader(API_KEY_HEADER),
                request.getHeader(AUTHORIZATION_HEADER));
        request.setAttribute(CALLER_IDENTITY_ATTRIBUTE, caller);
        return true;
    }
}
