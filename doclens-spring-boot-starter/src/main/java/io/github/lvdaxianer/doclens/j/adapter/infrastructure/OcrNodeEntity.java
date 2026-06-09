package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

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
    private String channelKey;
    private String providerModel;
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
    private OffsetDateTime lastHealthAt;
    private OffsetDateTime lastSuccessAt;
    private OffsetDateTime lastFailureAt;
    private String lastError;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
