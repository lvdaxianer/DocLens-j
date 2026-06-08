package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * OCR 节点图片调用 MyBatis-Plus 实体。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
@Getter
@Setter
@TableName("doclens_ocr_node_calls")
public class OcrNodeCallEntity {

    @TableId("id")
    private String id;
    private String batchId;
    private String documentId;
    private int pageNo;
    private String modelKey;
    private String nodeId;
    private String routingMode;
    private String status;
    private int retryCount;
    private long elapsedMs;
    private String errorCode;
    private String errorMessage;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
}
