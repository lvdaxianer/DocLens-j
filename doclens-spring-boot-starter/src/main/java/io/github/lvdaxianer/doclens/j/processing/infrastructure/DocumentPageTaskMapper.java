package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskClaimRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskCompletionRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskFailureRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 文档页 OCR 任务的 MyBatis-Plus Mapper。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
@Mapper
public interface DocumentPageTaskMapper extends BaseMapper<DocumentPageTaskEntity> {

    /**
     * 基于 QUEUED 条件原子抢占页任务。
     *
     * @param request 抢占请求
     * @return 更新行数
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Update("""
            UPDATE ocr_document_page_tasks
            SET status = 'PROCESSING',
                locked_by = #{request.workerId},
                locked_until = #{request.lockedUntil},
                error_code = NULL,
                error_message = NULL,
                started_at = #{request.now},
                completed_at = NULL,
                updated_at = #{request.now}
            WHERE task_id = #{request.taskId}
              AND status = 'QUEUED'
            """)
    int tryMarkProcessing(@Param("request") DocumentPageTaskClaimRequest request);

    /**
     * 标记当前 worker 持有的页任务已完成。
     *
     * @param request 完成请求
     * @return 更新行数
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Update("""
            UPDATE ocr_document_page_tasks
            SET status = 'COMPLETED',
                error_code = NULL,
                error_message = NULL,
                completed_at = #{request.now},
                updated_at = #{request.now}
            WHERE task_id = #{request.taskId}
              AND locked_by = #{request.workerId}
              AND status = 'PROCESSING'
            """)
    int markCompleted(@Param("request") DocumentPageTaskCompletionRequest request);

    /**
     * 标记页任务终态失败。
     *
     * @param request 失败请求
     * @return 更新行数
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Update("""
            UPDATE ocr_document_page_tasks
            SET status = 'FAILED',
                error_code = #{request.errorCode},
                error_message = #{request.errorMessage},
                completed_at = #{request.now},
                updated_at = #{request.now}
            WHERE task_id = #{request.taskId}
              AND status = 'PROCESSING'
            """)
    int markFailed(@Param("request") DocumentPageTaskFailureRequest request);
}
