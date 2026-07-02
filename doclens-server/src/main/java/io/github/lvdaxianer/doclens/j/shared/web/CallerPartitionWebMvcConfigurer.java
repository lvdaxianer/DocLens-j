package io.github.lvdaxianer.doclens.j.shared.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 为 API 路由统一注册 caller 分区拦截器。
 *
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
@Configuration
public class CallerPartitionWebMvcConfigurer implements WebMvcConfigurer {

    private final GlobalProtectionInterceptor globalProtectionInterceptor;
    private final TrustedGatewayInterceptor trustedGatewayInterceptor;
    private final CallerPartitionInterceptor callerPartitionInterceptor;
    private final CallerTrafficInterceptor callerTrafficInterceptor;

    /**
     * 创建 Web MVC 配置。
     *
     * @param globalProtectionInterceptor 全局保护拦截器
     * @param trustedGatewayInterceptor 可信网关拦截器
     * @param callerPartitionInterceptor caller 分区拦截器
     * @param callerTrafficInterceptor caller 流量拦截器
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    public CallerPartitionWebMvcConfigurer(
            GlobalProtectionInterceptor globalProtectionInterceptor,
            TrustedGatewayInterceptor trustedGatewayInterceptor,
            CallerPartitionInterceptor callerPartitionInterceptor,
            CallerTrafficInterceptor callerTrafficInterceptor
    ) {
        this.globalProtectionInterceptor = globalProtectionInterceptor;
        this.trustedGatewayInterceptor = trustedGatewayInterceptor;
        this.callerPartitionInterceptor = callerPartitionInterceptor;
        this.callerTrafficInterceptor = callerTrafficInterceptor;
    }

    /**
     * 注册需要 caller 分区键的 API 拦截器。
     *
     * @param registry 拦截器注册表
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(globalProtectionInterceptor).addPathPatterns("/api/v1/**");
        registry.addInterceptor(trustedGatewayInterceptor).addPathPatterns("/api/v1/**");
        registry.addInterceptor(callerPartitionInterceptor).addPathPatterns("/api/v1/**");
        registry.addInterceptor(callerTrafficInterceptor).addPathPatterns("/api/v1/**");
    }
}
