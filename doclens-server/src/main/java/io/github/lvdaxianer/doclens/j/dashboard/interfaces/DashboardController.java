package io.github.lvdaxianer.doclens.j.dashboard.interfaces;

import io.github.lvdaxianer.doclens.j.processing.infrastructure.CallbackDeliveryWorker;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard 控制台 API 控制器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardHttpFacade dashboardHttpFacade;
    private final CallbackDeliveryWorker callbackDeliveryWorker;

    /**
     * 创建 Dashboard 控制器。
     *
     * @param dashboardHttpFacade Dashboard HTTP 查询门面
     * @param callbackDeliveryWorker 回调投递 worker
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public DashboardController(
            DashboardHttpFacade dashboardHttpFacade,
            CallbackDeliveryWorker callbackDeliveryWorker
    ) {
        this.dashboardHttpFacade = dashboardHttpFacade;
        this.callbackDeliveryWorker = callbackDeliveryWorker;
    }

    /**
     * 获取 Dashboard 总览。
     *
     * @return Dashboard 总览
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @GetMapping("/summary")
    public Map<String, Object> summary(HttpServletRequest request) {
        return dashboardHttpFacade.summary(request);
    }

    /**
     * 获取 Dashboard 批次列表。
     *
     * @return Dashboard 批次列表
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @GetMapping("/batches")
    public Map<String, Object> batches(HttpServletRequest request) {
        return dashboardHttpFacade.batches(request);
    }

    /**
     * 获取 Dashboard 批次详情。
     *
     * @param batchId 批次 ID
     * @return Dashboard 批次详情
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @GetMapping("/batches/{batchId}")
    public Map<String, Object> batchDetail(@PathVariable String batchId, HttpServletRequest request) {
        return dashboardHttpFacade.batchDetail(request, batchId);
    }

    /**
     * 立即重试指定回调任务。
     *
     * @param callbackJobId 回调任务 ID
     * @return 回调重试结果
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    @PostMapping("/callback-jobs/{callbackJobId}/retry")
    public Map<String, Object> retryCallbackJob(@PathVariable String callbackJobId) {
        int deliveredCount = callbackDeliveryWorker.retryNow(callbackJobId);
        return Map.of("callback_job_id", callbackJobId, "delivered_count", deliveredCount);
    }

    /**
     * 获取 OCR 健康摘要。
     *
     * @return OCR 健康摘要
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @GetMapping("/ocr-health")
    public Map<String, Object> ocrHealth(HttpServletRequest request) {
        return dashboardHttpFacade.ocrHealth(request);
    }
}
