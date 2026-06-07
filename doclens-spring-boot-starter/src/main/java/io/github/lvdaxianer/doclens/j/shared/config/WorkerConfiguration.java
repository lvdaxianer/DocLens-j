package io.github.lvdaxianer.doclens.j.shared.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 带业务专用线程名的 Worker 执行器配置。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@Configuration
public class WorkerConfiguration {

    private static final int CORE_POOL_SIZE = 2;
    private static final int MAX_POOL_SIZE = 4;
    private static final int QUEUE_CAPACITY = 100;

    /**
     * 创建 OCR 处理执行器。
     *
     * @return Worker 执行器
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Bean(name = "ocrWorkerExecutor")
    public Executor ocrWorkerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix("doclens-ocr-worker-");
        executor.initialize();
        return executor;
    }
}
