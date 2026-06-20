package io.github.lvdaxianer.doclens.j.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigService;
import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigTester;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkCheckpointStore;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessor;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfigRepository;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.ConfigurableMarkdownPostProcessor;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.DefaultLlmMarkdownConfigTester;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.FileSystemMarkdownChunkCheckpointStore;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.HttpMarkdownPostProcessor;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.HttpMarkdownPostProcessor.HttpMarkdownPostProcessorOptions;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.LlmMarkdownHealthCheckScheduler;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.LlmMarkdownHealthChecker;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.NamedThreadPoolFactory;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * LLM Markdown 后处理与健康检查自动配置。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
@AutoConfiguration(after = DocLensAutoConfiguration.class)
@ConditionalOnBean(LlmMarkdownConfigRepository.class)
public class DocLensLlmMarkdownAutoConfiguration {

    private static final int LLM_MARKDOWN_TIMEOUT_SECONDS = 60;
    private static final int LLM_MARKDOWN_HEALTH_INTERVAL_SECONDS = 5;
    private static final int LLM_MARKDOWN_CHUNK_WORKER_THREADS = 20;

    /**
     * 创建 LLM Markdown 配置服务。
     *
     * @param configRepository LLM Markdown 配置仓储
     * @return LLM Markdown 配置服务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Bean
    @ConditionalOnMissingBean
    LlmMarkdownConfigService llmMarkdownConfigService(LlmMarkdownConfigRepository configRepository) {
        return new LlmMarkdownConfigService(configRepository);
    }

    /**
     * 创建 LLM Markdown 配置测试器。
     *
     * @param objectMapper JSON 映射器
     * @param llmMarkdownConfigService LLM Markdown 配置服务
     * @return LLM 配置测试器
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Bean
    @ConditionalOnMissingBean
    LlmMarkdownConfigTester llmMarkdownConfigTester(
            ObjectMapper objectMapper,
            LlmMarkdownConfigService llmMarkdownConfigService
    ) {
        return new DefaultLlmMarkdownConfigTester(objectMapper, llmMarkdownConfigService);
    }

    /**
     * 创建 LLM Markdown 健康检查器。
     *
     * @param configRepository LLM Markdown 配置仓储
     * @param configTester LLM Markdown 配置测试器
     * @return LLM Markdown 健康检查器
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Bean
    @ConditionalOnMissingBean
    LlmMarkdownHealthChecker llmMarkdownHealthChecker(
            LlmMarkdownConfigRepository configRepository,
            LlmMarkdownConfigTester configTester
    ) {
        return new LlmMarkdownHealthChecker(configRepository, configTester);
    }

    /**
     * 创建 LLM Markdown 健康检查调度线程池。
     *
     * @return LLM Markdown 健康检查调度线程池
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "doclensLlmHealthSchedulerExecutor")
    ScheduledExecutorService doclensLlmHealthSchedulerExecutor() {
        return Executors.newSingleThreadScheduledExecutor(new NamedThreadPoolFactory("doclens-llm-health-scheduler-"));
    }

    /**
     * 创建 LLM Markdown 健康检查工作线程池。
     *
     * @return LLM Markdown 健康检查工作线程池
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "doclensLlmHealthExecutor")
    ExecutorService doclensLlmHealthExecutor() {
        return Executors.newSingleThreadExecutor(new NamedThreadPoolFactory("doclens-llm-health-"));
    }

    /**
     * 创建 LLM Markdown 分片共享工作线程池。
     *
     * @return LLM Markdown 分片共享工作线程池
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "doclensLlmMarkdownChunkExecutor")
    ExecutorService doclensLlmMarkdownChunkExecutor() {
        return Executors.newFixedThreadPool(LLM_MARKDOWN_CHUNK_WORKER_THREADS,
                new NamedThreadPoolFactory("doclens-llm-markdown-chunk-"));
    }

    /**
     * 创建 LLM Markdown chunk checkpoint 存储。
     *
     * @param properties Spring 配置属性
     * @return Markdown chunk checkpoint 存储
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Bean
    @ConditionalOnMissingBean
    MarkdownChunkCheckpointStore markdownChunkCheckpointStore(DocLensSpringProperties properties) {
        return new FileSystemMarkdownChunkCheckpointStore(Path.of(properties.storageRoot()));
    }

    /**
     * 创建 LLM Markdown checkpoint 写入线程池。
     *
     * @return LLM Markdown checkpoint 写入线程池
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "doclensLlmMarkdownCheckpointExecutor")
    ExecutorService doclensLlmMarkdownCheckpointExecutor() {
        return Executors.newSingleThreadExecutor(new NamedThreadPoolFactory("doclens-llm-markdown-checkpoint-"));
    }

    /**
     * 创建 LLM Markdown 健康检查周期调度器。
     *
     * @param healthChecker LLM Markdown 健康检查器
     * @param schedulerExecutor LLM Markdown 健康检查调度线程池
     * @param healthExecutor LLM Markdown 健康检查工作线程池
     * @return LLM Markdown 健康检查调度器
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Bean
    @ConditionalOnMissingBean
    LlmMarkdownHealthCheckScheduler llmMarkdownHealthCheckScheduler(
            LlmMarkdownHealthChecker healthChecker,
            @Qualifier("doclensLlmHealthSchedulerExecutor") ScheduledExecutorService schedulerExecutor,
            @Qualifier("doclensLlmHealthExecutor") ExecutorService healthExecutor
    ) {
        return new LlmMarkdownHealthCheckScheduler(healthChecker, schedulerExecutor, healthExecutor,
                LLM_MARKDOWN_HEALTH_INTERVAL_SECONDS);
    }

    /**
     * 应用启动完成后启动 LLM Markdown 健康检查调度。
     *
     * @param scheduler LLM Markdown 健康检查调度器
     * @return 启动任务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Bean
    @ConditionalOnMissingBean(name = "llmMarkdownHealthCheckSchedulerRunner")
    ApplicationRunner llmMarkdownHealthCheckSchedulerRunner(LlmMarkdownHealthCheckScheduler scheduler) {
        return args -> scheduler.start();
    }

    /**
     * 创建支持运行时配置的 Markdown 后处理器。
     *
     * @param objectMapper JSON 映射器
     * @param properties Spring 配置属性
     * @param configRepository LLM Markdown 配置仓储
     * @return Markdown 后处理器
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Bean
    @ConditionalOnMissingBean
    MarkdownPostProcessor markdownPostProcessor(
            ObjectMapper objectMapper,
            DocLensSpringProperties properties,
            LlmMarkdownConfigRepository configRepository,
            @Qualifier("doclensLlmMarkdownChunkExecutor") ExecutorService chunkExecutor
    ) {
        MarkdownPostProcessor fallbackProcessor = fallbackMarkdownPostProcessor(objectMapper, properties);
        return new ConfigurableMarkdownPostProcessor(objectMapper, configRepository, fallbackProcessor, chunkExecutor);
    }

    /**
     * 创建 Spring 配置兜底 Markdown 后处理器。
     *
     * @param objectMapper JSON 映射器
     * @param properties Spring 配置属性
     * @return Markdown 后处理器
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    MarkdownPostProcessor fallbackMarkdownPostProcessor(ObjectMapper objectMapper, DocLensSpringProperties properties) {
        DocLensSpringProperties.LlmMarkdownProperties llmMarkdown = properties.llmMarkdown();
        if (isConfigured(llmMarkdown)) {
            // URL 与模型已配置时启用 HTTP LLM Markdown 后处理。
            HttpMarkdownPostProcessorOptions options = new HttpMarkdownPostProcessorOptions(
                    URI.create(llmMarkdown.url()), llmMarkdown.model(), llmMarkdown.apiKey(),
                    Duration.ofSeconds(LLM_MARKDOWN_TIMEOUT_SECONDS));
            return new HttpMarkdownPostProcessor(objectMapper, options);
        } else {
            // 未配置 URL 或模型时保持 OCR 合并纯文本直通。
            return MarkdownPostProcessor.noop();
        }
    }

    /**
     * 判断 LLM Markdown 是否已配置。
     *
     * @param properties LLM Markdown 配置
     * @return 是否已配置
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private boolean isConfigured(DocLensSpringProperties.LlmMarkdownProperties properties) {
        return properties.url() != null && !properties.url().isBlank()
                && properties.model() != null && !properties.model().isBlank();
    }
}
