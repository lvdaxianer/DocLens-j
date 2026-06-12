package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigSettings;
import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigTestResponse;
import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigTester;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfig;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfigRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * LLM Markdown 配置健康检查器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public class LlmMarkdownHealthChecker {

    private final LlmMarkdownConfigRepository configRepository;
    private final LlmMarkdownConfigTester configTester;
    private final Supplier<OffsetDateTime> nowSupplier;

    /**
     * 创建 LLM Markdown 健康检查器。
     *
     * @param configRepository LLM Markdown 配置仓储
     * @param configTester LLM Markdown 配置测试器
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public LlmMarkdownHealthChecker(
            LlmMarkdownConfigRepository configRepository,
            LlmMarkdownConfigTester configTester
    ) {
        this(configRepository, configTester, OffsetDateTime::now);
    }

    /**
     * 创建可控时钟的 LLM Markdown 健康检查器。
     *
     * @param configRepository LLM Markdown 配置仓储
     * @param configTester LLM Markdown 配置测试器
     * @param nowSupplier 当前时间提供器
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public LlmMarkdownHealthChecker(
            LlmMarkdownConfigRepository configRepository,
            LlmMarkdownConfigTester configTester,
            Supplier<OffsetDateTime> nowSupplier
    ) {
        this.configRepository = configRepository;
        this.configTester = configTester;
        this.nowSupplier = nowSupplier;
    }

    /**
     * 执行一次 LLM Markdown 健康检查。
     *
     * @return 检查结果
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public Optional<LlmMarkdownConfig> checkOnce() {
        List<LlmMarkdownConfig> configs = configRepository.listConfigs();
        if (configs.isEmpty()) {
            // 未配置任何 LLM 时健康检查空跑，不写失败状态。
            return Optional.empty();
        } else {
            // 有配置时批量检查所有已配置项，未配置占位项不写错误日志。
            return checkConfigured(configs);
        }
    }

    /**
     * 批量检查已配置的 LLM 配置。
     *
     * @param configs 配置列表
     * @return 本轮第一条更新后的配置
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private Optional<LlmMarkdownConfig> checkConfigured(List<LlmMarkdownConfig> configs) {
        List<LlmMarkdownConfig> updatedConfigs = new ArrayList<>(configs.size());
        for (LlmMarkdownConfig config : configs) {
            if (config.isConfigured()) {
                // 已配置 URL 和模型时执行健康检查。
                updatedConfigs.add(checkedConfig(config));
            } else {
                // 未配置占位项保持原状态，避免刷错误健康消息。
                updatedConfigs.add(config);
            }
        }
        configRepository.saveAll(updatedConfigs);
        return updatedConfigs.stream().findFirst();
    }

    /**
     * 更新已配置 LLM 的健康状态。
     *
     * @param config 当前配置
     * @return 更新后的配置
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private LlmMarkdownConfig checkedConfig(LlmMarkdownConfig config) {
        LlmMarkdownConfigTestResponse response = configTester.test(new LlmMarkdownConfigSettings(
                config.apiType().value(), config.url(), config.model(), config.credentialValue()));
        return config.updateHealth(response.healthy(), healthMessage(response), nowSupplier.get());
    }

    /**
     * 成功时清空健康消息，失败时保留可展示原因。
     *
     * @param response 测试响应
     * @return 健康消息
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private String healthMessage(LlmMarkdownConfigTestResponse response) {
        return response.healthy() ? "" : response.message();
    }
}
