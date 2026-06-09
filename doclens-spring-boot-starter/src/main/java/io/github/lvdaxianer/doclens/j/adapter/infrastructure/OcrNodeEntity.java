package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * OCR 节点 MyBatis-Plus 实体。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
@Getter
@Setter
@TableName("doclens_ocr_nodes")
public class OcrNodeEntity {

    @TableId("id")
    private String id;
    private String modelKey;
    private String deploymentType;
    private String name;
    private String host;
    private int port;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String channelKey;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String providerModel;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String credentialRef;
    private boolean credentialConfigured;
    private boolean enabled;
    private boolean participateGlobal;
    private int weight;
    private int maxConcurrency;
    private String status;
    private long failureCount;
    private long successCount;
    private long avgLatencyMs;
    private long p95LatencyMs;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private OffsetDateTime lastHealthAt;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private OffsetDateTime lastSuccessAt;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private OffsetDateTime lastFailureAt;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String lastError;
    private long consecutiveFailureCount;
    private long recoverySuccessCount;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private OffsetDateTime lastHealthCheckAt;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private OffsetDateTime circuitOpenUntil;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private OffsetDateTime lastManualRecoveryAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
