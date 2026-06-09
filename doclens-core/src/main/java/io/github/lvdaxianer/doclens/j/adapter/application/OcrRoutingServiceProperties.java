package io.github.lvdaxianer.doclens.j.adapter.application;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrHealthGovernance;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutePolicy;

/**
 * OCR 路由服务配置。
 *
 * @param defaultPolicy 默认路由策略
 * @param idleFactor 空闲容量评分权重
 * @param weightFactor 节点权重评分权重
 * @param topBucketThreshold Top Bucket 分桶阈值
 * @param requestRetryTimes OCR 请求重试次数
 * @param healthGovernance OCR 节点健康治理配置
 * @param specificNodeFallbackEnabled 指定节点失败后是否允许回退
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public record OcrRoutingServiceProperties(
        OcrRoutePolicy defaultPolicy,
        double idleFactor,
        double weightFactor,
        double topBucketThreshold,
        int requestRetryTimes,
        OcrHealthGovernance healthGovernance,
        boolean specificNodeFallbackEnabled
) {
    public static final String DEFAULT_LOAD_BALANCE_STRATEGY = "weighted-idle";
    public static final double DEFAULT_IDLE_FACTOR = 0.7D;
    public static final double DEFAULT_WEIGHT_FACTOR = 0.3D;
    public static final double DEFAULT_TOP_BUCKET_THRESHOLD = 0.15D;
    public static final int DEFAULT_REQUEST_RETRY_TIMES = 3;

    /**
     * 创建兼容旧调用方式的 OCR 路由服务配置。
     *
     * @param defaultPolicy 默认路由策略
     * @param requestRetryTimes OCR 请求重试次数
     * @param specificNodeFallbackEnabled 指定节点失败后是否允许回退
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public OcrRoutingServiceProperties(
            OcrRoutePolicy defaultPolicy,
            int requestRetryTimes,
            boolean specificNodeFallbackEnabled
    ) {
        this(defaultPolicy, DEFAULT_IDLE_FACTOR, DEFAULT_WEIGHT_FACTOR, DEFAULT_TOP_BUCKET_THRESHOLD,
                requestRetryTimes, OcrHealthGovernance.defaults(), specificNodeFallbackEnabled);
    }
}
