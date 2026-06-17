package io.github.lvdaxianer.doclens.j.dashboard.interfaces;

import io.github.lvdaxianer.doclens.j.ingestion.domain.CallerIdentity;
import io.github.lvdaxianer.doclens.j.query.application.DashboardQueryService;
import io.github.lvdaxianer.doclens.j.shared.web.CallerIdentityRequestResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Dashboard HTTP 层查询门面。
 *
 * @author lvdaxianerplus
 * @date 2026-06-17
 */
@Component
public class DashboardHttpFacade {

    private final DashboardQueryService dashboardQueryService;
    private final CallerIdentityRequestResolver callerIdentityRequestResolver;

    /**
     * 创建 Dashboard HTTP 层查询门面。
     *
     * @param dashboardQueryService Dashboard 查询服务
     * @param callerIdentityRequestResolver caller 请求解析器
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public DashboardHttpFacade(
            DashboardQueryService dashboardQueryService,
            CallerIdentityRequestResolver callerIdentityRequestResolver
    ) {
        this.dashboardQueryService = dashboardQueryService;
        this.callerIdentityRequestResolver = callerIdentityRequestResolver;
    }

    /**
     * 获取当前 caller 的 Dashboard 总览。
     *
     * @param request HTTP 请求
     * @return Dashboard 总览
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public Map<String, Object> summary(HttpServletRequest request) {
        return dashboardQueryService.summary(caller(request));
    }

    /**
     * 获取当前 caller 的 Dashboard 批次列表。
     *
     * @param request HTTP 请求
     * @return Dashboard 批次列表
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public Map<String, Object> batches(HttpServletRequest request) {
        return dashboardQueryService.batches(caller(request));
    }

    /**
     * 获取当前 caller 的 Dashboard 批次详情。
     *
     * @param request HTTP 请求
     * @param batchId 批次 ID
     * @return Dashboard 批次详情
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public Map<String, Object> batchDetail(HttpServletRequest request, String batchId) {
        return dashboardQueryService.batchDetail(caller(request), batchId);
    }

    /**
     * 获取当前 caller 的 OCR 健康摘要。
     *
     * @param request HTTP 请求
     * @return OCR 健康摘要
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public Map<String, Object> ocrHealth(HttpServletRequest request) {
        return dashboardQueryService.ocrHealth(caller(request));
    }

    /**
     * 解析当前请求 caller。
     *
     * @param request HTTP 请求
     * @return caller 身份
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private CallerIdentity caller(HttpServletRequest request) {
        return callerIdentityRequestResolver.resolve(request);
    }
}
