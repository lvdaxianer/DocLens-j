package io.github.lvdaxianer.doclens.j.autoconfigure;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrBatchHitTracker;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRunningPageTaskTracker;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrModelRegistry;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeRepository;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.OcrRuntimeNodePool;
import io.github.lvdaxianer.doclens.j.query.application.DashboardOcrMetricsProvider;
import io.github.lvdaxianer.doclens.j.query.application.EmptyDashboardOcrMetricsProvider;
import io.github.lvdaxianer.doclens.j.query.infrastructure.DashboardThreadPools;
import io.github.lvdaxianer.doclens.j.query.infrastructure.OcrDashboardAttributionSources;
import io.github.lvdaxianer.doclens.j.query.infrastructure.OcrDashboardDataSources;
import io.github.lvdaxianer.doclens.j.query.infrastructure.OcrDashboardMetricsProvider;
import io.github.lvdaxianer.doclens.j.query.infrastructure.OcrDashboardMetricsProviderDependencies;
import io.github.lvdaxianer.doclens.j.query.infrastructure.OcrDashboardRuntimeSources;
import java.util.Optional;
import org.springframework.context.ApplicationContext;

/**
 * Dashboard OCR 指标提供器工厂。
 *
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
final class DashboardOcrMetricsProviderFactory {

    private final ApplicationContext context;

    /**
     * 创建 Dashboard OCR 指标提供器工厂。
     *
     * @param context Spring 上下文
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    DashboardOcrMetricsProviderFactory(ApplicationContext context) {
        this.context = context;
    }

    /**
     * 创建 Dashboard OCR 指标提供器。
     *
     * @return Dashboard OCR 指标提供器
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    DashboardOcrMetricsProvider create() {
        return dependencies().<DashboardOcrMetricsProvider>map(OcrDashboardMetricsProvider::new)
                .orElseGet(EmptyDashboardOcrMetricsProvider::new);
    }

    /**
     * 解析 Dashboard OCR 指标依赖。
     *
     * @return 指标依赖集合
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    private Optional<OcrDashboardMetricsProviderDependencies> dependencies() {
        OcrDashboardDataSources dataSources = dataSources().orElse(null);
        OcrDashboardRuntimeSources runtimeSources = runtimeSources().orElse(null);
        OcrDashboardAttributionSources attributionSources = attributionSources().orElse(null);
        if (dataSources == null || runtimeSources == null || attributionSources == null) {
            // OCR 基础设施不完整时，Dashboard 使用空指标实现。
            return Optional.empty();
        } else {
            // OCR 基础设施齐全时，装配真实指标提供器。
            return Optional.of(new OcrDashboardMetricsProviderDependencies(dataSources, runtimeSources,
                    attributionSources));
        }
    }

    /**
     * 解析 OCR 查询侧仓储集合。
     *
     * @return 查询侧仓储集合
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    private Optional<OcrDashboardDataSources> dataSources() {
        OcrNodeRepository nodeRepository = optionalBean(OcrNodeRepository.class).orElse(null);
        OcrNodeCallRepository callRepository = optionalBean(OcrNodeCallRepository.class).orElse(null);
        if (nodeRepository == null || callRepository == null) {
            // 缺任一查询仓储时无法计算真实 OCR 指标。
            return Optional.empty();
        } else {
            // 两个查询仓储都存在时，返回真实数据源集合。
            return Optional.of(new OcrDashboardDataSources(nodeRepository, callRepository));
        }
    }

    /**
     * 解析 OCR 运行态依赖集合。
     *
     * @return 运行态依赖集合
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    private Optional<OcrDashboardRuntimeSources> runtimeSources() {
        OcrRuntimeNodePool nodePool = optionalBean(OcrRuntimeNodePool.class).orElse(null);
        DashboardThreadPools threadPools = optionalBean(DashboardThreadPools.class).orElse(null);
        if (nodePool == null || threadPools == null) {
            // 缺运行态组件时无法计算真实 OCR 指标。
            return Optional.empty();
        } else {
            // 运行态依赖齐全时，返回真实运行态集合。
            return Optional.of(new OcrDashboardRuntimeSources(nodePool, threadPools));
        }
    }

    /**
     * 解析 OCR 路由归因依赖集合。
     *
     * @return 路由归因依赖集合
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    private Optional<OcrDashboardAttributionSources> attributionSources() {
        OcrBatchHitTracker batchHitTracker = optionalBean(OcrBatchHitTracker.class).orElse(null);
        OcrRunningPageTaskTracker taskTracker = optionalBean(OcrRunningPageTaskTracker.class).orElse(null);
        OcrModelRegistry modelRegistry = optionalBean(OcrModelRegistry.class).orElse(null);
        if (batchHitTracker == null || taskTracker == null || modelRegistry == null) {
            // 缺归因组件时无法提供命中节点和运行中页任务指标。
            return Optional.empty();
        } else {
            // 路由归因组件齐全时，返回真实归因集合。
            return Optional.of(new OcrDashboardAttributionSources(batchHitTracker, taskTracker, modelRegistry));
        }
    }

    /**
     * 读取可选 Bean。
     *
     * @param beanType Bean 类型
     * @return 可选 Bean
     * @param <T> Bean 类型
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    private <T> Optional<T> optionalBean(Class<T> beanType) {
        String[] beanNames = context.getBeanNamesForType(beanType);
        if (beanNames.length == 0) {
            // 当前宿主未启用该类型 Bean。
            return Optional.empty();
        } else {
            // 交给 Spring 按类型解析，保留 @Primary 等多候选选择语义。
            return Optional.of(context.getBean(beanType));
        }
    }
}
