package io.github.lvdaxianer.doclens.j.health.infrastructure;

import io.github.lvdaxianer.doclens.j.health.application.ModelHeartbeatProbe;
import io.github.lvdaxianer.doclens.j.health.application.ModelHeartbeatProbeResult;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthFailureType;
import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigSettings;
import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigTestResponse;
import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigTester;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfig;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfigRepository;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LLM Markdown 模型心跳探针。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public class LlmMarkdownHeartbeatProbe implements ModelHeartbeatProbe {

    private static final Logger LOGGER = LoggerFactory.getLogger(LlmMarkdownHeartbeatProbe.class);
    private static final String UNCONFIGURED_MESSAGE = "llm markdown config is not configured";
    private static final String BAD_RESPONSE_MESSAGE = "llm markdown heartbeat returned unhealthy";
    private static final String UNKNOWN_MESSAGE = "llm markdown heartbeat failed";

    private final LlmMarkdownConfigRepository configRepository;
    private final LlmMarkdownConfigTester configTester;

    /**
     * 创建 LLM Markdown 模型心跳探针。
     *
     * @param configRepository LLM Markdown 配置仓储
     * @param configTester LLM Markdown 配置测试器
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public LlmMarkdownHeartbeatProbe(
            LlmMarkdownConfigRepository configRepository,
            LlmMarkdownConfigTester configTester
    ) {
        this.configRepository = Objects.requireNonNull(configRepository,
                "llm markdown config repository is required");
        this.configTester = Objects.requireNonNull(configTester, "llm markdown config tester is required");
    }

    /**
     * 执行一次 LLM Markdown 心跳探测。
     *
     * @return 心跳探测结果
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public ModelHeartbeatProbeResult probe() {
        Optional<LlmMarkdownConfig> config = configRepository.find();
        // 当前存在可用 LLM Markdown 配置时执行真实连通性探测。
        if (config.isPresent() && config.get().isEnabled()) {
            return probeConfigured(config.get());
        } else {
            // 配置缺失或未启用时无需调用远端，直接返回可持久化的坏响应。
            return ModelHeartbeatProbeResult.failure(ModelHealthFailureType.BAD_RESPONSE, UNCONFIGURED_MESSAGE);
        }
    }

    /**
     * 执行已配置 LLM Markdown 心跳探测。
     *
     * @param config 当前配置
     * @return 心跳探测结果
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ModelHeartbeatProbeResult probeConfigured(LlmMarkdownConfig config) {
        try {
            return resultOf(configTester.test(settingsOf(config)));
        } catch (RuntimeException exception) {
            LOGGER.warn("[模型心跳] LLM Markdown心跳探测异常, configId={}, apiType={}, model={}, exceptionType={}",
                    config.id(), config.apiType().value(), config.model(), exception.getClass().getName());
            return ModelHeartbeatProbeResult.failure(ModelHealthFailureType.UNKNOWN, UNKNOWN_MESSAGE);
        }
    }

    /**
     * 将配置转换为连通性测试参数。
     *
     * @param config LLM Markdown 配置
     * @return 连通性测试参数
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private LlmMarkdownConfigSettings settingsOf(LlmMarkdownConfig config) {
        return new LlmMarkdownConfigSettings(config.apiType().value(), config.url(), config.model(),
                config.credentialValue());
    }

    /**
     * 将 LLM 连通性响应转换为心跳结果。
     *
     * @param response 连通性测试响应
     * @return 心跳探测结果
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ModelHeartbeatProbeResult resultOf(LlmMarkdownConfigTestResponse response) {
        // LLM 连通性测试确认健康时返回成功心跳结果。
        if (response.healthy()) {
            return ModelHeartbeatProbeResult.success();
        } else {
            // LLM 连通性测试明确失败时返回脱敏后的坏响应摘要。
            return ModelHeartbeatProbeResult.failure(ModelHealthFailureType.BAD_RESPONSE, BAD_RESPONSE_MESSAGE);
        }
    }
}
