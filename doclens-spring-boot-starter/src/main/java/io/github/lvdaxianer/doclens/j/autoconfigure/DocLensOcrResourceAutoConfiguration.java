package io.github.lvdaxianer.doclens.j.autoconfigure;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrCallIdGenerator;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrBatchHitTracker;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrDispatchCoordinator;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrGovernanceConfigService;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrNodeImageExecutor;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrNodeSelector;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrPendingRequestQueue;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingDependencies;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingService;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingServiceProperties;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrHealthGovernance;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrGovernanceConfigRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutePolicy;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutingMode;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.DashScopeOnlineOcrClient;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.InMemoryOcrBatchHitTracker;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.OcrHealthCheckProperties;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.OcrHealthCheckScheduler;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.OcrHealthChecker;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.OcrHealthClient;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.OcrManualRecoveryService;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.OcrRuntimeNodePool;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.PaddleOcrHealthClient;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.RoutingOcrHealthClient;
import io.github.lvdaxianer.doclens.j.shared.config.DocLensProperties;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.NamedThreadPoolFactory;
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
 * OCR 资源路由自动配置。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
@AutoConfiguration(after = DocLensPaddleOcrAutoConfiguration.class)
public class DocLensOcrResourceAutoConfiguration {

    /**
     * 创建 OCR 路由自动配置依赖。
     *
     * @param nodePool OCR 运行时节点池
     * @param nodeSelector OCR 节点选择器
     * @param nodeExecutor OCR 节点执行器
     * @param callRepository OCR 调用记录仓储
     * @param callIdGenerator OCR 调用记录 ID 生成器
     * @param batchHitTracker 批次运行时命中跟踪器
     * @return OCR 路由自动配置依赖
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Bean
    @ConditionalOnBean(OcrNodeImageExecutor.class)
    @ConditionalOnMissingBean
    OcrRoutingAutoConfigurationDependencies ocrRoutingAutoConfigurationDependencies(
            OcrRuntimeNodePool nodePool,
            OcrNodeSelector nodeSelector,
            OcrPendingRequestQueue pendingRequestQueue,
            OcrNodeImageExecutor nodeExecutor,
            OcrNodeCallRepository callRepository,
            OcrCallIdGenerator callIdGenerator,
            OcrBatchHitTracker batchHitTracker
    ) {
        return new OcrRoutingAutoConfigurationDependencies(nodePool, nodeSelector,
                new OcrDispatchCoordinator(nodePool, nodeSelector, pendingRequestQueue), nodeExecutor,
                callRepository, callIdGenerator, batchHitTracker);
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
    @ConditionalOnBean(OcrRoutingAutoConfigurationDependencies.class)
    @ConditionalOnMissingBean
    OcrRoutingService ocrRoutingService(
            OcrRoutingAutoConfigurationDependencies dependencies,
            DocLensSpringProperties properties
    ) {
        return new OcrRoutingService(new OcrRoutingDependencies(dependencies.nodePool(),
                dependencies.nodeSelector(), dependencies.dispatchCoordinator(), dependencies.nodeExecutor(),
                dependencies.callRepository(), dependencies.callIdGenerator(), dependencies.batchHitTracker(),
                routingProperties(properties.ocr())));
    }

    /**
     * 创建进程内批次 OCR 命中跟踪器。
     *
     * @return 批次 OCR 命中跟踪器
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Bean
    @ConditionalOnMissingBean
    OcrBatchHitTracker ocrBatchHitTracker() {
        return new InMemoryOcrBatchHitTracker();
    }

    /**
     * 创建 PaddleOCR 健康检查客户端。
     *
     * @param properties DocLens 配置
     * @return OCR 健康检查客户端
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Bean
    @ConditionalOnBean(DashScopeOnlineOcrClient.class)
    @ConditionalOnMissingBean
    OcrHealthClient ocrHealthClient(
            DocLensSpringProperties properties,
            DashScopeOnlineOcrClient onlineOcrClient
    ) {
        return new RoutingOcrHealthClient(new PaddleOcrHealthClient(properties.ocr().healthCheckTimeoutSeconds()),
                onlineOcrClient);
    }

    /**
     * 创建 OCR 健康检查器。
     *
     * @param nodeRepository OCR 节点仓储
     * @param healthClient OCR 健康检查客户端
     * @param healthExecutor OCR 健康检查线程池
     * @param properties DocLens 配置
     * @return OCR 健康检查器
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Bean
    @ConditionalOnBean(OcrGovernanceConfigRepository.class)
    @ConditionalOnMissingBean
    OcrGovernanceConfigService ocrGovernanceConfigService(
            OcrGovernanceConfigRepository repository,
            DocLensSpringProperties properties
    ) {
        return new OcrGovernanceConfigService(repository, new OcrHealthGovernance(properties.ocr().failureThreshold(),
                properties.ocr().probeIntervalSeconds(), properties.ocr().circuitOpenSeconds(),
                properties.ocr().recoverySuccessThreshold(), properties.ocr().manualRecoveryAttempts()));
    }

    /**
     * 创建 OCR 健康检查器。
     *
     * @param nodeRepository OCR 节点仓储
     * @param healthClient OCR 健康检查客户端
     * @param healthExecutor OCR 健康检查线程池
     * @param governanceConfigService OCR 全局治理配置服务
     * @return OCR 健康检查器
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Bean
    @ConditionalOnBean({OcrNodeRepository.class, OcrHealthClient.class, OcrGovernanceConfigService.class})
    @ConditionalOnMissingBean
    OcrHealthChecker ocrHealthChecker(
            OcrNodeRepository nodeRepository,
            OcrHealthClient healthClient,
            @Qualifier("doclensOcrHealthExecutor") ExecutorService healthExecutor,
            OcrGovernanceConfigService governanceConfigService
    ) {
        return new OcrHealthChecker(nodeRepository, healthClient, healthExecutor,
                governanceConfigService::currentGovernance, java.time.OffsetDateTime::now);
    }

    /**
     * 创建 OCR 健康检查调度线程池。
     *
     * @return OCR 健康检查调度线程池
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "doclensOcrHealthSchedulerExecutor")
    ScheduledExecutorService doclensOcrHealthSchedulerExecutor() {
        return Executors.newSingleThreadScheduledExecutor(new NamedThreadPoolFactory("doclens-ocr-health-scheduler-"));
    }

    /**
     * 创建 OCR 健康检查周期调度器。
     *
     * @param healthChecker OCR 健康检查器
     * @param nodePool OCR 运行时节点池
     * @param schedulerExecutor OCR 健康检查调度线程池
     * @param properties DocLens 配置
     * @return OCR 健康检查周期调度器
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Bean
    @ConditionalOnBean({OcrHealthChecker.class, OcrRuntimeNodePool.class, OcrGovernanceConfigService.class})
    @ConditionalOnMissingBean
    OcrHealthCheckScheduler ocrHealthCheckScheduler(
            OcrHealthChecker healthChecker,
            OcrRuntimeNodePool nodePool,
            @Qualifier("doclensOcrHealthSchedulerExecutor") ScheduledExecutorService schedulerExecutor,
            OcrGovernanceConfigService governanceConfigService
    ) {
        return new OcrHealthCheckScheduler(healthChecker, nodePool, schedulerExecutor,
                () -> governanceConfigService.currentGovernance().probeIntervalSeconds());
    }

    /**
     * 应用启动完成后启动 OCR 健康检查调度。
     *
     * @param scheduler OCR 健康检查调度器
     * @return 应用启动任务
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Bean
    @ConditionalOnBean(OcrHealthCheckScheduler.class)
    @ConditionalOnMissingBean(name = "ocrHealthCheckSchedulerRunner")
    ApplicationRunner ocrHealthCheckSchedulerRunner(OcrHealthCheckScheduler scheduler) {
        return args -> scheduler.start();
    }

    /**
     * 创建 OCR 节点手动恢复服务。
     *
     * @param nodeRepository OCR 节点仓储
     * @param healthChecker OCR 健康检查器
     * @param properties DocLens 配置
     * @return OCR 节点手动恢复服务
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Bean
    @ConditionalOnBean({OcrNodeRepository.class, OcrHealthChecker.class, OcrGovernanceConfigService.class})
    @ConditionalOnMissingBean
    OcrManualRecoveryService ocrManualRecoveryService(
            OcrNodeRepository nodeRepository,
            OcrHealthChecker healthChecker,
            OcrGovernanceConfigService governanceConfigService
    ) {
        return new OcrManualRecoveryService(nodeRepository, healthChecker,
                governanceConfigService::currentGovernance, java.time.OffsetDateTime::now);
    }

    /**
     * 创建 OCR 路由服务配置。
     *
     * @param properties OCR 配置属性
     * @return OCR 路由服务配置
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private OcrRoutingServiceProperties routingProperties(DocLensSpringProperties.OcrProperties properties) {
        return new OcrRoutingServiceProperties(defaultPolicy(properties), properties.idleFactor(),
                properties.weightFactor(), properties.topBucketThreshold(), properties.requestRetryTimes(),
                new OcrHealthGovernance(properties.failureThreshold(), properties.probeIntervalSeconds(),
                        properties.circuitOpenSeconds(), properties.recoverySuccessThreshold(),
                        properties.manualRecoveryAttempts()),
                properties.specificNodeFallbackEnabled());
    }

    /**
     * 创建默认 OCR 路由策略。
     *
     * @param properties OCR 配置属性
     * @return 默认 OCR 路由策略
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private OcrRoutePolicy defaultPolicy(DocLensSpringProperties.OcrProperties properties) {
        if (properties.defaultRoutingMode() == OcrRoutingMode.DEFAULT) {
            return OcrRoutePolicy.defaultPolicy();
        } else {
            return OcrRoutePolicy.globalLoadBalance(properties.loadBalanceStrategy());
        }
    }

    /**
     * OCR 路由自动配置依赖。
     *
     * @param nodePool OCR 运行时节点池
     * @param nodeSelector OCR 节点选择器
     * @param dispatchCoordinator OCR 同步派发协调器
     * @param nodeExecutor OCR 节点执行器
     * @param callRepository OCR 调用记录仓储
     * @param callIdGenerator OCR 调用记录 ID 生成器
     * @param batchHitTracker 批次运行时命中跟踪器
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    record OcrRoutingAutoConfigurationDependencies(
            OcrRuntimeNodePool nodePool,
            OcrNodeSelector nodeSelector,
            OcrDispatchCoordinator dispatchCoordinator,
            OcrNodeImageExecutor nodeExecutor,
            OcrNodeCallRepository callRepository,
            OcrCallIdGenerator callIdGenerator,
            OcrBatchHitTracker batchHitTracker
    ) {
    }
}
