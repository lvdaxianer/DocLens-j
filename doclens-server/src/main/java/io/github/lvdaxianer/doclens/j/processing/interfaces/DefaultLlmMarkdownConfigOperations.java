package io.github.lvdaxianer.doclens.j.processing.interfaces;

import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigService;
import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigTestResponse;
import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigTester;
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
    private final LlmMarkdownConfigTester configTester;

    /**
     * 创建默认 LLM Markdown 配置 HTTP 操作实现。
     *
     * @param configService LLM Markdown 配置服务
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public DefaultLlmMarkdownConfigOperations(
            LlmMarkdownConfigService configService,
            LlmMarkdownConfigTester configTester
    ) {
        this.configService = configService;
        this.configTester = configTester;
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

    /**
     * 测试 LLM Markdown 配置。
     *
     * @param request LLM Markdown 配置请求
     * @return 测试结果
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Override
    public LlmMarkdownConfigTestResponse testConfig(LlmMarkdownConfigRequest request) {
        return configTester.test(request.toSettings());
    }
}
