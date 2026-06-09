package io.github.lvdaxianer.doclens.j.adapter.interfaces;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrNodeManagementService;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrNodeMetricsViewReader;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCall;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeMetrics;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * OCR 节点管理 API 控制器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
@RestController
@RequestMapping("/api/v1")
public class OcrNodeController {

    private static final int RECENT_CALL_LIMIT = 20;

    private final OcrNodeManagementService managementService;
    private final OcrNodeMetricsViewReader metricsViewReader;
    private final OcrNodeCallRepository callRepository;

    /**
     * 创建 OCR 节点管理控制器。
     *
     * @param managementService OCR 节点管理服务
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public OcrNodeController(
            OcrNodeManagementService managementService,
            OcrNodeMetricsViewReader metricsViewReader,
            OcrNodeCallRepository callRepository
    ) {
        this.managementService = managementService;
        this.metricsViewReader = metricsViewReader;
        this.callRepository = callRepository;
    }

    /**
     * 按模型列出 OCR 节点。
     *
     * @param modelKey OCR 模型标识
     * @return OCR 节点列表响应
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @GetMapping("/ocr-models/{modelKey}/nodes")
    public Map<String, List<OcrNodeResponse>> listNodes(@PathVariable String modelKey) {
        List<OcrNode> nodes = managementService.listNodes(modelKey);
        Map<String, OcrNodeMetrics> metricsByNodeId = metricsViewReader.metricsByNodeIds(
                nodes.stream().map(OcrNode::id).toList());
        List<OcrNodeResponse> items = nodes.stream()
                .map(node -> OcrNodeResponse.from(node, metricsByNodeId.getOrDefault(node.id(),
                        new OcrNodeMetrics(0, 0, 0L, 0L, 0L, 0L, 0L, java.util.Optional.empty(),
                                java.util.Optional.empty()))))
                .toList();
        return Map.of("items", items);
    }

    /**
     * 查询节点最近调用记录。
     *
     * @param nodeId OCR 节点 ID
     * @return 最近调用记录响应
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @GetMapping("/ocr-nodes/{nodeId}/calls")
    public Map<String, List<OcrNodeCallResponse>> listRecentCalls(@PathVariable String nodeId) {
        List<OcrNodeCallResponse> items = callRepository.listRecentByNodeId(nodeId, RECENT_CALL_LIMIT).stream()
                .map(OcrNodeCallResponse::from)
                .toList();
        return Map.of("items", items);
    }

    /**
     * 创建 OCR 节点。
     *
     * @param modelKey OCR 模型标识
     * @param request OCR 节点请求
     * @return OCR 节点响应
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @PostMapping("/ocr-models/{modelKey}/nodes")
    @ResponseStatus(HttpStatus.CREATED)
    public OcrNodeResponse createNode(@PathVariable String modelKey, @RequestBody OcrNodeRequest request) {
        return OcrNodeResponse.from(managementService.createNode(modelKey, request.toSettings()));
    }
}
