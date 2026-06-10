package io.github.lvdaxianer.doclens.j.health.infrastructure;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 模型健康 MyBatis-Plus 实体。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
@Getter
@Setter
@TableName("doclens_model_health")
public class ModelHealthEntity {

    @TableId("health_key")
    private String healthKey;
    private String targetType;
    private String modelKey;
    private String targetId;
    private String status;
    private long consecutiveFailures;
    private long consecutiveSuccesses;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private OffsetDateTime lastHeartbeatAt;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private OffsetDateTime lastSuccessAt;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private OffsetDateTime lastFailureAt;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String lastFailureType;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String lastError;
    private OffsetDateTime updatedAt;
}
