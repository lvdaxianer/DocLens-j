package io.github.lvdaxianer.doclens.j.shared.web;

import io.github.lvdaxianer.doclens.j.ingestion.domain.CallerIdentity;
import io.github.lvdaxianer.doclens.j.ingestion.infrastructure.CallerPartitionResolver;
import io.github.lvdaxianer.doclens.j.ingestion.infrastructure.CallerPartitionResolver.ResolvedCallerPartition;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 为所有 API 请求解析 caller 分区键。
 *
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
@Component
public class CallerPartitionInterceptor implements HandlerInterceptor {

    /** Request attribute 中保存的 caller identity 键。 */
    public static final String CALLER_IDENTITY_ATTRIBUTE = CallerPartitionInterceptor.class.getName()
            + ".callerIdentity";
    /** Request attribute 中保存的 caller 分区键解析结果键。 */
    public static final String RESOLVED_PARTITION_ATTRIBUTE = CallerPartitionInterceptor.class.getName()
            + ".resolvedPartition";

    private static final String CALLER_PARTITION_HEADER = "X-Doclens-Key";

    private final CallerPartitionResolver callerPartitionResolver;

    /**
     * 创建 caller 分区键拦截器。
     *
     * @param callerPartitionResolver caller 分区键解析器
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    public CallerPartitionInterceptor(CallerPartitionResolver callerPartitionResolver) {
        this.callerPartitionResolver = callerPartitionResolver;
    }

    /**
     * 在请求进入控制器前解析 caller 并写入 request attribute。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param handler 当前处理器
     * @return 是否继续处理
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        ResolvedCallerPartition resolvedPartition = callerPartitionResolver.resolveWithPartition(
                request.getHeader(CALLER_PARTITION_HEADER));
        CallerIdentity caller = resolvedPartition.callerIdentity();
        request.setAttribute(CALLER_IDENTITY_ATTRIBUTE, caller);
        request.setAttribute(RESOLVED_PARTITION_ATTRIBUTE, resolvedPartition);
        return true;
    }
}
