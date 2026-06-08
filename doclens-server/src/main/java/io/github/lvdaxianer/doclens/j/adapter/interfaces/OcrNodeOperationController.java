package io.github.lvdaxianer.doclens.j.adapter.interfaces;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrNodeManagementService;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OCR 节点实例操作 API 控制器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
@RestController
@RequestMapping("/api/v1/ocr-nodes")
public class OcrNodeOperationController {

    private final OcrNodeManagementService managementService;

    /**
     * 创建 OCR 节点实例操作控制器。
     *
     * @param managementService OCR 节点管理服务
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public OcrNodeOperationController(OcrNodeManagementService managementService) {
        this.managementService = managementService;
    }

    /**
     * 更新 OCR 节点配置。
     *
     * @param nodeId OCR 节点 ID
     * @param request OCR 节点请求
     * @return OCR 节点响应
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @PutMapping("/{nodeId}")
    public OcrNodeResponse updateNode(@PathVariable String nodeId, @RequestBody OcrNodeRequest request) {
        return OcrNodeResponse.from(managementService.updateNode(nodeId, request.toSettings()));
    }

    /**
     * 更新 OCR 节点启用状态。
     *
     * @param nodeId OCR 节点 ID
     * @param request 启停请求
     * @return OCR 节点响应
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @PatchMapping("/{nodeId}/enabled")
    public OcrNodeResponse updateEnabled(@PathVariable String nodeId, @RequestBody OcrNodeEnabledRequest request) {
        return OcrNodeResponse.from(managementService.changeEnabled(nodeId, request.enabled()));
    }
}
