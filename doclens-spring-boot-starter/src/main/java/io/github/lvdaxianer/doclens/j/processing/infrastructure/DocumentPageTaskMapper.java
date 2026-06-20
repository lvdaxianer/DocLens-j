package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskClaimRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskCompletionRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskFailureRequest;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
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
     * 按文档公平查询等待调度的页任务。
     *
     * @param limit 最大返回数量
     * @return 等待调度页任务集合
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Select("""
            SELECT task_id, batch_id, document_id, page_no, image_storage_uri,
                   status, locked_by, locked_until, retry_count, error_code,
                   error_message, started_at, completed_at, created_at, updated_at
            FROM (
                SELECT task.*,
                       ROW_NUMBER() OVER (
                           PARTITION BY document_id
                           ORDER BY created_at ASC, task_id ASC
                       ) AS document_queue_rank
                FROM ocr_document_page_tasks task
                WHERE status = 'QUEUED'
            ) ranked_tasks
            ORDER BY document_queue_rank ASC, created_at ASC, task_id ASC
            LIMIT #{limit}
            """)
    List<DocumentPageTaskEntity> listQueuedFairly(@Param("limit") int limit);

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
