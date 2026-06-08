package io.github.lvdaxianer.doclens.j.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrNodeImageExecutor;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.OcrRuntimeNodePool;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.PaddleOcrNodeImageExecutor;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.PaddleOcrNativeAdapter;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.PaddleOcrNativeClient;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.PaddleOcrNativeResponseMapper;
import io.github.lvdaxianer.doclens.j.shared.config.DocLensProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
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
    @ConditionalOnProperty(prefix = "doclens.paddle-ocr", name = "enabled", havingValue = "true", matchIfMissing = true)
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
     * 创建 PaddleOCR 节点执行器。
     *
     * @param nodePool OCR 运行时节点池
     * @param client PaddleOCR 客户端
     * @param responseMapper PaddleOCR 响应映射器
     * @return OCR 节点执行器
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "doclens.paddle-ocr", name = "enabled", havingValue = "true", matchIfMissing = true)
    OcrNodeImageExecutor paddleOcrNodeImageExecutor(
            OcrRuntimeNodePool nodePool,
            PaddleOcrNativeClient client,
            PaddleOcrNativeResponseMapper responseMapper
    ) {
        return new PaddleOcrNodeImageExecutor(nodePool, client, responseMapper);
    }
}
