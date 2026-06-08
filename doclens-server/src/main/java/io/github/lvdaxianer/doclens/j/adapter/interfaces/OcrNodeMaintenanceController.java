package io.github.lvdaxianer.doclens.j.adapter.interfaces;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrNodeManagementService;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.OcrHealthClient;
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
    private final ObjectProvider<OcrHealthClient> healthClientProvider;

    /**
     * 创建 OCR 节点维护控制器。
     *
     * @param managementService OCR 节点管理服务
     * @param healthClientProvider OCR 健康检查客户端提供器
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public OcrNodeMaintenanceController(
            OcrNodeManagementService managementService,
            ObjectProvider<OcrHealthClient> healthClientProvider
    ) {
        this.managementService = managementService;
        this.healthClientProvider = healthClientProvider;
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
        OcrHealthClient healthClient = healthClientProvider.getIfAvailable();
        return healthClient == null
                ? new OcrNodeTestResponse(false, "ocr health client is not configured")
                : testNodeHealth(node, healthClient);
    }

    /**
     * 执行节点健康测试。
     *
     * @param node OCR 节点
     * @param healthClient OCR 健康检查客户端
     * @return 节点测试响应
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private OcrNodeTestResponse testNodeHealth(OcrNode node, OcrHealthClient healthClient) {
        boolean healthy = healthClient.isHealthy(node);
        if (healthy) {
            return new OcrNodeTestResponse(true, "ocr node is healthy");
        } else {
            return new OcrNodeTestResponse(false, "ocr node health check failed");
        }
    }
}
