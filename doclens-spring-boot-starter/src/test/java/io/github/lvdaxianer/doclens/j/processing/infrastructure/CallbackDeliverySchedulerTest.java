package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.processing.infrastructure.CallbackDeliveryScheduler.Dependencies;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.NamedThreadPoolFactory;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * 回调投递调度器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-15
 */
class CallbackDeliverySchedulerTest {

    /**
     * 调度器启动后应提交一轮回调扫描。
     *
     * @throws InterruptedException 等待调度执行被中断
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    @Test
    void startsCallbackDeliveryScanOnCallbackExecutor() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger runCount = new AtomicInteger();
        ScheduledExecutorService schedulerExecutor = schedulerExecutor();
        ExecutorService callbackExecutor = callbackExecutor();

        try {
            Dependencies dependencies = new Dependencies(() -> {
                runCount.incrementAndGet();
                latch.countDown();
                return 1;
            }, schedulerExecutor, callbackExecutor);
            CallbackDeliveryScheduler scheduler = new CallbackDeliveryScheduler(dependencies, 1000);

            scheduler.start();

            assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(runCount).hasValue(1);
        } finally {
            schedulerExecutor.shutdownNow();
            callbackExecutor.shutdownNow();
        }
    }

    /**
     * 创建测试调度线程池。
     *
     * @return 调度线程池
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private ScheduledExecutorService schedulerExecutor() {
        return Executors.newSingleThreadScheduledExecutor(new NamedThreadPoolFactory("callback-scheduler-test-"));
    }

    /**
     * 创建测试回调线程池。
     *
     * @return 回调线程池
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private ExecutorService callbackExecutor() {
        return Executors.newSingleThreadExecutor(new NamedThreadPoolFactory("callback-worker-test-"));
    }
}
