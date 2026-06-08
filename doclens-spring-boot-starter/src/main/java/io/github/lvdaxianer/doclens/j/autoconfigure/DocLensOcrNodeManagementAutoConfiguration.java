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
        OcrModelDefinition paddleOcr = OcrModelDefinition.create(new OcrModelDefinition.CreateCommand(
                "paddle_ocr", "PaddleOCR", "PaddleOCR native-compatible HTTP API", List.of("image"),
                "/ocr", "/health", true));
        return new OcrModelRegistry(List.of(paddleOcr));
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
