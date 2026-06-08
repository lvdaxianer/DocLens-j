package io.github.lvdaxianer.doclens.j.autoconfigure;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrCallIdGenerator;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrNodeImageExecutor;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrNodeSelector;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingDependencies;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingService;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingServiceProperties;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutePolicy;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.OcrRuntimeNodePool;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * OCR 资源路由自动配置。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
@AutoConfiguration(after = DocLensPaddleOcrAutoConfiguration.class)
@ConditionalOnBean(OcrNodeImageExecutor.class)
public class DocLensOcrResourceAutoConfiguration {

    /**
     * 创建 OCR 路由自动配置依赖。
     *
     * @param nodePool OCR 运行时节点池
     * @param nodeSelector OCR 节点选择器
     * @param nodeExecutor OCR 节点执行器
     * @param callRepository OCR 调用记录仓储
     * @param callIdGenerator OCR 调用记录 ID 生成器
     * @return OCR 路由自动配置依赖
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Bean
    @ConditionalOnMissingBean
    OcrRoutingAutoConfigurationDependencies ocrRoutingAutoConfigurationDependencies(
            OcrRuntimeNodePool nodePool,
            OcrNodeSelector nodeSelector,
            OcrNodeImageExecutor nodeExecutor,
            OcrNodeCallRepository callRepository,
            OcrCallIdGenerator callIdGenerator
    ) {
        return new OcrRoutingAutoConfigurationDependencies(nodePool, nodeSelector, nodeExecutor, callRepository,
                callIdGenerator);
    }

    /**
     * 创建 OCR 路由服务。
     *
     * @param dependencies OCR 路由自动配置依赖
     * @return OCR 路由服务
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Bean
    @ConditionalOnMissingBean
    OcrRoutingService ocrRoutingService(OcrRoutingAutoConfigurationDependencies dependencies) {
        return new OcrRoutingService(new OcrRoutingDependencies(dependencies.nodePool(), dependencies.nodeSelector(),
                dependencies.nodeExecutor(), dependencies.callRepository(), dependencies.callIdGenerator(),
                new OcrRoutingServiceProperties(OcrRoutePolicy.globalLoadBalance("least-inflight"), 3, false)));
    }

    /**
     * OCR 路由自动配置依赖。
     *
     * @param nodePool OCR 运行时节点池
     * @param nodeSelector OCR 节点选择器
     * @param nodeExecutor OCR 节点执行器
     * @param callRepository OCR 调用记录仓储
     * @param callIdGenerator OCR 调用记录 ID 生成器
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    record OcrRoutingAutoConfigurationDependencies(
            OcrRuntimeNodePool nodePool,
            OcrNodeSelector nodeSelector,
            OcrNodeImageExecutor nodeExecutor,
            OcrNodeCallRepository callRepository,
            OcrCallIdGenerator callIdGenerator
    ) {
    }
}
