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

    private static final String MISSING_CALLER_MESSAGE = "missing caller partition key";

    /**
     * 读取当前请求的 caller 身份。
     *
     * @param request HTTP 请求
     * @return caller 身份
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public CallerIdentity resolve(HttpServletRequest request) {
        Object caller = request.getAttribute(CallerPartitionInterceptor.CALLER_IDENTITY_ATTRIBUTE);
        if (caller instanceof CallerIdentity callerIdentity) {
            // 拦截器已完成分区键解析时直接复用当前请求身份。
            return callerIdentity;
        } else {
            // 缺少拦截器结果时拒绝请求，避免落回匿名共享空间。
            throw new CallerCredentialException(MISSING_CALLER_MESSAGE);
        }
    }
}
