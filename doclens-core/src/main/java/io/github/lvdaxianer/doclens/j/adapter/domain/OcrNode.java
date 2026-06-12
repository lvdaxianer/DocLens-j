package io.github.lvdaxianer.doclens.j.adapter.domain;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * OCR 服务节点领域对象。
 *
 * @param id 节点 ID
 * @param modelKey OCR 模型标识
 * @param deploymentType 节点部署类型
 * @param name 节点名称
 * @param host 节点主机
 * @param port 节点端口
 * @param channelKey 在线渠道标识
 * @param providerModel 在线模型名称
 * @param credentialRef 在线凭证引用
 * @param credentialConfigured 是否已配置在线凭证
 * @param enabled 是否启用
 * @param participateGlobal 是否参与全局负载均衡
 * @param weight 节点权重
 * @param maxConcurrency 最大并发图片数
 * @param status 节点状态
 * @param failureCount 失败次数
 * @param successCount 成功次数
 * @param avgLatencyMs 平均耗时
 * @param p95LatencyMs P95 耗时
 * @param lastHealthAt 最近健康检查时间
 * @param lastSuccessAt 最近成功时间
 * @param lastFailureAt 最近失败时间
 * @param lastError 最近错误
 * @param circuitOpenUntil 熔断结束时间
 * @param lastManualRecoveryAt 最近手动恢复时间
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public record OcrNode(
        String id,
        String modelKey,
        OcrNodeDeploymentType deploymentType,
        String name,
        String host,
        int port,
        Optional<String> channelKey,
        Optional<String> providerModel,
        Optional<String> credentialRef,
        boolean credentialConfigured,
        boolean enabled,
        boolean participateGlobal,
        int weight,
        int maxConcurrency,
        OcrNodeStatus status,
        long failureCount,
        long successCount,
        long avgLatencyMs,
        long p95LatencyMs,
        Optional<OffsetDateTime> lastHealthAt,
        Optional<OffsetDateTime> lastSuccessAt,
        Optional<OffsetDateTime> lastFailureAt,
        Optional<String> lastError,
        Optional<OffsetDateTime> circuitOpenUntil,
        Optional<OffsetDateTime> lastManualRecoveryAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    /**
     * 创建带安全默认值的 OCR 节点。
     *
     * @param id 节点 ID
     * @param modelKey OCR 模型标识
     * @param deploymentType 节点部署类型
     * @param name 节点名称
     * @param host 节点主机
     * @param port 节点端口
     * @param channelKey 在线渠道标识
     * @param providerModel 在线模型名称
     * @param credentialRef 在线凭证引用
     * @param credentialConfigured 是否已配置在线凭证
     * @param enabled 是否启用
     * @param participateGlobal 是否参与全局负载均衡
     * @param weight 节点权重
     * @param maxConcurrency 最大并发图片数
     * @param status 节点状态
     * @param failureCount 失败次数
     * @param successCount 成功次数
     * @param avgLatencyMs 平均耗时
     * @param p95LatencyMs P95 耗时
     * @param lastHealthAt 最近健康检查时间
     * @param lastSuccessAt 最近成功时间
     * @param lastFailureAt 最近失败时间
     * @param lastError 最近错误
     * @param createdAt 创建时间
     * @param updatedAt 更新时间
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public OcrNode {
        id = OcrNodeNormalization.requiredText(id, "ocr node id is required");
        modelKey = OcrNodeNormalization.requiredText(modelKey, "ocr model key is required");
        deploymentType = deploymentType == null ? OcrNodeDeploymentType.OFFLINE : deploymentType;
        name = OcrNodeNormalization.requiredText(name, "ocr node name is required");
        host = OcrNodeNormalization.normalizeHost(deploymentType, host);
        port = OcrNodeNormalization.normalizePort(deploymentType, port);
        channelKey = OcrNodeNormalization.normalizeChannel(deploymentType, channelKey);
        providerModel = OcrNodeNormalization.normalizeProviderModel(deploymentType, channelKey, providerModel);
        credentialRef = OcrNodeNormalization.normalize(credentialRef);
        credentialConfigured = credentialConfigured || credentialRef.isPresent();
        OcrNodeNormalization.validatePositive(weight, "ocr node weight must be greater than 0");
        OcrNodeNormalization.validatePositive(maxConcurrency, "ocr node max concurrency must be greater than 0");
        status = status == null ? statusFor(enabled) : status;
        lastHealthAt = OcrNodeNormalization.normalizeOptional(lastHealthAt);
        lastSuccessAt = OcrNodeNormalization.normalizeOptional(lastSuccessAt);
        lastFailureAt = OcrNodeNormalization.normalizeOptional(lastFailureAt);
        lastError = OcrNodeNormalization.normalize(lastError);
        circuitOpenUntil = OcrNodeNormalization.normalizeOptional(circuitOpenUntil);
        lastManualRecoveryAt = OcrNodeNormalization.normalizeOptional(lastManualRecoveryAt);
    }

    /**
     * 创建兼容旧离线节点参数的 OCR 节点。
     *
     * @param id 节点 ID
     * @param modelKey OCR 模型标识
     * @param name 节点名称
     * @param host 节点主机
     * @param port 节点端口
     * @param enabled 是否启用
     * @param participateGlobal 是否参与全局负载均衡
     * @param weight 节点权重
     * @param maxConcurrency 最大并发图片数
     * @param status 节点状态
     * @param failureCount 失败次数
     * @param successCount 成功次数
     * @param avgLatencyMs 平均耗时
     * @param p95LatencyMs P95 耗时
     * @param lastHealthAt 最近健康检查时间
     * @param lastSuccessAt 最近成功时间
     * @param lastFailureAt 最近失败时间
     * @param lastError 最近错误
     * @param createdAt 创建时间
     * @param updatedAt 更新时间
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public OcrNode(
            String id,
            String modelKey,
            String name,
            String host,
            int port,
            boolean enabled,
            boolean participateGlobal,
            int weight,
            int maxConcurrency,
            OcrNodeStatus status,
            long failureCount,
            long successCount,
            long avgLatencyMs,
            long p95LatencyMs,
            Optional<OffsetDateTime> lastHealthAt,
            Optional<OffsetDateTime> lastSuccessAt,
            Optional<OffsetDateTime> lastFailureAt,
            Optional<String> lastError,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this(id, modelKey, OcrNodeDeploymentType.OFFLINE, name, host, port, Optional.empty(), Optional.empty(),
                Optional.empty(), false, enabled, participateGlobal, weight, maxConcurrency, status, failureCount,
                successCount, avgLatencyMs, p95LatencyMs, lastHealthAt, lastSuccessAt, lastFailureAt, lastError,
                Optional.empty(), Optional.empty(), createdAt, updatedAt);
    }

    /**
     * 创建新的 OCR 节点。
     *
     * @param request OCR 节点创建请求
     * @return OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public static OcrNode create(OcrNodeCreateRequest request) {
        return new OcrNode(request.id(), request.modelKey(), request.deploymentType(), request.name(), request.host(),
                request.port(), Optional.ofNullable(request.channelKey()), Optional.ofNullable(request.providerModel()),
                Optional.ofNullable(request.credentialRef()), request.credentialConfigured(), request.enabled(),
                request.participateGlobal(), request.weight(), request.maxConcurrency(), statusFor(request.enabled()),
                0L, 0L, 0L, 0L, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), request.now(), request.now());
    }

    /**
     * 更新节点配置并保留运行指标。
     *
     * @param request OCR 节点创建请求格式的更新值
     * @return 更新后的 OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public OcrNode updateSettings(OcrNodeCreateRequest request) {
        return new OcrNode(id, modelKey, request.deploymentType(), request.name(), request.host(), request.port(),
                Optional.ofNullable(request.channelKey()), Optional.ofNullable(request.providerModel()),
                Optional.ofNullable(request.credentialRef()), request.credentialConfigured(), request.enabled(),
                request.participateGlobal(), request.weight(), request.maxConcurrency(), updateStatus(request.enabled()),
                failureCount, successCount, avgLatencyMs, p95LatencyMs, lastHealthAt, lastSuccessAt, lastFailureAt,
                lastError, circuitOpenUntil, lastManualRecoveryAt, createdAt, request.now());
    }

    /**
     * 更新节点启用状态。
     *
     * @param enabled 是否启用
     * @param now 更新时间
     * @return 更新后的 OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public OcrNode changeEnabled(boolean enabled, OffsetDateTime now) {
        return new OcrNode(id, modelKey, deploymentType, name, host, port, channelKey, providerModel, credentialRef,
                credentialConfigured, enabled, participateGlobal, weight, maxConcurrency, updateStatus(enabled),
                failureCount, successCount, avgLatencyMs, p95LatencyMs, lastHealthAt, lastSuccessAt, lastFailureAt,
                lastError, circuitOpenUntil, lastManualRecoveryAt, createdAt, now);
    }

    /**
     * 更新节点健康治理时间窗与计数。
     *
     * @param failureCount 连续失败次数
     * @param successCount 恢复成功次数
     * @param lastHealthAt 最近健康检查时间
     * @param lastFailureAt 最近失败时间
     * @param lastError 最近错误
     * @param circuitOpenUntil 熔断结束时间
     * @param lastManualRecoveryAt 最近手动恢复时间
     * @param now 更新时间
     * @return 更新后的 OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public OcrNode updateHealthGovernance(
            long failureCount,
            long successCount,
            Optional<OffsetDateTime> lastHealthAt,
            Optional<OffsetDateTime> lastFailureAt,
            Optional<String> lastError,
            Optional<OffsetDateTime> circuitOpenUntil,
            Optional<OffsetDateTime> lastManualRecoveryAt,
            OffsetDateTime now
    ) {
        return new OcrNode(id, modelKey, deploymentType, name, host, port, channelKey, providerModel, credentialRef,
                credentialConfigured, enabled, participateGlobal, weight, maxConcurrency, status, failureCount,
                successCount, avgLatencyMs, p95LatencyMs, lastHealthAt, lastSuccessAt, lastFailureAt, lastError,
                circuitOpenUntil, lastManualRecoveryAt, createdAt, now);
    }

    /**
     * 根据启用状态更新节点状态。
     *
     * @param nextEnabled 下一启用状态
     * @return 节点状态
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private OcrNodeStatus updateStatus(boolean nextEnabled) {
        if (nextEnabled) {
            // 启用节点时保留健康态，仅将禁用态重新拉回恢复中。
            return status == OcrNodeStatus.DISABLED ? OcrNodeStatus.RECOVERING : status;
        } else {
            // 禁用节点必须显式进入 DISABLED，避免继续参与调度。
            return OcrNodeStatus.DISABLED;
        }
    }

    /**
     * 根据启用状态确定初始节点状态。
     *
     * @param enabled 是否启用
     * @return 节点状态
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static OcrNodeStatus statusFor(boolean enabled) {
        if (enabled) {
            // 新启用节点需要先走恢复探测再进入可用调度。
            return OcrNodeStatus.RECOVERING;
        } else {
            // 新禁用节点直接固化为 DISABLED。
            return OcrNodeStatus.DISABLED;
        }
    }
}
