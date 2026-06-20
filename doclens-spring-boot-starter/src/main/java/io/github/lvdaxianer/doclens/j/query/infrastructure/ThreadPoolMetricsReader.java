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

    /** 活跃线程数指标键。 */
    private static final String ACTIVE_COUNT_KEY = "active_count";
    /** 队列大小指标键。 */
    private static final String QUEUE_SIZE_KEY = "queue_size";
    /** 当前线程池大小指标键。 */
    private static final String POOL_SIZE_KEY = "pool_size";
    /** 核心线程池大小指标键。 */
    private static final String CORE_POOL_SIZE_KEY = "core_pool_size";
    /** 最大线程池大小指标键。 */
    private static final String MAXIMUM_POOL_SIZE_KEY = "maximum_pool_size";
    /** 历史最大线程池大小指标键。 */
    private static final String LARGEST_POOL_SIZE_KEY = "largest_pool_size";
    /** 已完成任务数指标键。 */
    private static final String COMPLETED_TASK_COUNT_KEY = "completed_task_count";

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
                Map.entry(ACTIVE_COUNT_KEY, executor.getActiveCount()),
                Map.entry(QUEUE_SIZE_KEY, executor.getQueue().size()),
                Map.entry(POOL_SIZE_KEY, executor.getPoolSize()),
                Map.entry(CORE_POOL_SIZE_KEY, executor.getCorePoolSize()),
                Map.entry(MAXIMUM_POOL_SIZE_KEY, executor.getMaximumPoolSize()),
                Map.entry(LARGEST_POOL_SIZE_KEY, executor.getLargestPoolSize()),
                Map.entry(COMPLETED_TASK_COUNT_KEY, executor.getCompletedTaskCount())
        );
    }

    /**
     * 创建空线程池指标。
     *
     * @return 空线程池指标
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public Map<String, Object> emptyMetrics() {
        return Map.ofEntries(
                Map.entry(ACTIVE_COUNT_KEY, 0),
                Map.entry(QUEUE_SIZE_KEY, 0),
                Map.entry(POOL_SIZE_KEY, 0),
                Map.entry(CORE_POOL_SIZE_KEY, 0),
                Map.entry(MAXIMUM_POOL_SIZE_KEY, 0),
                Map.entry(LARGEST_POOL_SIZE_KEY, 0),
                Map.entry(COMPLETED_TASK_COUNT_KEY, 0L)
        );
    }
}
