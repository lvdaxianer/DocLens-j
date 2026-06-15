package io.github.lvdaxianer.doclens.j.autoconfigure;

import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobRepository;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.CallbackJobMapper;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.CallbackDeliveryProcessor;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.CallbackDeliveryScheduler;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.CallbackDeliveryScheduler.Dependencies;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.CallbackDeliveryWorker;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.CallbackDeliveryWorker.Options;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.MybatisPlusCallbackJobRepository;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.JsonCodec;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.NamedThreadPoolFactory;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * 回调投递自动配置。
 *
 * @author lvdaxianerplus
 * @date 2026-06-15
 */
@AutoConfiguration(after = {DocLensAutoConfiguration.class, DocLensOcrThreadPoolAutoConfiguration.class})
@MapperScan(basePackageClasses = CallbackJobMapper.class, annotationClass = Mapper.class)
@Import(MybatisPlusCallbackJobRepository.class)
public class DocLensCallbackDeliveryAutoConfiguration {

    private static final int CALLBACK_DISPATCH_INTERVAL_MILLIS = 1000;
    private static final int CALLBACK_DISPATCH_BATCH_SIZE = 100;

    /**
     * 创建回调投递 worker。
     *
     * @param callbackJobRepository 回调任务仓储
     * @param jsonCodec JSON 编解码器
     * @param callbackExecutor 回调投递线程池
     * @param properties Spring 配置
     * @return 回调投递 worker
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    @Bean
    @ConditionalOnMissingBean
    CallbackDeliveryWorker callbackDeliveryWorker(
            CallbackDeliveryWorker.Dependencies dependencies,
            DocLensSpringProperties properties
    ) {
        return new CallbackDeliveryWorker(dependencies,
                new Options(Duration.ofSeconds(properties.callback().timeoutSeconds()), CALLBACK_DISPATCH_BATCH_SIZE));
    }

    /**
     * 创建回调投递 worker 依赖集合。
     *
     * @param callbackJobRepository 回调任务仓储
     * @param jsonCodec JSON 编解码器
     * @param callbackExecutor 回调投递线程池
     * @return 回调投递 worker 依赖集合
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    @Bean
    @ConditionalOnMissingBean
    CallbackDeliveryWorker.Dependencies callbackDeliveryWorkerDependencies(
            CallbackJobRepository callbackJobRepository,
            JsonCodec jsonCodec,
            @Qualifier("doclensCallbackExecutor") ExecutorService callbackExecutor
    ) {
        return new CallbackDeliveryWorker.Dependencies(callbackJobRepository,
                new CallbackDeliveryProcessor.Dependencies(callbackJobRepository, jsonCodec, callbackExecutor));
    }

    /**
     * 创建回调投递调度线程池。
     *
     * @return 回调投递调度线程池
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "doclensCallbackSchedulerExecutor")
    ScheduledExecutorService doclensCallbackSchedulerExecutor() {
        return Executors.newSingleThreadScheduledExecutor(new NamedThreadPoolFactory("doclens-callback-scheduler-"));
    }

    /**
     * 创建回调投递调度器。
     *
     * @param worker 回调投递 worker
     * @param schedulerExecutor 调度线程池
     * @param callbackExecutor 回调线程池
     * @return 回调投递调度器
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    @Bean
    @ConditionalOnMissingBean
    CallbackDeliveryScheduler callbackDeliveryScheduler(
            CallbackDeliveryWorker worker,
            @Qualifier("doclensCallbackSchedulerExecutor") ScheduledExecutorService schedulerExecutor,
            @Qualifier("doclensCallbackExecutor") ExecutorService callbackExecutor
    ) {
        return new CallbackDeliveryScheduler(new Dependencies(worker::runOnce, schedulerExecutor, callbackExecutor),
                CALLBACK_DISPATCH_INTERVAL_MILLIS);
    }

    /**
     * 应用启动后触发回调投递调度。
     *
     * @param scheduler 回调投递调度器
     * @return 启动任务
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    @Bean
    @ConditionalOnMissingBean(name = "callbackDeliverySchedulerRunner")
    ApplicationRunner callbackDeliverySchedulerRunner(CallbackDeliveryScheduler scheduler) {
        return args -> scheduler.start();
    }
}
