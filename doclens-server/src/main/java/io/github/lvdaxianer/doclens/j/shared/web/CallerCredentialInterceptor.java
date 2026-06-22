package io.github.lvdaxianer.doclens.j.shared.web;

import io.github.lvdaxianer.doclens.j.ingestion.domain.CallerIdentity;
import io.github.lvdaxianer.doclens.j.ingestion.infrastructure.CallerCredentialResolver;
import io.github.lvdaxianer.doclens.j.ingestion.infrastructure.CallerCredentialResolver.ResolvedCallerCredential;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 为所有 API 请求解析 caller 分区键。
 *
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
@Component
public class CallerCredentialInterceptor implements HandlerInterceptor {

    /** Request attribute 中保存的 caller identity 键。 */
    public static final String CALLER_IDENTITY_ATTRIBUTE = CallerCredentialInterceptor.class.getName()
            + ".callerIdentity";
    /** Request attribute 中保存的 caller 分区键解析结果键。 */
    public static final String RESOLVED_CREDENTIAL_ATTRIBUTE = CallerCredentialInterceptor.class.getName()
            + ".resolvedCredential";

    private static final String CALLER_PARTITION_HEADER = "X-Doclens-Key";

    private final CallerCredentialResolver callerCredentialResolver;

    /**
     * 创建 caller 分区键拦截器。
     *
     * @param callerCredentialResolver caller 分区键解析器
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
        ResolvedCallerCredential resolvedCredential = callerCredentialResolver.resolveWithCredential(
                request.getHeader(CALLER_PARTITION_HEADER), "");
        CallerIdentity caller = resolvedCredential.callerIdentity();
        request.setAttribute(CALLER_IDENTITY_ATTRIBUTE, caller);
        request.setAttribute(RESOLVED_CREDENTIAL_ATTRIBUTE, resolvedCredential);
        return true;
    }
}
