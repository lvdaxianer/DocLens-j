package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeDeploymentType;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrHealthGovernance;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import java.time.OffsetDateTime;
import java.util.function.Supplier;
import java.util.List;
import java.util.Optional;
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
            return false;
        }
        boolean healthy = isHealthy(node);
        if (healthy) {
            nodeRepository.update(successNode(node, now));
        } else {
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
        if (node.deploymentType() == OcrNodeDeploymentType.ONLINE) {
            return onlineConfigurationReady(node);
        } else {
            return healthClient.isHealthy(node);
        }
    }

    /**
     * 校验在线节点配置是否足以参与调度。
     *
     * @param node OCR 节点
     * @return 配置是否完整
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private boolean onlineConfigurationReady(OcrNode node) {
        return node.channelKey().isPresent()
                && node.providerModel().isPresent()
                && node.credentialConfigured()
                && node.credentialRef().isPresent();
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
        return tasks.stream().map(healthExecutor::submit).toList();
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
        if (node.status() == OcrNodeStatus.DOWN) {
            return replaceHealth(node, OcrNodeStatus.RECOVERING, 0L, 1L, Optional.empty(), Optional.empty(), now);
        } else if (node.status() == OcrNodeStatus.RECOVERING && reachedRecoveryThreshold(node)) {
            return replaceHealth(node, OcrNodeStatus.UP, 0L, 0L, Optional.empty(), Optional.empty(), now);
        } else {
            return replaceHealth(node, node.status(), 0L, node.successCount() + 1, Optional.empty(),
                    node.circuitOpenUntil(), now);
        }
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
        OcrHealthGovernance governance = governance();
        long failureCount = node.failureCount() + 1;
        OcrNodeStatus status = failureStatus(failureCount, node, governance);
        LOGGER.warn("[OCR健康检查] 节点健康检查失败, nodeId={}, modelKey={}, error={}",
                node.id(), node.modelKey(), errorMessage);
        Optional<OffsetDateTime> circuitOpenUntil = failureCount >= Math.max(1, governance.failureThreshold())
                ? Optional.of(now.plusSeconds(governance.circuitOpenSeconds()))
                : node.circuitOpenUntil();
        return replaceHealth(node, status, failureCount, 0L, Optional.of(errorMessage), circuitOpenUntil, now);
    }

    /**
     * 计算健康成功后的节点状态。
     *
     * @param node OCR 节点
     * @return 节点状态
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private OcrNodeStatus failureStatus(long failureCount, OcrNode node, OcrHealthGovernance governance) {
        if (failureCount >= Math.max(1, governance.failureThreshold())) {
            return OcrNodeStatus.DOWN;
        } else {
            return node.status();
        }
    }

    /**
     * 判断是否达到恢复成功阈值。
     *
     * @param node OCR 节点
     * @return 是否达到恢复成功阈值
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private boolean reachedRecoveryThreshold(OcrNode node) {
        return node.successCount() + 1 >= Math.max(1, governance().recoverySuccessThreshold());
    }

    /**
     * 替换节点健康状态字段。
     *
     * @param node OCR 节点
     * @param status 节点状态
     * @param failureCount 失败次数
     * @param successCount 成功次数
     * @param errorMessage 错误消息
     * @return 更新后的 OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private OcrNode replaceHealth(
            OcrNode node,
            OcrNodeStatus status,
            long failureCount,
            long successCount,
            Optional<String> errorMessage,
            Optional<OffsetDateTime> circuitOpenUntil,
            OffsetDateTime now
    ) {
        return new OcrNode(node.id(), node.modelKey(), node.deploymentType(), node.name(), node.host(), node.port(),
                node.channelKey(), node.providerModel(), node.credentialRef(), node.credentialConfigured(),
                node.enabled(), node.participateGlobal(), node.weight(), node.maxConcurrency(), status, failureCount,
                successCount, node.avgLatencyMs(), node.p95LatencyMs(), Optional.of(now), successAt(successCount, now),
                failureAt(failureCount, now), errorMessage, circuitOpenUntil, node.lastManualRecoveryAt(),
                node.createdAt(), now);
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
        if (ignoreCircuitWindow) {
            return true;
        } else {
            return node.circuitOpenUntil().isEmpty() || !node.circuitOpenUntil().get().isAfter(now);
        }
    }

    /**
     * 读取当前生效的 OCR 治理配置。
     *
     * @return 当前治理配置
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private OcrHealthGovernance governance() {
        return governanceSupplier.get();
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

    /**
     * 生成最近成功时间。
     *
     * @param successCount 成功次数
     * @param now 当前时间
     * @return 最近成功时间
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private Optional<OffsetDateTime> successAt(long successCount, OffsetDateTime now) {
        if (successCount > 0) {
            return Optional.of(now);
        } else {
            return Optional.empty();
        }
    }

    /**
     * 生成最近失败时间。
     *
     * @param failureCount 失败次数
     * @param now 当前时间
     * @return 最近失败时间
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private Optional<OffsetDateTime> failureAt(long failureCount, OffsetDateTime now) {
        if (failureCount > 0) {
            return Optional.of(now);
        } else {
            return Optional.empty();
        }
    }
}
