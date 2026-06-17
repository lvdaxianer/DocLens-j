package io.github.lvdaxianer.doclens.j.shared.web;

import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensSpringProperties;
import java.util.concurrent.Semaphore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 全局并发保护拦截器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
@Component
public class GlobalProtectionInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(GlobalProtectionInterceptor.class);
    private static final String GLOBAL_PROTECTION_ATTRIBUTE = GlobalProtectionInterceptor.class.getName() + ".permit";
    private static final String GLOBAL_PROTECTION_MESSAGE = "global protection limit exceeded";
    private static final String GLOBAL_PROTECTION_HEADER = "X-DocLens-Global-Protection";

    private final DocLensSpringProperties properties;
    private final Semaphore semaphore;

    /**
     * 创建全局保护拦截器。
     *
     * @param properties Spring 绑定配置
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public GlobalProtectionInterceptor(DocLensSpringProperties properties) {
        this.properties = properties;
        this.semaphore = new Semaphore(Math.max(0, properties.traffic().globalProtection().maxInFlight()));
    }

    /**
     * 在请求执行前申请全局并发许可。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param handler 当前处理器
     * @return 是否继续执行
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!properties.traffic().globalProtection().enabled()) {
            return true;
        }
        if (semaphore.tryAcquire()) {
            request.setAttribute(GLOBAL_PROTECTION_ATTRIBUTE, Boolean.TRUE);
            return true;
        } else {
            log.warn("[流量保护] 全局并发超限, path={}, maxInFlight={}", request.getRequestURI(),
                    properties.traffic().globalProtection().maxInFlight());
            throw new GlobalProtectionExceededException(GLOBAL_PROTECTION_MESSAGE);
        }
    }

    /**
     * 请求结束后释放全局并发许可。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param handler 当前处理器
     * @param ex 处理异常
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (Boolean.TRUE.equals(request.getAttribute(GLOBAL_PROTECTION_ATTRIBUTE))) {
            semaphore.release();
            request.removeAttribute(GLOBAL_PROTECTION_ATTRIBUTE);
        }
    }

    /**
     * 返回全局保护头名称。
     *
     * @return header 名称
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    static String headerName() {
        return GLOBAL_PROTECTION_HEADER;
    }
}
