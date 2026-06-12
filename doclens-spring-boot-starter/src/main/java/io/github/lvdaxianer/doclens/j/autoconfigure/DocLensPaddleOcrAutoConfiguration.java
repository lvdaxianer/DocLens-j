package io.github.lvdaxianer.doclens.j.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrNodeImageExecutor;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.DashScopeOnlineOcrClient;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeRepository;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.OcrNodeBootstrapper;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.OcrRuntimeNodePool;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.OllamaOcrClient;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.OcrNodeProtocolClients;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.PaddleOcrNodeImageExecutor;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.PaddleOcrNativeAdapter;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.PaddleOcrNativeClient;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.PaddleOcrNativeResponseMapper;
import io.github.lvdaxianer.doclens.j.shared.config.DocLensProperties;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.ExecutorService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * PaddleOCR 原生 API 适配器自动配置。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
@AutoConfiguration(after = DocLensAutoConfiguration.class)
public class DocLensPaddleOcrAutoConfiguration {

    private static final int MIN_HTTP_TIMEOUT_SECONDS = 1;

    /**
     * 创建 PaddleOCR 响应映射器。
     *
     * @param objectMapper Jackson 映射器
     * @return 响应映射器
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Bean
    @ConditionalOnMissingBean
    PaddleOcrNativeResponseMapper paddleOcrNativeResponseMapper(ObjectMapper objectMapper) {
        return new PaddleOcrNativeResponseMapper(objectMapper);
    }

    /**
     * 创建 PaddleOCR 原生客户端。
     *
     * @param properties DocLens 配置
     * @param objectMapper Jackson 映射器
     * @return PaddleOCR 客户端
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Bean
    @ConditionalOnMissingBean
    PaddleOcrNativeClient paddleOcrNativeClient(DocLensProperties properties, ObjectMapper objectMapper) {
        return new PaddleOcrNativeClient(properties, objectMapper);
    }

    /**
     * 创建 PaddleOCR 原生适配器。
     *
     * @param client PaddleOCR 客户端
     * @param responseMapper 响应映射器
     * @return PaddleOCR 适配器
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "doclens.paddle-ocr", name = "enabled", havingValue = "true", matchIfMissing = true)
    PaddleOcrNativeAdapter paddleOcrNativeAdapter(
            PaddleOcrNativeClient client,
            PaddleOcrNativeResponseMapper responseMapper
    ) {
        return new PaddleOcrNativeAdapter(client, responseMapper);
    }

    /**
     * 创建 DashScope compatible 在线 OCR 客户端。
     *
     * @param objectMapper Jackson 映射器
     * @param properties DocLens 配置
     * @return DashScope compatible 在线 OCR 客户端
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Bean
    @ConditionalOnMissingBean
    DashScopeOnlineOcrClient dashScopeOnlineOcrClient(ObjectMapper objectMapper, DocLensProperties properties) {
        return new DashScopeOnlineOcrClient(objectMapper, DashScopeOnlineOcrClient.DEFAULT_ENDPOINT,
                Duration.ofSeconds(Math.max(MIN_HTTP_TIMEOUT_SECONDS, properties.paddleOcr().timeoutSeconds())));
    }

    /**
     * 创建 Ollama OCR 客户端。
     *
     * @param objectMapper Jackson 映射器
     * @param properties DocLens 配置
     * @return Ollama OCR 客户端
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Bean
    @ConditionalOnMissingBean
    OllamaOcrClient ollamaOcrClient(ObjectMapper objectMapper, DocLensProperties properties) {
        return new OllamaOcrClient(objectMapper,
                Duration.ofSeconds(Math.max(MIN_HTTP_TIMEOUT_SECONDS, properties.paddleOcr().timeoutSeconds())));
    }

    /**
     * 创建 OCR 节点协议客户端集合。
     *
     * @param client PaddleOCR 客户端
     * @param responseMapper PaddleOCR 响应映射器
     * @param onlineClient 在线 OCR 客户端
     * @param ollamaClient Ollama OCR 客户端
     * @return OCR 节点协议客户端集合
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Bean
    @ConditionalOnMissingBean
    OcrNodeProtocolClients ocrNodeProtocolClients(
            PaddleOcrNativeClient client,
            PaddleOcrNativeResponseMapper responseMapper,
            DashScopeOnlineOcrClient onlineClient,
            OllamaOcrClient ollamaClient
    ) {
        return new OcrNodeProtocolClients(client, responseMapper, onlineClient, ollamaClient);
    }

    /**
     * 创建 PaddleOCR 节点执行器。
     *
     * @param nodePool OCR 运行时节点池
     * @param protocolClients OCR 协议客户端集合
     * @param ocrRequestExecutor OCR 请求线程池
     * @return OCR 节点执行器
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "doclens.paddle-ocr", name = "enabled", havingValue = "true", matchIfMissing = true)
    OcrNodeImageExecutor paddleOcrNodeImageExecutor(
            OcrRuntimeNodePool nodePool,
            OcrNodeProtocolClients protocolClients,
            @Qualifier("doclensOcrRequestExecutor") ExecutorService ocrRequestExecutor
    ) {
        return new PaddleOcrNodeImageExecutor(nodePool, protocolClients, ocrRequestExecutor);
    }

    /**
     * 创建 PaddleOCR 节点启动初始化器。
     *
     * @param nodeRepository OCR 节点仓储
     * @param properties Spring 配置属性
     * @return OCR 节点启动初始化器
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "doclens.paddle-ocr", name = "enabled", havingValue = "true", matchIfMissing = true)
    OcrNodeBootstrapper ocrNodeBootstrapper(
            OcrNodeRepository nodeRepository,
            DocLensSpringProperties properties
    ) {
        return new OcrNodeBootstrapper(nodeRepository, properties.paddleOcr().bootstrapNodes());
    }

    /**
     * 应用启动完成后初始化 PaddleOCR 节点并刷新运行时池。
     *
     * @param bootstrapper OCR 节点启动初始化器
     * @param nodePool OCR 运行时节点池
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Bean
    @ConditionalOnMissingBean(name = "paddleOcrBootstrapRunner")
    @ConditionalOnBean(OcrNodeBootstrapper.class)
    ApplicationRunner paddleOcrBootstrapRunner(OcrNodeBootstrapper bootstrapper, OcrRuntimeNodePool nodePool) {
        return args -> {
            bootstrapper.bootstrap(OffsetDateTime.now());
            nodePool.refresh();
        };
    }
}
