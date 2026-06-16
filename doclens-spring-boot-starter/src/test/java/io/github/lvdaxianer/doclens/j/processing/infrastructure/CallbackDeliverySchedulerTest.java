package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.processing.infrastructure.CallbackDeliveryScheduler.Dependencies;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.NamedThreadPoolFactory;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
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

    private static final int CALLBACK_POOL_CORE_SIZE = 3;

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

        try {
            Dependencies dependencies = new Dependencies(() -> {
                runCount.incrementAndGet();
                latch.countDown();
                return 1;
            }, schedulerExecutor);
            CallbackDeliveryScheduler scheduler = new CallbackDeliveryScheduler(dependencies, 1000);

            scheduler.start();

            assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(runCount).hasValue(1);
        } finally {
            schedulerExecutor.shutdownNow();
        }
    }

    /**
     * 调度器不应在同一个单线程回调池里同步等待子投递任务。
     *
     * @throws InterruptedException 等待调度执行被中断
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    @Test
    void scheduledCallbackPoolRunsNestedDeliveryWithoutSelfBlocking() throws InterruptedException {
        CountDownLatch nestedDeliveryFinished = new CountDownLatch(1);
        ScheduledExecutorService callbackExecutor = callbackScheduledExecutor();

        try {
            Dependencies dependencies = new Dependencies(() -> {
                callbackExecutor.schedule(nestedDeliveryFinished::countDown, 0, TimeUnit.MILLISECONDS);
                return 1;
            }, callbackExecutor);
            CallbackDeliveryScheduler scheduler = new CallbackDeliveryScheduler(dependencies, 1000);

            scheduler.start();

            assertThat(nestedDeliveryFinished.await(1, TimeUnit.SECONDS)).isTrue();
        } finally {
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
     * 创建核心线程数为 3 的测试回调延迟调度池。
     *
     * @return 回调延迟调度池
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    private ScheduledExecutorService callbackScheduledExecutor() {
        return Executors.newScheduledThreadPool(CALLBACK_POOL_CORE_SIZE,
                new NamedThreadPoolFactory("callback-scheduled-test-"));
    }
}
