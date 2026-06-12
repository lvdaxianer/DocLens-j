package io.github.lvdaxianer.doclens.j.processing.interfaces;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * LLM Markdown 多配置状态接口。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
@RestController
@RequestMapping("/api/v1/llm-markdown-config")
public class LlmMarkdownConfigStateController {

    /** LLM Markdown 配置 HTTP 操作端口。 */
    private final LlmMarkdownConfigOperations operations;

    /**
     * 创建多配置状态接口。
     *
     * @param operations LLM Markdown 配置 HTTP 操作端口
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public LlmMarkdownConfigStateController(LlmMarkdownConfigOperations operations) {
        this.operations = operations;
    }

    /**
     * 更新指定 LLM Markdown 配置启停状态。
     *
     * @param id 配置 ID
     * @param request 启停请求
     * @return LLM Markdown 配置响应
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @PatchMapping("/{id}/enabled")
    public LlmMarkdownConfigResponse updateEnabled(
            @PathVariable String id,
            @RequestBody LlmMarkdownConfigEnabledRequest request
    ) {
        return operations.updateEnabled(id, request);
    }

    /**
     * 设置默认 LLM Markdown 配置。
     *
     * @param id 配置 ID
     * @return LLM Markdown 配置响应
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @PatchMapping("/{id}/default")
    public LlmMarkdownConfigResponse makeDefault(@PathVariable String id) {
        return operations.makeDefault(id);
    }
}
