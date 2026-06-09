package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrHealthGovernance;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OCR 节点手动恢复服务。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public class OcrManualRecoveryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OcrManualRecoveryService.class);

    private final OcrNodeRepository nodeRepository;
    private final OcrHealthChecker healthChecker;
    private final Supplier<OcrHealthGovernance> governanceSupplier;
    private final Supplier<OffsetDateTime> nowSupplier;

    /**
     * 创建 OCR 节点手动恢复服务。
     *
     * @param nodeRepository OCR 节点仓储
     * @param healthChecker OCR 健康检查器
     * @param manualRecoveryAttempts 手动恢复尝试次数
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public OcrManualRecoveryService(
            OcrNodeRepository nodeRepository,
            OcrHealthChecker healthChecker,
            int manualRecoveryAttempts
    ) {
        this(nodeRepository, healthChecker, () -> new OcrHealthGovernance(
                OcrHealthGovernance.DEFAULT_FAILURE_THRESHOLD,
                OcrHealthGovernance.DEFAULT_PROBE_INTERVAL_SECONDS,
                OcrHealthGovernance.DEFAULT_CIRCUIT_OPEN_SECONDS,
                OcrHealthGovernance.DEFAULT_RECOVERY_SUCCESS_THRESHOLD,
                manualRecoveryAttempts), OffsetDateTime::now);
    }

    /**
     * 创建带可控时钟的 OCR 节点手动恢复服务。
     *
     * @param nodeRepository OCR 节点仓储
     * @param healthChecker OCR 健康检查器
     * @param manualRecoveryAttempts 手动恢复尝试次数
     * @param nowSupplier 当前时间提供器
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public OcrManualRecoveryService(
            OcrNodeRepository nodeRepository,
            OcrHealthChecker healthChecker,
            int manualRecoveryAttempts,
            Supplier<OffsetDateTime> nowSupplier
    ) {
        this(nodeRepository, healthChecker, () -> new OcrHealthGovernance(
                OcrHealthGovernance.DEFAULT_FAILURE_THRESHOLD,
                OcrHealthGovernance.DEFAULT_PROBE_INTERVAL_SECONDS,
                OcrHealthGovernance.DEFAULT_CIRCUIT_OPEN_SECONDS,
                OcrHealthGovernance.DEFAULT_RECOVERY_SUCCESS_THRESHOLD,
                manualRecoveryAttempts), nowSupplier);
    }

    /**
     * 创建读取运行时治理配置的 OCR 节点手动恢复服务。
     *
     * @param nodeRepository OCR 节点仓储
     * @param healthChecker OCR 健康检查器
     * @param governanceSupplier 当前治理配置提供器
     * @param nowSupplier 当前时间提供器
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public OcrManualRecoveryService(
            OcrNodeRepository nodeRepository,
            OcrHealthChecker healthChecker,
            Supplier<OcrHealthGovernance> governanceSupplier,
            Supplier<OffsetDateTime> nowSupplier
    ) {
        this.nodeRepository = nodeRepository;
        this.healthChecker = healthChecker;
        this.governanceSupplier = governanceSupplier;
        this.nowSupplier = nowSupplier;
    }

    /**
     * 执行有限次手动恢复探测并返回最终结果。
     *
     * @param node OCR 节点
     * @return 手动恢复结果
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public ManualRecoveryResult recover(OcrNode node) {
        OcrNode recoveryNode = markManualRecoveryStarted(node);
        int attempts = 0;
        boolean recovered = false;
        int maxAttempts = Math.max(1, governanceSupplier.get().manualRecoveryAttempts());
        LOGGER.info("[OCR手动恢复] 开始执行节点手动恢复, nodeId={}, attempts={}", node.id(), maxAttempts);
        while (attempts < maxAttempts && !recovered) {
            attempts++;
            boolean probeHealthy = healthChecker.checkNodeIgnoringCircuitWindow(recoveryNode);
            recoveryNode = reloadNode(node.id());
            recovered = isRecoveredHealthy(recoveryNode, probeHealthy);
        }
        if (!recovered) {
            LOGGER.warn("[OCR手动恢复] 节点手动恢复未达到可用阈值, nodeId={}, attempts={}, status={}",
                    node.id(), attempts, recoveryNode.status());
        } else {
            LOGGER.info("[OCR手动恢复] 节点手动恢复成功, nodeId={}, attempts={}", node.id(), attempts);
        }
        return new ManualRecoveryResult(recovered, attempts, recoveryNode);
    }

    /**
     * 标记手动恢复开始时间，并清理旧熔断窗口以允许进入恢复状态机。
     *
     * @param node OCR 节点
     * @return 更新后的 OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private OcrNode markManualRecoveryStarted(OcrNode node) {
        OffsetDateTime now = nowSupplier.get();
        OcrNode updated = node.updateHealthGovernance(node.failureCount(), node.successCount(),
                node.lastHealthAt(), node.lastFailureAt(), node.lastError(), Optional.empty(),
                Optional.of(now), now);
        nodeRepository.update(updated);
        return updated;
    }

    /**
     * 重新装载最新节点状态。
     *
     * @param nodeId 节点 ID
     * @return 最新 OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private OcrNode reloadNode(String nodeId) {
        return nodeRepository.findById(nodeId).orElseThrow(() -> new IllegalStateException("ocr node not found"));
    }

    /**
     * 判断节点是否已通过恢复阈值并重新可用。
     *
     * @param node 最新 OCR 节点
     * @param probeHealthy 本次探测是否成功
     * @return 是否恢复健康
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private boolean isRecoveredHealthy(OcrNode node, boolean probeHealthy) {
        return probeHealthy && node.status() == OcrNodeStatus.UP;
    }

    /**
     * 手动恢复结果。
     *
     * @param healthy 是否恢复健康
     * @param attempts 实际尝试次数
     * @param node 最新节点状态
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public record ManualRecoveryResult(boolean healthy, int attempts, OcrNode node) {
    }
}
