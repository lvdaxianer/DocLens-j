package io.github.lvdaxianer.doclens.j.adapter.interfaces;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrNodeManagementService;
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

    private final OcrNodeManagementService managementService;

    /**
     * 创建 OCR 节点管理控制器。
     *
     * @param managementService OCR 节点管理服务
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public OcrNodeController(OcrNodeManagementService managementService) {
        this.managementService = managementService;
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
        List<OcrNodeResponse> items = managementService.listNodes(modelKey).stream()
                .map(OcrNodeResponse::from)
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
