package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessor;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownApiType;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfig;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.AnthropicMarkdownPostProcessor.AnthropicMarkdownPostProcessorOptions;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.HttpMarkdownPostProcessor.HttpMarkdownPostProcessorOptions;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.EnvironmentCredentialResolver;
import java.net.URI;
import java.time.Duration;

/**
 * LLM Markdown 后处理器工厂。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public class MarkdownPostProcessorFactory {

    private final ObjectMapper objectMapper;
    private final Duration timeout;
    private final EnvironmentCredentialResolver credentialResolver;

    /**
     * 创建 LLM Markdown 后处理器工厂。
     *
     * @param objectMapper JSON 映射器
     * @param timeout HTTP 请求超时时间
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public MarkdownPostProcessorFactory(ObjectMapper objectMapper, Duration timeout) {
        this(objectMapper, timeout, new EnvironmentCredentialResolver());
    }

    /**
     * 创建 LLM Markdown 后处理器工厂。
     *
     * @param objectMapper JSON 映射器
     * @param timeout HTTP 请求超时时间
     * @param credentialResolver 凭证解析器
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    public MarkdownPostProcessorFactory(
            ObjectMapper objectMapper,
            Duration timeout,
            EnvironmentCredentialResolver credentialResolver
    ) {
        this.objectMapper = objectMapper;
        this.timeout = timeout;
        this.credentialResolver = credentialResolver;
    }

    /**
     * 按运行时配置创建后处理器。
     *
     * @param config LLM Markdown 配置
     * @return Markdown 后处理器
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public MarkdownPostProcessor create(LlmMarkdownConfig config) {
        return createWithCredentialEnvVar(config.apiType(), URI.create(config.url()), config.model(),
                config.credentialValue());
    }

    /**
     * 按协议参数和凭证环境变量名创建后处理器。
     *
     * @param apiType API 协议类型
     * @param endpoint LLM endpoint
     * @param model 模型名称
     * @param credentialEnvVar 凭证环境变量名
     * @return Markdown 后处理器
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    public MarkdownPostProcessor createWithCredentialEnvVar(
            LlmMarkdownApiType apiType,
            URI endpoint,
            String model,
            String credentialEnvVar
    ) {
        String apiKey = credentialResolver.resolve(credentialEnvVar);
        return create(apiType, endpoint, model, apiKey);
    }

    /**
     * 按协议参数创建后处理器。
     *
     * @param apiType API 协议类型
     * @param endpoint LLM endpoint
     * @param model 模型名称
     * @param apiKey API Key
     * @return Markdown 后处理器
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public MarkdownPostProcessor create(LlmMarkdownApiType apiType, URI endpoint, String model, String apiKey) {
        if (apiType == LlmMarkdownApiType.ANTHROPIC) {
            AnthropicMarkdownPostProcessorOptions options = new AnthropicMarkdownPostProcessorOptions(endpoint,
                    model, apiKey, timeout);
            return new AnthropicMarkdownPostProcessor(objectMapper, options);
        } else {
            HttpMarkdownPostProcessorOptions options = new HttpMarkdownPostProcessorOptions(endpoint, model, apiKey,
                    timeout);
            return new HttpMarkdownPostProcessor(objectMapper, options);
        }
    }
}
