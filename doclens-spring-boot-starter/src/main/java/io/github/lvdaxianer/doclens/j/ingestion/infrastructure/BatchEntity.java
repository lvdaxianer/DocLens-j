package io.github.lvdaxianer.doclens.j.ingestion.infrastructure;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * OCR 批次持久化的 MyBatis-Plus 实体。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@Getter
@Setter
@TableName("ocr_batches")
public class BatchEntity {

    @TableId("batch_id")
    private String batchId;
    private String status;
    private int totalFiles;
    private int completedFiles;
    private int failedFiles;
    private String currentDocumentId;
    private String currentDocumentName;
    private String currentStage;
    private String metadata;
    private String callbackUrl;
    private String idempotencyKey;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
