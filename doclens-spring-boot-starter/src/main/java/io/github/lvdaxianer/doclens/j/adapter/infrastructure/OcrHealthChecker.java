package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeDeploymentType;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
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
    private final OcrHealthCheckProperties properties;

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
        this.nodeRepository = nodeRepository;
        this.healthClient = healthClient;
        this.healthExecutor = healthExecutor;
        this.properties = properties;
    }

    /**
     * 执行一次 OCR 节点健康检查。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public void checkOnce() {
        List<Callable<Void>> tasks = nodeRepository.listEnabled().stream()
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
        boolean healthy = isHealthy(node);
        if (healthy) {
            nodeRepository.update(successNode(node));
        } else {
            nodeRepository.update(failedNode(node, "health check failed"));
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
    private Callable<Void> healthTask(OcrNode node) {
        return () -> {
            checkNode(node);
            return null;
        };
    }

    /**
     * 提交健康检查任务。
     *
     * @param tasks 健康检查任务
     * @return Future 集合
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private List<Future<Void>> submitTasks(List<Callable<Void>> tasks) {
        try {
            return healthExecutor.invokeAll(tasks);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OCR health check interrupted", ex);
        }
    }

    /**
     * 等待所有健康检查任务完成。
     *
     * @param futures Future 集合
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private void waitForTasks(List<Future<Void>> futures) {
        futures.forEach(this::waitForTask);
    }

    /**
     * 等待单个健康检查任务完成。
     *
     * @param future Future
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private void waitForTask(Future<Void> future) {
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
    private OcrNode successNode(OcrNode node) {
        OcrNodeStatus status = successStatus(node);
        return replaceHealth(node, status, 0L, node.successCount() + 1, Optional.empty());
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
    private OcrNode failedNode(OcrNode node, String errorMessage) {
        OcrNodeStatus status = failureStatus(node);
        LOGGER.warn("[OCR健康检查] 节点健康检查失败, nodeId={}, modelKey={}, error={}",
                node.id(), node.modelKey(), errorMessage);
        return replaceHealth(node, status, node.failureCount() + 1, 0L, Optional.of(errorMessage));
    }

    /**
     * 计算健康成功后的节点状态。
     *
     * @param node OCR 节点
     * @return 节点状态
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private OcrNodeStatus successStatus(OcrNode node) {
        if (node.status() == OcrNodeStatus.DOWN) {
            return OcrNodeStatus.RECOVERING;
        } else if (node.status() == OcrNodeStatus.RECOVERING && reachedRecoveryThreshold(node)) {
            return OcrNodeStatus.UP;
        } else {
            return node.status();
        }
    }

    /**
     * 计算健康失败后的节点状态。
     *
     * @param node OCR 节点
     * @return 节点状态
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private OcrNodeStatus failureStatus(OcrNode node) {
        if (node.failureCount() + 1 >= Math.max(1, properties.healthFailureThreshold())) {
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
        return node.successCount() + 1 >= Math.max(1, properties.recoverySuccessThreshold());
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
            Optional<String> errorMessage
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        return new OcrNode(node.id(), node.modelKey(), node.deploymentType(), node.name(), node.host(), node.port(),
                node.channelKey(), node.providerModel(), node.credentialRef(), node.credentialConfigured(),
                node.enabled(), node.participateGlobal(), node.weight(), node.maxConcurrency(), status, failureCount,
                successCount, node.avgLatencyMs(), node.p95LatencyMs(), Optional.of(now), successAt(successCount, now),
                failureAt(failureCount, now), errorMessage, node.createdAt(), now);
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
