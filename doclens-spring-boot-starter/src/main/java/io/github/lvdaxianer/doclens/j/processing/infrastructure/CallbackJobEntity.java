package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 回调任务的 MyBatis-Plus 实体。
 *
 * @author lvdaxianerplus
 * @date 2026-06-15
 */
@Getter
@Setter
@TableName("ocr_callback_jobs")
public class CallbackJobEntity {

    /** 回调任务主键。 */
    @TableId("callback_job_id")
    private String callbackJobId;
    /** 关联事件 ID。 */
    private String eventId;
    /** 关联批次 ID。 */
    private String batchId;
    /** 可选文档 ID。 */
    private String documentId;
    /** 回调地址。 */
    private String callbackUrl;
    /** 回调状态。 */
    private String status;
    /** 回调载荷。 */
    private String payload;
    /** 重试次数。 */
    private int retryCount;
    /** 下次重试时间。 */
    private OffsetDateTime nextRetryAt;
    /** 失败原因。 */
    private String failureReason;
    /** 失败详情。 */
    private String failureDetail;
    /** 创建时间。 */
    private OffsetDateTime createdAt;
    /** 更新时间。 */
    private OffsetDateTime updatedAt;
}
