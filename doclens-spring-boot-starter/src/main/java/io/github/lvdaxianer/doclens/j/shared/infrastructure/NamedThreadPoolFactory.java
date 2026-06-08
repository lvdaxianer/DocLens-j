package io.github.lvdaxianer.doclens.j.shared.infrastructure;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 带业务前缀的线程工厂。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public class NamedThreadPoolFactory implements ThreadFactory {

    private final String threadNamePrefix;
    private final AtomicInteger sequence = new AtomicInteger(1);

    /**
     * 创建命名线程工厂。
     *
     * @param threadNamePrefix 线程名前缀
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public NamedThreadPoolFactory(String threadNamePrefix) {
        this.threadNamePrefix = threadNamePrefix;
    }

    /**
     * 创建带业务名称的守护线程。
     *
     * @param runnable 线程任务
     * @return 命名线程
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setName(threadNamePrefix + sequence.getAndIncrement());
        thread.setDaemon(true);
        return thread;
    }
}
