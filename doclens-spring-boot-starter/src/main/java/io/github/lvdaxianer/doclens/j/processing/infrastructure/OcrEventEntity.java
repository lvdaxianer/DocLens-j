package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * OCR 生命周期事件的 MyBatis-Plus 实体。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@Getter
@Setter
@TableName("ocr_events")
public class OcrEventEntity {

    @TableId("event_id")
    private String eventId;
    private String eventType;
    private String batchId;
    private String documentId;
    private String status;
    private String stage;
    private String progress;
    private String metadata;
    private String resultId;
    private String resultSummary;
    private String error;
    private OffsetDateTime occurredAt;
}
