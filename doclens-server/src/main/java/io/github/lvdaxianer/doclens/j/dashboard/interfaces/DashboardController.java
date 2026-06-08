package io.github.lvdaxianer.doclens.j.dashboard.interfaces;

import io.github.lvdaxianer.doclens.j.query.application.DashboardQueryService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    private final DashboardQueryService dashboardQueryService;

    /**
     * 创建 Dashboard 控制器。
     *
     * @param dashboardQueryService Dashboard 查询服务
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public DashboardController(DashboardQueryService dashboardQueryService) {
        this.dashboardQueryService = dashboardQueryService;
    }

    /**
     * 获取 Dashboard 总览。
     *
     * @return Dashboard 总览
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @GetMapping("/summary")
    public Map<String, Object> summary() {
        return dashboardQueryService.summary();
    }

    /**
     * 获取 Dashboard 批次列表。
     *
     * @return Dashboard 批次列表
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @GetMapping("/batches")
    public Map<String, Object> batches() {
        return dashboardQueryService.batches();
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
    public Map<String, Object> batchDetail(@PathVariable String batchId) {
        return dashboardQueryService.batchDetail(batchId);
    }

    /**
     * 获取 OCR 健康摘要。
     *
     * @return OCR 健康摘要
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @GetMapping("/ocr-health")
    public Map<String, Object> ocrHealth() {
        return dashboardQueryService.ocrHealth();
    }
}
