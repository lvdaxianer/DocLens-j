package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * OCR 全局治理配置 MyBatis-Plus 实体。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
@Getter
@Setter
@TableName("doclens_ocr_governance_config")
public class OcrGovernanceConfigEntity {

    @TableId("id")
    private String id;
    private int failureThreshold;
    private int probeIntervalSeconds;
    private int circuitOpenSeconds;
    private int recoverySuccessThreshold;
    private int manualRecoveryAttempts;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
