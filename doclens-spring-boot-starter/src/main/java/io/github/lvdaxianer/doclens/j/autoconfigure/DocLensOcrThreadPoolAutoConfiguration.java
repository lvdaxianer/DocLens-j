package io.github.lvdaxianer.doclens.j.autoconfigure;

import io.github.lvdaxianer.doclens.j.shared.infrastructure.NamedThreadPoolFactory;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * OCR 资源路由相关线程池隔离自动配置。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
@AutoConfiguration
@EnableConfigurationProperties(DocLensSpringProperties.class)
public class DocLensOcrThreadPoolAutoConfiguration {

    /**
     * 创建文档处理线程池。
     *
     * @param properties DocLens Spring 配置
     * @return 文档处理线程池
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "doclensDocumentProcessingExecutor")
    ExecutorService doclensDocumentProcessingExecutor(DocLensSpringProperties properties) {
        return executor(properties.threadPools().documentProcessingThreadPool());
    }

    /**
     * 创建 OCR 请求线程池。
     *
     * @param properties DocLens Spring 配置
     * @return OCR 请求线程池
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "doclensOcrRequestExecutor")
    ExecutorService doclensOcrRequestExecutor(DocLensSpringProperties properties) {
        return executor(properties.threadPools().ocrRequestThreadPool());
    }

    /**
     * 创建 OCR 健康检查线程池。
     *
     * @param properties DocLens Spring 配置
     * @return OCR 健康检查线程池
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "doclensOcrHealthExecutor")
    ExecutorService doclensOcrHealthExecutor(DocLensSpringProperties properties) {
        return executor(properties.threadPools().ocrHealthThreadPool());
    }

    /**
     * 创建回调投递线程池。
     *
     * @param properties DocLens Spring 配置
     * @return 回调投递线程池
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "doclensCallbackExecutor")
    ExecutorService doclensCallbackExecutor(DocLensSpringProperties properties) {
        return executor(properties.threadPools().callbackThreadPool());
    }

    /**
     * 根据属性创建线程池。
     *
     * @param properties 线程池属性
     * @return 线程池
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private ExecutorService executor(DocLensSpringProperties.ThreadPoolProperties properties) {
        int coreSize = positive(properties.coreSize());
        int maxSize = Math.max(coreSize, properties.maxSize());
        int queueCapacity = positive(properties.queueCapacity());
        int keepAliveSeconds = positive(properties.keepAliveSeconds());
        return new ThreadPoolExecutor(coreSize, maxSize, keepAliveSeconds, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity), new NamedThreadPoolFactory(properties.threadNamePrefix()),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * 将非正数收敛为一。
     *
     * @param value 配置值
     * @return 正整数
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private int positive(int value) {
        if (value > 0) {
            return value;
        } else {
            return 1;
        }
    }
}
