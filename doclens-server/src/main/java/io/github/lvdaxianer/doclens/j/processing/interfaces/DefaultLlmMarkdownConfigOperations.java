package io.github.lvdaxianer.doclens.j.processing.interfaces;

import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigService;
import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigTester;
import java.util.List;
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
     * 查询全部 LLM Markdown 配置。
     *
     * @return LLM Markdown 配置响应列表
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Override
    public List<LlmMarkdownConfigResponse> listConfigs() {
        return configService.listConfigs().stream().map(LlmMarkdownConfigResponse::from).toList();
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
     * 创建 LLM Markdown 配置。
     *
     * @param request LLM Markdown 配置请求
     * @return LLM Markdown 配置响应
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Override
    public LlmMarkdownConfigResponse createConfig(LlmMarkdownConfigRequest request) {
        return LlmMarkdownConfigResponse.from(configService.createConfig(request.toSettings()));
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
    @Override
    public LlmMarkdownConfigResponse updateConfig(String id, LlmMarkdownConfigRequest request) {
        return LlmMarkdownConfigResponse.from(configService.updateConfig(id, request.toSettings()));
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
    @Override
    public LlmMarkdownConfigResponse updateEnabled(String id, LlmMarkdownConfigEnabledRequest request) {
        return LlmMarkdownConfigResponse.from(configService.updateEnabled(id, request.enabled()));
    }

    /**
     * 设置默认 LLM Markdown 配置。
     *
     * @param id 配置 ID
     * @return LLM Markdown 配置响应
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Override
    public LlmMarkdownConfigResponse makeDefault(String id) {
        return LlmMarkdownConfigResponse.from(configService.makeDefault(id));
    }

    /**
     * 删除 LLM Markdown 配置。
     *
     * @param id 配置 ID
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Override
    public void deleteConfig(String id) {
        configService.deleteConfig(id);
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
    public LlmMarkdownConfigTestResultResponse testConfig(LlmMarkdownConfigRequest request) {
        return LlmMarkdownConfigTestResultResponse.from(
                configTester.test(configService.settingsForTest(request.toSettings())));
    }
}
