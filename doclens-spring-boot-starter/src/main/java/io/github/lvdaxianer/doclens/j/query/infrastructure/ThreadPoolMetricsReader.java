package io.github.lvdaxianer.doclens.j.query.infrastructure;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池指标读取器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public class ThreadPoolMetricsReader {

    /**
     * 读取线程池运行指标。
     *
     * @param executorService 线程池
     * @return 线程池指标
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public Map<String, Object> read(ExecutorService executorService) {
        if (executorService instanceof ThreadPoolExecutor threadPoolExecutor) {
            return threadPoolMetrics(threadPoolExecutor);
        } else {
            return emptyMetrics();
        }
    }

    /**
     * 读取 ThreadPoolExecutor 指标。
     *
     * @param executor 线程池执行器
     * @return 线程池指标
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private Map<String, Object> threadPoolMetrics(ThreadPoolExecutor executor) {
        return Map.ofEntries(
                Map.entry("active_count", executor.getActiveCount()),
                Map.entry("queue_size", executor.getQueue().size()),
                Map.entry("pool_size", executor.getPoolSize()),
                Map.entry("completed_task_count", executor.getCompletedTaskCount())
        );
    }

    /**
     * 创建空线程池指标。
     *
     * @return 空线程池指标
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private Map<String, Object> emptyMetrics() {
        return Map.ofEntries(
                Map.entry("active_count", 0),
                Map.entry("queue_size", 0),
                Map.entry("pool_size", 0),
                Map.entry("completed_task_count", 0L)
        );
    }
}
