package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 文档页 OCR 任务的 MyBatis-Plus 实体。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
@Getter
@Setter
@TableName("ocr_document_page_tasks")
public class DocumentPageTaskEntity {

    /** 页任务主键。 */
    @TableId("task_id")
    private String taskId;
    /** 所属批次 ID。 */
    private String batchId;
    /** 所属文档 ID。 */
    private String documentId;
    /** 文档内页码，从 1 开始。 */
    private int pageNo;
    /** 页图片存储地址。 */
    private String imageStorageUri;
    /** 页任务状态。 */
    private String status;
    /** 抢占该任务的工作线程标识。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String lockedBy;
    /** 抢占锁过期时间。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private OffsetDateTime lockedUntil;
    /** OCR 重试次数。 */
    private int retryCount;
    /** 终态失败错误码。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String errorCode;
    /** 终态失败错误信息。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String errorMessage;
    /** OCR 开始时间。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private OffsetDateTime startedAt;
    /** OCR 完成时间。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private OffsetDateTime completedAt;
    /** 创建时间。 */
    private OffsetDateTime createdAt;
    /** 更新时间。 */
    private OffsetDateTime updatedAt;
}
