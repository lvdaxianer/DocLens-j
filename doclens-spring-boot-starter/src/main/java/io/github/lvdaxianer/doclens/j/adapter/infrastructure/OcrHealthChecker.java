package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrHealthGovernance;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import java.time.OffsetDateTime;
import java.util.function.Supplier;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OCR 节点健康检查器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public class OcrHealthChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(OcrHealthChecker.class);

    private final OcrNodeRepository nodeRepository;
    private final OcrHealthClient healthClient;
    private final ExecutorService healthExecutor;
    private final Supplier<OcrHealthGovernance> governanceSupplier;
    private final Supplier<OffsetDateTime> nowSupplier;
    private final OcrHealthNodeStateUpdater stateUpdater;

    /**
     * 创建 OCR 节点健康检查器。
     *
     * @param nodeRepository OCR 节点仓储
     * @param healthClient OCR 健康检查客户端
     * @param healthExecutor OCR 健康检查线程池
     * @param properties 健康检查阈值配置
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public OcrHealthChecker(
            OcrNodeRepository nodeRepository,
            OcrHealthClient healthClient,
            ExecutorService healthExecutor,
            OcrHealthCheckProperties properties
    ) {
        this(nodeRepository, healthClient, healthExecutor, () -> toGovernance(properties), OffsetDateTime::now);
    }

    /**
     * 创建带可控时钟的 OCR 节点健康检查器。
     *
     * @param nodeRepository OCR 节点仓储
     * @param healthClient OCR 健康检查客户端
     * @param healthExecutor OCR 健康检查线程池
     * @param properties 健康检查阈值配置
     * @param nowSupplier 当前时间提供器
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public OcrHealthChecker(
            OcrNodeRepository nodeRepository,
            OcrHealthClient healthClient,
            ExecutorService healthExecutor,
            OcrHealthCheckProperties properties,
            Supplier<OffsetDateTime> nowSupplier
    ) {
        this(nodeRepository, healthClient, healthExecutor, () -> toGovernance(properties), nowSupplier);
    }

    /**
     * 创建读取运行时治理配置的 OCR 健康检查器。
     *
     * @param nodeRepository OCR 节点仓储
     * @param healthClient OCR 健康检查客户端
     * @param healthExecutor OCR 健康检查线程池
     * @param governanceSupplier 当前治理配置提供器
     * @param nowSupplier 当前时间提供器
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public OcrHealthChecker(
            OcrNodeRepository nodeRepository,
            OcrHealthClient healthClient,
            ExecutorService healthExecutor,
            Supplier<OcrHealthGovernance> governanceSupplier,
            Supplier<OffsetDateTime> nowSupplier
    ) {
        this.nodeRepository = nodeRepository;
        this.healthClient = healthClient;
        this.healthExecutor = healthExecutor;
        this.governanceSupplier = governanceSupplier;
        this.nowSupplier = nowSupplier;
        this.stateUpdater = new OcrHealthNodeStateUpdater(governanceSupplier);
    }

    /**
     * 执行一次 OCR 节点健康检查。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public void checkOnce() {
        List<Runnable> tasks = nodeRepository.listEnabled().stream()
                .filter(node -> node.status() != OcrNodeStatus.DISABLED)
                .map(this::healthTask)
                .toList();
        waitForTasks(submitTasks(tasks));
    }

    /**
     * 执行单个 OCR 节点健康检查并更新状态。
     *
     * @param node OCR 节点
     * @return 节点是否健康
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public boolean checkNode(OcrNode node) {
        return checkNode(node, false);
    }

    /**
     * 执行一次忽略熔断窗口的手动健康探测。
     *
     * @param node OCR 节点
     * @return 节点是否健康
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public boolean checkNodeIgnoringCircuitWindow(OcrNode node) {
        return checkNode(node, true);
    }

    /**
     * 按指定模式执行单个 OCR 节点健康检查并更新状态。
     *
     * @param node OCR 节点
     * @param ignoreCircuitWindow 是否忽略熔断窗口
     * @return 节点是否健康
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private boolean checkNode(OcrNode node, boolean ignoreCircuitWindow) {
        OffsetDateTime now = nowSupplier.get();
        if (!shouldProbe(node, now, ignoreCircuitWindow)) {
            // 熔断窗口内跳过自动探测，避免反复打到异常节点。
            return false;
        }
        boolean healthy = isHealthy(node);
        if (healthy) {
            // 健康成功后按治理阈值推进恢复状态。
            nodeRepository.update(successNode(node, now));
        } else {
            // 健康失败后记录失败计数并可能打开熔断窗口。
            nodeRepository.update(failedNode(node, "health check failed", now));
        }
        return healthy;
    }

    /**
     * 按节点部署类型执行健康判断。
     *
     * @param node OCR 节点
     * @return 节点是否健康
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private boolean isHealthy(OcrNode node) {
        return healthClient.isHealthy(node);
    }

    /**
     * 创建单节点健康检查任务。
     *
     * @param node OCR 节点
     * @return 健康检查任务
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private Runnable healthTask(OcrNode node) {
        return () -> checkNode(node);
    }

    /**
     * 提交健康检查任务。
     *
     * @param tasks 健康检查任务
     * @return Future 集合
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private List<Future<?>> submitTasks(List<Runnable> tasks) {
        List<Future<?>> futures = new java.util.ArrayList<>(tasks.size());
        tasks.forEach(task -> futures.add(healthExecutor.submit(task)));
        return futures;
    }

    /**
     * 等待所有健康检查任务完成。
     *
     * @param futures Future 集合
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private void waitForTasks(List<Future<?>> futures) {
        futures.forEach(this::waitForTask);
    }

    /**
     * 等待单个健康检查任务完成。
     *
     * @param future Future
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private void waitForTask(Future<?> future) {
        try {
            future.get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OCR health check interrupted", ex);
        } catch (ExecutionException ex) {
            throw new IllegalStateException("OCR health check failed", ex);
        }
    }

    /**
     * 根据健康成功结果更新节点。
     *
     * @param node OCR 节点
     * @return 更新后的 OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private OcrNode successNode(OcrNode node, OffsetDateTime now) {
        return stateUpdater.successNode(node, now);
    }

    /**
     * 根据健康失败结果更新节点。
     *
     * @param node OCR 节点
     * @param errorMessage 错误消息
     * @return 更新后的 OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private OcrNode failedNode(OcrNode node, String errorMessage, OffsetDateTime now) {
        LOGGER.warn("[OCR健康检查] 节点健康检查失败, nodeId={}, modelKey={}, error={}",
                node.id(), node.modelKey(), errorMessage);
        return stateUpdater.failedNode(node, errorMessage, now);
    }

    /**
     * 判断当前是否应探测节点。
     *
     * @param node OCR 节点
     * @param now 当前时间
     * @return 是否应探测
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private boolean shouldProbe(OcrNode node, OffsetDateTime now, boolean ignoreCircuitWindow) {
        // 手动恢复探测需要绕过熔断窗口，立即验证用户刚修复的节点。
        if (ignoreCircuitWindow) {
            return true;
        // 已失败或恢复中的节点需要持续探测，避免服务恢复后仍被熔断窗口挡住。
        } else if (node.status() == OcrNodeStatus.DOWN || node.status() == OcrNodeStatus.RECOVERING) {
            return true;
        // 正常节点遵守熔断窗口，避免持续打爆异常服务。
        } else {
            return node.circuitOpenUntil().isEmpty() || !node.circuitOpenUntil().get().isAfter(now);
        }
    }

    /**
     * 将旧版健康检查属性适配为治理配置。
     *
     * @param properties 健康检查属性
     * @return 治理配置
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static OcrHealthGovernance toGovernance(OcrHealthCheckProperties properties) {
        return new OcrHealthGovernance(properties.healthFailureThreshold(),
                OcrHealthGovernance.DEFAULT_PROBE_INTERVAL_SECONDS, properties.circuitOpenSeconds(),
                properties.recoverySuccessThreshold(), OcrHealthGovernance.DEFAULT_MANUAL_RECOVERY_ATTEMPTS);
    }

}
