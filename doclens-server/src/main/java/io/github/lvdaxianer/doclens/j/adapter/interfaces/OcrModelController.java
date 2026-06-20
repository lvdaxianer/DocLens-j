package io.github.lvdaxianer.doclens.j.adapter.interfaces;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrNodeMetricsViewReader;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrNodeManagementService;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeMetrics;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OCR 模型查询 API 控制器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
@RestController
@RequestMapping("/api/v1")
public class OcrModelController {

    private final OcrNodeManagementService managementService;
    private final OcrNodeRepository nodeRepository;
    private final OcrNodeMetricsViewReader metricsViewReader;

    /**
     * 创建 OCR 模型控制器。
     *
     * @param managementService OCR 节点管理服务
     * @param nodeRepository OCR 节点仓储
     * @param metricsViewReader OCR 节点指标读取器
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    public OcrModelController(
            OcrNodeManagementService managementService,
            OcrNodeRepository nodeRepository,
            OcrNodeMetricsViewReader metricsViewReader
    ) {
        this.managementService = managementService;
        this.nodeRepository = nodeRepository;
        this.metricsViewReader = metricsViewReader;
    }

    /**
     * 列出系统已支持 OCR 模型。
     *
     * @return OCR 模型列表响应
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @GetMapping("/ocr-models")
    public Map<String, List<OcrModelResponse>> listModels() {
        List<OcrNode> nodes = nodeRepository.listAll();
        Map<String, OcrNodeMetrics> metricsByNodeId = metricsViewReader.metricsByNodeIds(
                nodes.stream().map(OcrNode::id).toList());
        Map<String, List<OcrNode>> nodesByModelKey = nodes.stream()
                .collect(Collectors.groupingBy(OcrNode::modelKey));
        List<OcrModelResponse> items = managementService.listModels().stream()
                .map(model -> OcrModelResponse.from(model,
                        nodesByModelKey.getOrDefault(model.modelKey(), List.of()), metricsByNodeId))
                .toList();
        return Map.of("items", items);
    }
}
