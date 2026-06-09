package io.github.lvdaxianer.doclens.j.processing.interfaces;

import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigService;
import org.springframework.stereotype.Component;

/**
 * 默认 LLM Markdown 配置 HTTP 操作实现。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
@Component
public class DefaultLlmMarkdownConfigOperations implements LlmMarkdownConfigOperations {

    private final LlmMarkdownConfigService configService;

    /**
     * 创建默认 LLM Markdown 配置 HTTP 操作实现。
     *
     * @param configService LLM Markdown 配置服务
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public DefaultLlmMarkdownConfigOperations(LlmMarkdownConfigService configService) {
        this.configService = configService;
    }

    /**
     * 查询 LLM Markdown 配置。
     *
     * @return LLM Markdown 配置响应
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Override
    public LlmMarkdownConfigResponse getConfig() {
        return LlmMarkdownConfigResponse.from(configService.getConfig());
    }

    /**
     * 保存 LLM Markdown 配置。
     *
     * @param request LLM Markdown 配置请求
     * @return LLM Markdown 配置响应
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Override
    public LlmMarkdownConfigResponse updateConfig(LlmMarkdownConfigRequest request) {
        return LlmMarkdownConfigResponse.from(configService.saveConfig(request.toSettings()));
    }
}
