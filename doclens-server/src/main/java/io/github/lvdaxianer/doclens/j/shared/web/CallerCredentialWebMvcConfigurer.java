package io.github.lvdaxianer.doclens.j.shared.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 为 API 路由统一注册 caller 凭证拦截器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
@Configuration
public class CallerCredentialWebMvcConfigurer implements WebMvcConfigurer {

    private final GlobalProtectionInterceptor globalProtectionInterceptor;
    private final CallerCredentialInterceptor callerCredentialInterceptor;
    private final CallerTrafficInterceptor callerTrafficInterceptor;

    /**
     * 创建 Web MVC 配置。
     *
     * @param callerCredentialInterceptor caller 凭证拦截器
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public CallerCredentialWebMvcConfigurer(
            GlobalProtectionInterceptor globalProtectionInterceptor,
            CallerCredentialInterceptor callerCredentialInterceptor,
            CallerTrafficInterceptor callerTrafficInterceptor
    ) {
        this.globalProtectionInterceptor = globalProtectionInterceptor;
        this.callerCredentialInterceptor = callerCredentialInterceptor;
        this.callerTrafficInterceptor = callerTrafficInterceptor;
    }

    /**
     * 注册需要 caller 凭证的 API 拦截器。
     *
     * @param registry 拦截器注册表
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(globalProtectionInterceptor).addPathPatterns("/api/v1/**");
        registry.addInterceptor(callerCredentialInterceptor).addPathPatterns("/api/v1/**");
        registry.addInterceptor(callerTrafficInterceptor).addPathPatterns("/api/v1/**");
    }
}
