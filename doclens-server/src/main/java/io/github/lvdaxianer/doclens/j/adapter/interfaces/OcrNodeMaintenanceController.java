package io.github.lvdaxianer.doclens.j.adapter.interfaces;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrNodeManagementService;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.OcrHealthChecker;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.OcrManualRecoveryService;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.OcrRuntimeNodePool;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * OCR 节点维护 API 控制器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
@RestController
@RequestMapping("/api/v1/ocr-nodes")
public class OcrNodeMaintenanceController {

    private final OcrNodeManagementService managementService;
    private final ObjectProvider<OcrHealthChecker> healthCheckerProvider;
    private final ObjectProvider<OcrManualRecoveryService> manualRecoveryServiceProvider;
    private final ObjectProvider<OcrRuntimeNodePool> nodePoolProvider;

    /**
     * 创建 OCR 节点维护控制器。
     *
     * @param managementService OCR 节点管理服务
     * @param healthCheckerProvider OCR 健康检查器提供器
     * @param manualRecoveryServiceProvider OCR 手动恢复服务提供器
     * @param nodePoolProvider OCR 运行时节点池提供器
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public OcrNodeMaintenanceController(
            OcrNodeManagementService managementService,
            ObjectProvider<OcrHealthChecker> healthCheckerProvider,
            ObjectProvider<OcrManualRecoveryService> manualRecoveryServiceProvider,
            ObjectProvider<OcrRuntimeNodePool> nodePoolProvider
    ) {
        this.managementService = managementService;
        this.healthCheckerProvider = healthCheckerProvider;
        this.manualRecoveryServiceProvider = manualRecoveryServiceProvider;
        this.nodePoolProvider = nodePoolProvider;
    }

    /**
     * 删除 OCR 节点。
     *
     * @param nodeId OCR 节点 ID
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @DeleteMapping("/{nodeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNode(@PathVariable String nodeId) {
        managementService.deleteNode(nodeId);
    }

    /**
     * 手动测试 OCR 节点健康状态。
     *
     * @param nodeId OCR 节点 ID
     * @return 节点测试响应
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @PostMapping("/{nodeId}/test")
    public OcrNodeTestResponse testNode(@PathVariable String nodeId) {
        OcrNode node = managementService.requireNode(nodeId);
        OcrHealthChecker healthChecker = healthCheckerProvider.getIfAvailable();
        return healthChecker == null
                ? new OcrNodeTestResponse(false, "ocr health checker is not configured")
                : testNodeHealth(node, healthChecker);
    }

    /**
     * 手动触发 OCR 节点恢复探测。
     *
     * @param nodeId OCR 节点 ID
     * @return 节点重连响应
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @PostMapping("/{nodeId}/reconnect")
    public OcrNodeReconnectResponse reconnectNode(@PathVariable String nodeId) {
        OcrNode node = managementService.requireNode(nodeId);
        OcrManualRecoveryService recoveryService = manualRecoveryServiceProvider.getIfAvailable();
        if (recoveryService == null) {
            throw new IllegalStateException("ocr manual recovery service is not configured");
        } else {
            return reconnectNode(node, recoveryService);
        }
    }

    /**
     * 执行节点健康测试。
     *
     * @param node OCR 节点
     * @param healthChecker OCR 健康检查器
     * @return 节点测试响应
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private OcrNodeTestResponse testNodeHealth(OcrNode node, OcrHealthChecker healthChecker) {
        boolean healthy = healthChecker.checkNode(node);
        nodePoolProvider.ifAvailable(OcrRuntimeNodePool::refresh);
        if (healthy) {
            return new OcrNodeTestResponse(true, "ocr node is healthy");
        } else {
            return new OcrNodeTestResponse(false, "ocr node health check failed");
        }
    }

    /**
     * 执行节点手动恢复并返回恢复结果。
     *
     * @param node OCR 节点
     * @param recoveryService OCR 手动恢复服务
     * @return 节点重连响应
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private OcrNodeReconnectResponse reconnectNode(OcrNode node, OcrManualRecoveryService recoveryService) {
        OcrManualRecoveryService.ManualRecoveryResult result = recoveryService.recover(node);
        nodePoolProvider.ifAvailable(OcrRuntimeNodePool::refresh);
        return OcrNodeReconnectResponse.of(result.healthy(), result.attempts(), result.node().status().name(),
                result.node().circuitOpenUntil());
    }
}
