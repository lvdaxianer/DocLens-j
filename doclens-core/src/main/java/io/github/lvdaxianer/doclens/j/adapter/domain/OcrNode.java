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
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65535;
    private static final String ALIYUN_BAILIAN_DASHSCOPE = "aliyun_bailian_dashscope";

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
        id = requiredText(id, "ocr node id is required");
        modelKey = requiredText(modelKey, "ocr model key is required");
        deploymentType = deploymentType == null ? OcrNodeDeploymentType.OFFLINE : deploymentType;
        name = requiredText(name, "ocr node name is required");
        host = normalizeHost(deploymentType, host);
        port = normalizePort(deploymentType, port);
        channelKey = channelKey == null ? Optional.empty() : channelKey;
        providerModel = providerModel == null ? Optional.empty() : providerModel;
        credentialRef = credentialRef == null ? Optional.empty() : credentialRef;
        channelKey = normalizeChannel(deploymentType, channelKey);
        providerModel = normalizeProviderModel(deploymentType, providerModel);
        credentialRef = normalize(credentialRef);
        credentialConfigured = credentialConfigured || credentialRef.isPresent();
        validatePositive(weight, "ocr node weight must be greater than 0");
        validatePositive(maxConcurrency, "ocr node max concurrency must be greater than 0");
        status = status == null ? statusFor(enabled) : status;
        lastHealthAt = lastHealthAt == null ? Optional.empty() : lastHealthAt;
        lastSuccessAt = lastSuccessAt == null ? Optional.empty() : lastSuccessAt;
        lastFailureAt = lastFailureAt == null ? Optional.empty() : lastFailureAt;
        lastError = lastError == null ? Optional.empty() : normalize(lastError);
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
                createdAt, updatedAt);
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
                0L, 0L, 0L, 0L, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), request.now(),
                request.now());
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
                lastError, createdAt, request.now());
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
                lastError, createdAt, now);
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
            return status == OcrNodeStatus.DISABLED ? OcrNodeStatus.RECOVERING : status;
        } else {
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
            return OcrNodeStatus.RECOVERING;
        } else {
            return OcrNodeStatus.DISABLED;
        }
    }

    /**
     * 校验必填文本。
     *
     * @param value 文本值
     * @param message 校验失败消息
     * @return 标准化后的文本
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static String requiredText(String value, String message) {
        return normalize(Optional.ofNullable(value)).orElseThrow(() -> new IllegalArgumentException(message));
    }

    /**
     * 校验主机只包含 host，不包含 URL 结构。
     *
     * @param host 主机文本
     * @return 标准化后的主机文本
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static String validHost(String host) {
        String normalizedHost = requiredText(host, "ocr node host is required");
        if (containsUrlPart(normalizedHost)) {
            throw new IllegalArgumentException("ocr node host must not include scheme, path, query or fragment");
        } else {
            return normalizedHost;
        }
    }

    /**
     * 按部署类型标准化主机。
     *
     * @param deploymentType 节点部署类型
     * @param host 主机文本
     * @return 标准化后的主机文本
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static String normalizeHost(OcrNodeDeploymentType deploymentType, String host) {
        if (deploymentType == OcrNodeDeploymentType.OFFLINE) {
            return validHost(host);
        } else {
            return "";
        }
    }

    /**
     * 按部署类型标准化端口。
     *
     * @param deploymentType 节点部署类型
     * @param port 节点端口
     * @return 标准化后的端口
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static int normalizePort(OcrNodeDeploymentType deploymentType, int port) {
        if (deploymentType == OcrNodeDeploymentType.OFFLINE) {
            validatePort(port);
            return port;
        } else {
            return 0;
        }
    }

    /**
     * 按部署类型标准化在线渠道。
     *
     * @param deploymentType 节点部署类型
     * @param channelKey 在线渠道标识
     * @return 标准化后的在线渠道
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static Optional<String> normalizeChannel(
            OcrNodeDeploymentType deploymentType,
            Optional<String> channelKey
    ) {
        if (deploymentType == OcrNodeDeploymentType.ONLINE) {
            String normalizedChannel = requiredText(channelKey.orElse(""), "ocr online channel key is required");
            validateSupportedChannel(normalizedChannel);
            return Optional.of(normalizedChannel);
        } else {
            return Optional.empty();
        }
    }

    /**
     * 按部署类型标准化在线模型名称。
     *
     * @param deploymentType 节点部署类型
     * @param providerModel 在线模型名称
     * @return 标准化后的在线模型名称
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static Optional<String> normalizeProviderModel(
            OcrNodeDeploymentType deploymentType,
            Optional<String> providerModel
    ) {
        if (deploymentType == OcrNodeDeploymentType.ONLINE) {
            return Optional.of(requiredText(providerModel.orElse(""), "ocr online provider model is required"));
        } else {
            return Optional.empty();
        }
    }

    /**
     * 校验在线渠道是否已支持。
     *
     * @param channelKey 在线渠道标识
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static void validateSupportedChannel(String channelKey) {
        if (ALIYUN_BAILIAN_DASHSCOPE.equals(channelKey)) {
            return;
        } else {
            throw new IllegalArgumentException("unsupported ocr online channel key");
        }
    }

    /**
     * 判断主机是否包含 URL 结构。
     *
     * @param host 主机文本
     * @return 是否包含 URL 结构
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static boolean containsUrlPart(String host) {
        return host.contains("://") || host.contains("/") || host.contains("?") || host.contains("#");
    }

    /**
     * 校验端口范围。
     *
     * @param port 节点端口
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static void validatePort(int port) {
        if (port >= MIN_PORT && port <= MAX_PORT) {
            return;
        } else {
            throw new IllegalArgumentException("ocr node port must be between 1 and 65535");
        }
    }

    /**
     * 校验正整数。
     *
     * @param value 待校验值
     * @param message 校验失败消息
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static void validatePositive(int value, String message) {
        if (value > 0) {
            return;
        } else {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 标准化可选文本。
     *
     * @param value 可选文本
     * @return 标准化后的可选文本
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static Optional<String> normalize(Optional<String> value) {
        return value.map(String::trim).filter(text -> !text.isBlank());
    }
}
