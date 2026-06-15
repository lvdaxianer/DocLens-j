package io.github.lvdaxianer.doclens.j.autoconfigure;

import io.github.lvdaxianer.doclens.j.shared.config.DocLensProperties;

/**
 * DocLens 自动配置公共转换支持。
 *
 * @author lvdaxianerplus
 * @date 2026-06-15
 */
final class DocLensConfigurationSupport {

    private DocLensConfigurationSupport() {
    }

    /**
     * 将 Spring 线程池属性转换为 core 线程池属性。
     *
     * @param properties Spring 线程池属性
     * @return core 线程池属性
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    static DocLensProperties.ThreadPoolsProperties threadPools(
            DocLensSpringProperties.ThreadPoolsProperties properties
    ) {
        return new DocLensProperties.ThreadPoolsProperties(
                threadPool(properties.documentProcessingThreadPool()),
                threadPool(properties.ocrRequestThreadPool()),
                threadPool(properties.ocrHealthThreadPool()),
                threadPool(properties.callbackThreadPool()));
    }

    /**
     * 将 Spring 单线程池属性转换为 core 单线程池属性。
     *
     * @param properties Spring 单线程池属性
     * @return core 单线程池属性
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    static DocLensProperties.ThreadPoolProperties threadPool(DocLensSpringProperties.ThreadPoolProperties properties) {
        return new DocLensProperties.ThreadPoolProperties(properties.coreSize(), properties.maxSize(),
                properties.queueCapacity(), properties.keepAliveSeconds(), properties.threadNamePrefix());
    }
}
