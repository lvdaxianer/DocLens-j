package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackFailureReason;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJob;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobFailureRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobStatus;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.JsonCodec;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 回调任务仓储的 MyBatis-Plus 实现。
 *
 * @author lvdaxianerplus
 * @date 2026-06-15
 */
@Repository
public class MybatisPlusCallbackJobRepository
        extends ServiceImpl<CallbackJobMapper, CallbackJobEntity>
        implements CallbackJobRepository {

    private final JsonCodec jsonCodec;

    /**
     * 创建 MyBatis-Plus 回调任务仓储。
     *
     * @param jsonCodec JSON 编解码器
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    public MybatisPlusCallbackJobRepository(JsonCodec jsonCodec) {
        this.jsonCodec = jsonCodec;
    }

    /**
     * 保存回调任务。
     *
     * @param job 回调任务
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    @Override
    public void save(CallbackJob job) {
        super.save(toEntity(job));
    }

    /**
     * 根据 ID 查找回调任务。
     *
     * @param callbackJobId 回调任务 ID
     * @return 可选回调任务
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    @Override
    public Optional<CallbackJob> findById(String callbackJobId) {
        return Optional.ofNullable(getById(callbackJobId)).map(this::toDomain);
    }

    /**
     * 标记回调任务投递成功。
     *
     * @param callbackJobId 回调任务 ID
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    @Override
    public void markSucceeded(String callbackJobId) {
        CallbackJob current = requiredJob(callbackJobId);
        updateById(toEntity(current.markSucceeded(OffsetDateTime.now())));
    }

    /**
     * 将回调任务重置为新一轮手动投递。
     *
     * @param callbackJobId 回调任务 ID
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    @Override
    public void restartAttempts(String callbackJobId) {
        CallbackJob current = requiredJob(callbackJobId);
        updateById(toEntity(current.restartAttempts(OffsetDateTime.now())));
    }

    /**
     * 记录一次失败回调尝试。
     *
     * @param request 失败更新请求
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    @Override
    public void markFailed(CallbackJobFailureRequest request) {
        CallbackJob current = requiredJob(request.callbackJobId());
        // 未到达重试上限时保留重试状态，否则直接落终态失败。
        if (request.nextRetryAt() == null) {
            updateById(toEntity(current.markFailed(request)));
        } else {
            updateById(toEntity(current.markRetrying(request)));
        }
    }

    /**
     * 查询待执行的回调任务。
     *
     * @param limit 最大返回数量
     * @return 待执行回调任务
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    public List<CallbackJob> listPending(int limit) {
        OffsetDateTime now = OffsetDateTime.now();
        LambdaQueryWrapper<CallbackJobEntity> wrapper = new LambdaQueryWrapper<CallbackJobEntity>()
                .and(query -> query.eq(CallbackJobEntity::getStatus, CallbackJobStatus.PENDING.name().toLowerCase())
                        .or(retrying -> retrying.eq(CallbackJobEntity::getStatus,
                                        CallbackJobStatus.RETRYING.name().toLowerCase())
                                .le(CallbackJobEntity::getNextRetryAt, now)))
                .orderByAsc(CallbackJobEntity::getCreatedAt)
                .orderByAsc(CallbackJobEntity::getCallbackJobId);
        return page(io.github.lvdaxianer.doclens.j.shared.infrastructure.MybatisPlusPages.limit(limit), wrapper)
                .getRecords().stream().map(this::toDomain).toList();
    }

    /**
     * 根据批次查询回调任务。
     *
     * @param batchId 批次 ID
     * @return 回调任务集合
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    @Override
    public List<CallbackJob> listByBatchId(String batchId) {
        LambdaQueryWrapper<CallbackJobEntity> wrapper = new LambdaQueryWrapper<CallbackJobEntity>()
                .eq(CallbackJobEntity::getBatchId, batchId)
                .orderByAsc(CallbackJobEntity::getCreatedAt)
                .orderByAsc(CallbackJobEntity::getCallbackJobId);
        return list(wrapper).stream().map(this::toDomain).toList();
    }

    /**
     * 查询必须存在的回调任务。
     *
     * @param callbackJobId 回调任务 ID
     * @return 回调任务
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private CallbackJob requiredJob(String callbackJobId) {
        return findById(callbackJobId)
                .orElseThrow(() -> new IllegalStateException("callback job not found: " + callbackJobId));
    }

    /**
     * 将领域回调任务转换为持久化实体。
     *
     * @param job 回调任务
     * @return 回调任务实体
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private CallbackJobEntity toEntity(CallbackJob job) {
        CallbackJobEntity entity = new CallbackJobEntity();
        entity.setCallbackJobId(job.callbackJobId());
        entity.setEventId(job.eventId());
        entity.setBatchId(job.batchId());
        entity.setDocumentId(job.documentId().orElse(null));
        entity.setCallbackUrl(job.callbackUrl());
        entity.setStatus(job.status().name().toLowerCase());
        entity.setPayload(jsonCodec.toJson(job.payload()));
        entity.setRetryCount(job.retryCount());
        entity.setNextRetryAt(job.nextRetryAt().orElse(null));
        entity.setFailureReason(job.failureReason().map(Enum::name).map(String::toLowerCase).orElse(null));
        entity.setFailureDetail(job.failureDetail().orElse(null));
        entity.setCreatedAt(job.createdAt());
        entity.setUpdatedAt(job.updatedAt());
        return entity;
    }

    /**
     * 将持久化实体转换为领域回调任务。
     *
     * @param entity 回调任务实体
     * @return 回调任务
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private CallbackJob toDomain(CallbackJobEntity entity) {
        return new CallbackJob(entity.getCallbackJobId(), entity.getEventId(), entity.getBatchId(),
                Optional.ofNullable(entity.getDocumentId()),
                entity.getCallbackUrl(), CallbackJobStatus.valueOf(entity.getStatus().toUpperCase()),
                jsonCodec.parseObject(entity.getPayload()), entity.getRetryCount(),
                Optional.ofNullable(entity.getNextRetryAt()),
                Optional.ofNullable(entity.getFailureReason()).map(String::toUpperCase)
                        .map(CallbackFailureReason::valueOf),
                Optional.ofNullable(entity.getFailureDetail()), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
