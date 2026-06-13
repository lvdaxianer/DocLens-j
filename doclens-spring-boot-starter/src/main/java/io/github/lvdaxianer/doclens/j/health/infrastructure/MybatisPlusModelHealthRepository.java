package io.github.lvdaxianer.doclens.j.health.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthCounters;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthFailure;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthFailureType;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthRepository;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthSnapshot;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthStatus;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthTargetId;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthTargetType;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthTimeline;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.MybatisPlusPages;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

/**
 * 模型健康仓储 MyBatis-Plus 实现。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
@Repository
public class MybatisPlusModelHealthRepository
        extends ServiceImpl<ModelHealthMapper, ModelHealthEntity>
        implements ModelHealthRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(MybatisPlusModelHealthRepository.class);
    private static final int LAST_ERROR_MAX_LENGTH = 1024;
    private static final int HEALTH_KEY_BUILDER_CAPACITY = 256;
    private static final String HEALTH_KEY_DIGEST_ALGORITHM = "SHA-256";
    private static final char HEALTH_KEY_PART_SEPARATOR = '#';

    /**
     * 查询全部模型健康快照。
     *
     * @return 模型健康快照集合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public List<ModelHealthSnapshot> listAll() {
        return page(MybatisPlusPages.listLimit(), orderedWrapper()).getRecords().stream()
                .map(this::toDomain)
                .toList();
    }

    /**
     * 批量写入或更新模型健康快照。
     *
     * @param snapshots 模型健康快照集合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public void upsertAll(List<ModelHealthSnapshot> snapshots) {
        // 未产生健康快照时跳过写入，避免触发无意义的批量数据库操作。
        if (snapshots == null || snapshots.isEmpty()) {
            return;
        } else {
            // 存在健康快照时使用批量保存或更新，避免循环逐条访问数据库。
            saveOrUpdateBatch(snapshots.stream().map(this::toEntity).toList());
        }
    }

    /**
     * 创建稳定排序查询条件。
     *
     * @return 查询条件
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private LambdaQueryWrapper<ModelHealthEntity> orderedWrapper() {
        return new LambdaQueryWrapper<ModelHealthEntity>()
                .orderByAsc(ModelHealthEntity::getTargetType)
                .orderByAsc(ModelHealthEntity::getModelKey)
                .orderByAsc(ModelHealthEntity::getTargetId);
    }

    /**
     * 将领域快照转换为持久化实体。
     *
     * @param snapshot 健康快照
     * @return 持久化实体
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ModelHealthEntity toEntity(ModelHealthSnapshot snapshot) {
        ModelHealthTargetId targetId = snapshot.targetId();
        ModelHealthEntity entity = new ModelHealthEntity();
        entity.setHealthKey(healthKey(targetId));
        entity.setTargetType(targetId.targetType().name());
        entity.setModelKey(targetId.modelKey());
        entity.setTargetId(targetId.targetId());
        entity.setStatus(snapshot.status().name());
        entity.setConsecutiveFailures(snapshot.consecutiveFailures());
        entity.setConsecutiveSuccesses(snapshot.consecutiveSuccesses());
        entity.setLastHeartbeatAt(snapshot.lastHeartbeatAt().orElse(null));
        entity.setLastSuccessAt(snapshot.lastSuccessAt().orElse(null));
        entity.setLastFailureAt(snapshot.lastFailureAt().orElse(null));
        entity.setLastFailureType(snapshot.lastFailureType().map(Enum::name).orElse(null));
        setLastError(entity, snapshot);
        entity.setUpdatedAt(snapshot.updatedAt());
        return entity;
    }

    /**
     * 将持久化实体转换为领域快照。
     *
     * @param entity 持久化实体
     * @return 健康快照
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ModelHealthSnapshot toDomain(ModelHealthEntity entity) {
        return new ModelHealthSnapshot(targetId(entity), status(entity), counters(entity), timeline(entity),
                failure(entity));
    }

    /**
     * 创建健康目标标识。
     *
     * @param entity 持久化实体
     * @return 健康目标标识
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ModelHealthTargetId targetId(ModelHealthEntity entity) {
        return new ModelHealthTargetId(ModelHealthTargetType.valueOf(entity.getTargetType()), entity.getModelKey(),
                entity.getTargetId());
    }

    /**
     * 读取健康状态。
     *
     * @param entity 持久化实体
     * @return 健康状态
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ModelHealthStatus status(ModelHealthEntity entity) {
        return ModelHealthStatus.valueOf(entity.getStatus());
    }

    /**
     * 创建健康计数。
     *
     * @param entity 持久化实体
     * @return 健康计数
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ModelHealthCounters counters(ModelHealthEntity entity) {
        return new ModelHealthCounters(entity.getConsecutiveFailures(), entity.getConsecutiveSuccesses());
    }

    /**
     * 创建健康时间线。
     *
     * @param entity 持久化实体
     * @return 健康时间线
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ModelHealthTimeline timeline(ModelHealthEntity entity) {
        return new ModelHealthTimeline(Optional.ofNullable(entity.getLastHeartbeatAt()),
                Optional.ofNullable(entity.getLastSuccessAt()), Optional.ofNullable(entity.getLastFailureAt()),
                entity.getUpdatedAt());
    }

    /**
     * 创建失败摘要。
     *
     * @param entity 持久化实体
     * @return 失败摘要
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ModelHealthFailure failure(ModelHealthEntity entity) {
        return new ModelHealthFailure(Optional.ofNullable(entity.getLastError()), failureType(entity));
    }

    /**
     * 读取失败类型。
     *
     * @param entity 持久化实体
     * @return 失败类型
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private Optional<ModelHealthFailureType> failureType(ModelHealthEntity entity) {
        return Optional.ofNullable(entity.getLastFailureType()).map(ModelHealthFailureType::valueOf);
    }

    /**
     * 生成健康状态主键。
     *
     * @param targetId 健康目标标识
     * @return 健康状态主键
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private String healthKey(ModelHealthTargetId targetId) {
        return digest(canonicalHealthKey(targetId));
    }

    /**
     * 生成无歧义健康状态主键原文。
     *
     * @param targetId 健康目标标识
     * @return 健康状态主键原文
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private String canonicalHealthKey(ModelHealthTargetId targetId) {
        StringBuilder builder = new StringBuilder(HEALTH_KEY_BUILDER_CAPACITY);
        appendKeyPart(builder, targetId.targetType().name());
        appendKeyPart(builder, targetId.modelKey());
        appendKeyPart(builder, targetId.targetId());
        return builder.toString();
    }

    /**
     * 追加带长度前缀的主键片段。
     *
     * @param builder 主键原文构建器
     * @param value 主键片段
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private void appendKeyPart(StringBuilder builder, String value) {
        builder.append(value.length())
                .append(HEALTH_KEY_PART_SEPARATOR)
                .append(value);
    }

    /**
     * 计算主键摘要。
     *
     * @param value 主键原文
     * @return 主键摘要
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private String digest(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HEALTH_KEY_DIGEST_ALGORITHM);
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            LOGGER.error("[模型健康持久化] 健康状态主键摘要算法不可用, algorithm={}", HEALTH_KEY_DIGEST_ALGORITHM, exception);
            throw new IllegalStateException("model health key digest algorithm is unavailable", exception);
        }
    }

    /**
     * 设置截断后的错误摘要。
     *
     * @param entity 持久化实体
     * @param snapshot 健康快照
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private void setLastError(ModelHealthEntity entity, ModelHealthSnapshot snapshot) {
        Optional<String> error = snapshot.lastError();
        // 没有错误摘要或摘要未超长时原样写入持久化字段。
        if (error.isEmpty() || error.orElseThrow().length() <= LAST_ERROR_MAX_LENGTH) {
            entity.setLastError(error.orElse(null));
        } else {
            // 错误摘要超过数据库字段上限时截断，保证批量写入不会因长度失败。
            entity.setLastError(error.orElseThrow().substring(0, LAST_ERROR_MAX_LENGTH));
        }
    }
}
