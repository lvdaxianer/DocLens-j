package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrHealthGovernance;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * OCR 健康检查节点状态更新器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
final class OcrHealthNodeStateUpdater {

    private final Supplier<OcrHealthGovernance> governanceSupplier;

    /**
     * 创建 OCR 健康检查节点状态更新器。
     *
     * @param governanceSupplier OCR 治理配置提供器
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    OcrHealthNodeStateUpdater(Supplier<OcrHealthGovernance> governanceSupplier) {
        this.governanceSupplier = governanceSupplier;
    }

    /**
     * 根据健康成功结果更新节点。
     *
     * @param node OCR 节点
     * @param now 当前时间
     * @return 更新后的 OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    OcrNode successNode(OcrNode node, OffsetDateTime now) {
        if (node.status() == OcrNodeStatus.DOWN) {
            // DOWN 节点首次探测成功后进入恢复中，避免立即参与满量调度。
            return replaceHealth(node, OcrNodeStatus.RECOVERING, 0L, 1L, Optional.empty(), Optional.empty(), now);
        } else if (node.status() == OcrNodeStatus.RECOVERING && reachedRecoveryThreshold(node)) {
            // 恢复中节点连续成功达标后才恢复 UP。
            return replaceHealth(node, OcrNodeStatus.UP, 0L, 0L, Optional.empty(), Optional.empty(), now);
        } else {
            // 其他健康节点保留原状态并累加成功次数。
            return replaceHealth(node, node.status(), 0L, node.successCount() + 1, Optional.empty(),
                    node.circuitOpenUntil(), now);
        }
    }

    /**
     * 根据健康失败结果更新节点。
     *
     * @param node OCR 节点
     * @param errorMessage 错误消息
     * @param now 当前时间
     * @return 更新后的 OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    OcrNode failedNode(OcrNode node, String errorMessage, OffsetDateTime now) {
        OcrHealthGovernance governance = governance();
        long failureCount = node.failureCount() + 1;
        OcrNodeStatus status = failureStatus(failureCount, node, governance);
        Optional<OffsetDateTime> circuitOpenUntil = circuitOpenUntil(node, failureCount, governance, now);
        return replaceHealth(node, status, failureCount, 0L, Optional.of(errorMessage), circuitOpenUntil, now);
    }

    /**
     * 计算健康失败后的节点状态。
     *
     * @param failureCount 失败次数
     * @param node OCR 节点
     * @param governance 治理配置
     * @return 节点状态
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private OcrNodeStatus failureStatus(long failureCount, OcrNode node, OcrHealthGovernance governance) {
        if (failureCount >= Math.max(1, governance.failureThreshold())) {
            // 连续失败达到阈值后进入 DOWN，避免继续调度。
            return OcrNodeStatus.DOWN;
        } else {
            // 未达到失败阈值时维持当前状态。
            return node.status();
        }
    }

    /**
     * 计算失败后的熔断截止时间。
     *
     * @param node OCR 节点
     * @param failureCount 失败次数
     * @param governance 治理配置
     * @param now 当前时间
     * @return 熔断截止时间
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private Optional<OffsetDateTime> circuitOpenUntil(
            OcrNode node,
            long failureCount,
            OcrHealthGovernance governance,
            OffsetDateTime now
    ) {
        if (failureCount >= Math.max(1, governance.failureThreshold())) {
            // 到达失败阈值后刷新熔断窗口。
            return Optional.of(now.plusSeconds(governance.circuitOpenSeconds()));
        } else {
            // 未达到失败阈值时保留原熔断窗口。
            return node.circuitOpenUntil();
        }
    }

    /**
     * 判断是否达到恢复成功阈值。
     *
     * @param node OCR 节点
     * @return 是否达到恢复成功阈值
     * @author lvdaxianerplus
     * @date 2026-06-11
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
     * @param circuitOpenUntil 熔断截止时间
     * @param now 当前时间
     * @return 更新后的 OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-11
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
     * 读取当前生效的 OCR 治理配置。
     *
     * @return 当前治理配置
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private OcrHealthGovernance governance() {
        return governanceSupplier.get();
    }

    /**
     * 生成最近成功时间。
     *
     * @param successCount 成功次数
     * @param now 当前时间
     * @return 最近成功时间
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private Optional<OffsetDateTime> successAt(long successCount, OffsetDateTime now) {
        if (successCount > 0) {
            // 成功计数大于 0 时记录最近成功时间。
            return Optional.of(now);
        } else {
            // 状态转 UP 会清零恢复计数，此时不刷新最近成功时间。
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
     * @date 2026-06-11
     */
    private Optional<OffsetDateTime> failureAt(long failureCount, OffsetDateTime now) {
        if (failureCount > 0) {
            // 失败计数大于 0 时记录最近失败时间。
            return Optional.of(now);
        } else {
            // 健康成功会清零失败计数，此时清空最近失败时间。
            return Optional.empty();
        }
    }
}
