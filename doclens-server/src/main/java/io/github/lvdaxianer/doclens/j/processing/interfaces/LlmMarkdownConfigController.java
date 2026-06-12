package io.github.lvdaxianer.doclens.j.processing.interfaces;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * LLM Markdown 后处理配置接口。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
@RestController
@RequestMapping("/api/v1/llm-markdown-config")
public class LlmMarkdownConfigController {

    private final LlmMarkdownConfigOperations operations;

    /**
     * 创建 LLM Markdown 配置接口。
     *
     * @param operations LLM Markdown 配置 HTTP 操作端口
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public LlmMarkdownConfigController(LlmMarkdownConfigOperations operations) {
        this.operations = operations;
    }

    /**
     * 查询全部 LLM Markdown 配置。
     *
     * @return LLM Markdown 配置响应列表
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @GetMapping
    public List<LlmMarkdownConfigResponse> listConfigs() {
        return operations.listConfigs();
    }

    /**
     * 保存兼容单配置入口的 LLM Markdown 配置。
     *
     * @param request LLM Markdown 配置请求
     * @return LLM Markdown 配置响应
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @PutMapping
    public LlmMarkdownConfigResponse updateConfig(@RequestBody LlmMarkdownConfigRequest request) {
        return operations.updateConfig(request);
    }

    /**
     * 测试 LLM Markdown 配置连通性。
     *
     * @param request LLM Markdown 配置请求
     * @return 连通性测试结果
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @PostMapping("/test")
    public LlmMarkdownConfigTestResultResponse testConfig(
            @RequestBody LlmMarkdownConfigRequest request
    ) {
        return operations.testConfig(request);
    }
}
