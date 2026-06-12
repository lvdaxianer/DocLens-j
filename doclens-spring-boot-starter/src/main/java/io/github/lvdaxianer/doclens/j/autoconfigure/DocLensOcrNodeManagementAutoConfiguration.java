package io.github.lvdaxianer.doclens.j.autoconfigure;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrNodeManagementService;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrNodePoolRefresher;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrModelDefinition;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrModelRegistry;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeRepository;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.OcrRuntimeNodePool;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.IdGenerator;
import java.util.List;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * OCR 节点管理自动配置。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
@AutoConfiguration(after = DocLensAutoConfiguration.class)
public class DocLensOcrNodeManagementAutoConfiguration {

    private static final String IMAGE_INPUT_TYPE = "image";
    private static final String PADDLE_MODEL_KEY = "paddle_ocr";
    private static final String PADDLE_MODEL_NAME = "PaddleOCR";
    private static final String PADDLE_MODEL_DESCRIPTION = "PaddleOCR native-compatible HTTP API";
    private static final String PADDLE_OCR_PATH = "/ocr";
    private static final String PADDLE_HEALTH_PATH = "/ocr";
    private static final String OLLAMA_MODEL_KEY = "ollama_deepseek_ocr";
    private static final String OLLAMA_MODEL_NAME = "Ollama DeepSeek OCR";
    private static final String OLLAMA_MODEL_DESCRIPTION = "Ollama DeepSeek-OCR markdown API";
    private static final String OLLAMA_GENERATE_PATH = "/api/generate";
    private static final String OLLAMA_CHANNEL_KEY = "ollama";
    private static final String OLLAMA_PROVIDER_MODEL = "deepseek-ocr:latest";
    private static final String EMPTY_DEFAULT_VALUE = "";
    private static final int PADDLE_DEFAULT_PORT = 18081;
    private static final int OLLAMA_DEFAULT_PORT = 11434;

    /**
     * 创建 OCR 模型注册表。
     *
     * @return OCR 模型注册表
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Bean
    @ConditionalOnMissingBean
    OcrModelRegistry ocrModelRegistry() {
        return new OcrModelRegistry(List.of(paddleOcrDefinition(), ollamaOcrDefinition()));
    }

    /**
     * 创建 PaddleOCR 模型定义。
     *
     * @return PaddleOCR 模型定义
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private OcrModelDefinition paddleOcrDefinition() {
        return OcrModelDefinition.create(new OcrModelDefinition.CreateCommand(
                new OcrModelDefinition.Identity(PADDLE_MODEL_KEY, PADDLE_MODEL_NAME, PADDLE_MODEL_DESCRIPTION),
                new OcrModelDefinition.Capability(List.of(IMAGE_INPUT_TYPE), PADDLE_OCR_PATH, PADDLE_HEALTH_PATH),
                new OcrModelDefinition.RuntimeDefaults(PADDLE_DEFAULT_PORT, EMPTY_DEFAULT_VALUE, EMPTY_DEFAULT_VALUE,
                        true)));
    }

    /**
     * 创建 Ollama DeepSeek OCR 模型定义。
     *
     * @return Ollama DeepSeek OCR 模型定义
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private OcrModelDefinition ollamaOcrDefinition() {
        return OcrModelDefinition.create(new OcrModelDefinition.CreateCommand(
                new OcrModelDefinition.Identity(OLLAMA_MODEL_KEY, OLLAMA_MODEL_NAME, OLLAMA_MODEL_DESCRIPTION),
                new OcrModelDefinition.Capability(List.of(IMAGE_INPUT_TYPE), OLLAMA_GENERATE_PATH,
                        OLLAMA_GENERATE_PATH),
                new OcrModelDefinition.RuntimeDefaults(OLLAMA_DEFAULT_PORT, OLLAMA_PROVIDER_MODEL,
                        OLLAMA_CHANNEL_KEY, true)));
    }

    /**
     * 创建 OCR 节点管理服务依赖。
     *
     * @param modelRegistry OCR 模型注册表
     * @param nodeRepository OCR 节点仓储
     * @param idGenerator 标识生成器
     * @param nodePool OCR 运行时节点池
     * @return OCR 节点管理服务依赖
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Bean
    @ConditionalOnBean({
            OcrNodeRepository.class,
            IdGenerator.class,
            OcrRuntimeNodePool.class
    })
    @ConditionalOnMissingBean
    OcrNodeManagementDependencies ocrNodeManagementDependencies(
            OcrModelRegistry modelRegistry,
            OcrNodeRepository nodeRepository,
            IdGenerator idGenerator,
            OcrRuntimeNodePool nodePool
    ) {
        return new OcrNodeManagementDependencies(modelRegistry, nodeRepository, idGenerator, nodePool::refresh);
    }

    /**
     * 创建 OCR 节点管理服务。
     *
     * @param dependencies OCR 节点管理依赖
     * @return OCR 节点管理服务
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Bean
    @ConditionalOnBean(OcrNodeManagementDependencies.class)
    @ConditionalOnMissingBean
    OcrNodeManagementService ocrNodeManagementService(OcrNodeManagementDependencies dependencies) {
        return new OcrNodeManagementService(dependencies.modelRegistry(), dependencies.nodeRepository(),
                dependencies.idGenerator(), dependencies.nodePoolRefresher());
    }

    /**
     * OCR 节点管理服务依赖。
     *
     * @param modelRegistry OCR 模型注册表
     * @param nodeRepository OCR 节点仓储
     * @param idGenerator 标识生成器
     * @param nodePoolRefresher 节点池刷新端口
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    record OcrNodeManagementDependencies(
            OcrModelRegistry modelRegistry,
            OcrNodeRepository nodeRepository,
            IdGenerator idGenerator,
            OcrNodePoolRefresher nodePoolRefresher
    ) {
    }
}
