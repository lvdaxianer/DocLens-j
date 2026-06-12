package io.github.lvdaxianer.doclens.j.processing.interfaces;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * LLM Markdown 多配置增删改接口。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
@RestController
@RequestMapping("/api/v1/llm-markdown-config")
public class LlmMarkdownConfigCrudController {

    /** LLM Markdown 配置 HTTP 操作端口。 */
    private final LlmMarkdownConfigOperations operations;

    /**
     * 创建多配置增删改接口。
     *
     * @param operations LLM Markdown 配置 HTTP 操作端口
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public LlmMarkdownConfigCrudController(LlmMarkdownConfigOperations operations) {
        this.operations = operations;
    }

    /**
     * 创建 LLM Markdown 配置。
     *
     * @param request LLM Markdown 配置请求
     * @return LLM Markdown 配置响应
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @PostMapping
    public LlmMarkdownConfigResponse createConfig(@RequestBody LlmMarkdownConfigRequest request) {
        return operations.createConfig(request);
    }

    /**
     * 更新指定 LLM Markdown 配置。
     *
     * @param id 配置 ID
     * @param request LLM Markdown 配置请求
     * @return LLM Markdown 配置响应
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @PutMapping("/{id}")
    public LlmMarkdownConfigResponse updateConfig(
            @PathVariable String id,
            @RequestBody LlmMarkdownConfigRequest request
    ) {
        return operations.updateConfig(id, request);
    }

    /**
     * 删除 LLM Markdown 配置。
     *
     * @param id 配置 ID
     * @return 空响应
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConfig(@PathVariable String id) {
        operations.deleteConfig(id);
        return ResponseEntity.noContent().build();
    }
}
