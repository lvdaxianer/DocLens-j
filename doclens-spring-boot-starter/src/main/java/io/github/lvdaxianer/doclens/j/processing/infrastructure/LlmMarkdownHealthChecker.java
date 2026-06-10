package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigSettings;
import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigTestResponse;
import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigTester;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfig;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfigRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * LLM Markdown 配置健康检查器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public class LlmMarkdownHealthChecker {

    private static final String UNCONFIGURED_MESSAGE = "LLM Markdown config is not configured";

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
        Optional<LlmMarkdownConfig> current = configRepository.find();
        if (current.isPresent() && current.get().isEnabled()) {
            return Optional.of(updateCheckedConfig(current.get()));
        } else {
            current.ifPresent(config -> configRepository.updateHealth(
                    config.updateHealth(false, UNCONFIGURED_MESSAGE, nowSupplier.get())));
            return current;
        }
    }

    /**
     * 更新已配置 LLM 的健康状态。
     *
     * @param config 当前配置
     * @return 更新后的配置
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private LlmMarkdownConfig updateCheckedConfig(LlmMarkdownConfig config) {
        LlmMarkdownConfigTestResponse response = configTester.test(new LlmMarkdownConfigSettings(
                config.apiType().value(), config.url(), config.model(), config.credentialValue()));
        LlmMarkdownConfig updated = config.updateHealth(response.healthy(), healthMessage(response), nowSupplier.get());
        configRepository.updateHealth(updated);
        return updated;
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
