package io.github.lvdaxianer.doclens.j.processing.interfaces;

import org.springframework.web.bind.annotation.GetMapping;
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
     * 查询 LLM Markdown 配置。
     *
     * @return LLM Markdown 配置响应
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @GetMapping
    public LlmMarkdownConfigResponse getConfig() {
        return operations.getConfig();
    }

    /**
     * 保存 LLM Markdown 配置。
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
}
