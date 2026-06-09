package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeDeploymentType;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import java.time.OffsetDateTime;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.MybatisPlusPages;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * OCR 节点仓储 MyBatis-Plus 实现。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
@Repository
public class MybatisPlusOcrNodeRepository
        extends ServiceImpl<OcrNodeMapper, OcrNodeEntity>
        implements OcrNodeRepository {

    /**
     * 保存 OCR 节点。
     *
     * @param node OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Override
    public void save(OcrNode node) {
        super.save(toEntity(node));
    }

    /**
     * 批量保存 OCR 节点。
     *
     * @param nodes OCR 节点集合
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Override
    public void saveAll(List<OcrNode> nodes) {
        super.saveBatch(nodes.stream().map(this::toEntity).toList());
    }

    /**
     * 更新 OCR 节点。
     *
     * @param node OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Override
    public void update(OcrNode node) {
        updateById(toEntity(node));
    }

    /**
     * 根据节点 ID 查询 OCR 节点。
     *
     * @param nodeId OCR 节点 ID
     * @return OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Override
    public Optional<OcrNode> findById(String nodeId) {
        return Optional.ofNullable(getById(nodeId)).map(this::toDomain);
    }

    /**
     * 按 OCR 模型标识列出节点。
     *
     * @param modelKey OCR 模型标识
     * @return OCR 节点集合
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Override
    public List<OcrNode> listByModelKey(String modelKey) {
        LambdaQueryWrapper<OcrNodeEntity> wrapper = orderedWrapper().eq(OcrNodeEntity::getModelKey, modelKey);
        return page(MybatisPlusPages.listLimit(), wrapper).getRecords().stream().map(this::toDomain).toList();
    }

    /**
     * 列出所有启用节点。
     *
     * @return OCR 节点集合
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Override
    public List<OcrNode> listEnabled() {
        LambdaQueryWrapper<OcrNodeEntity> wrapper = orderedWrapper().eq(OcrNodeEntity::isEnabled, true);
        return page(MybatisPlusPages.listLimit(), wrapper).getRecords().stream().map(this::toDomain).toList();
    }

    /**
     * 列出所有 OCR 节点。
     *
     * @return OCR 节点集合
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Override
    public List<OcrNode> listAll() {
        return page(MybatisPlusPages.listLimit(), orderedWrapper()).getRecords().stream().map(this::toDomain).toList();
    }

    /**
     * 按模型和主机端口查询节点。
     *
     * @param modelKey OCR 模型标识
     * @param host 节点主机
     * @param port 节点端口
     * @return OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Override
    public Optional<OcrNode> findByModelHostPort(String modelKey, String host, int port) {
        LambdaQueryWrapper<OcrNodeEntity> wrapper = new LambdaQueryWrapper<OcrNodeEntity>()
                .eq(OcrNodeEntity::getModelKey, modelKey)
                .eq(OcrNodeEntity::getHost, host)
                .eq(OcrNodeEntity::getPort, port);
        return page(MybatisPlusPages.limit(1), wrapper).getRecords().stream().findFirst().map(this::toDomain);
    }

    /**
     * 删除 OCR 节点。
     *
     * @param nodeId OCR 节点 ID
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Override
    public void deleteById(String nodeId) {
        removeById(nodeId);
    }

    /**
     * 创建稳定排序查询条件。
     *
     * @return 查询条件
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private LambdaQueryWrapper<OcrNodeEntity> orderedWrapper() {
        return new LambdaQueryWrapper<OcrNodeEntity>()
                .orderByAsc(OcrNodeEntity::getModelKey)
                .orderByAsc(OcrNodeEntity::getId);
    }

    /**
     * 将领域 OCR 节点转换为持久化实体。
     *
     * @param node OCR 节点
     * @return OCR 节点实体
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private OcrNodeEntity toEntity(OcrNode node) {
        OcrNodeEntity entity = new OcrNodeEntity();
        entity.setId(node.id());
        entity.setModelKey(node.modelKey());
        entity.setDeploymentType(node.deploymentType().name());
        entity.setName(node.name());
        entity.setHost(node.host());
        entity.setPort(node.port());
        entity.setChannelKey(node.channelKey().orElse(null));
        entity.setProviderModel(node.providerModel().orElse(null));
        entity.setCredentialRef(node.credentialRef().orElse(null));
        entity.setCredentialConfigured(node.credentialConfigured());
        entity.setEnabled(node.enabled());
        entity.setParticipateGlobal(node.participateGlobal());
        entity.setWeight(node.weight());
        entity.setMaxConcurrency(node.maxConcurrency());
        entity.setStatus(node.status().name());
        entity.setFailureCount(node.failureCount());
        entity.setSuccessCount(node.successCount());
        entity.setAvgLatencyMs(node.avgLatencyMs());
        entity.setP95LatencyMs(node.p95LatencyMs());
        entity.setLastHealthAt(node.lastHealthAt().orElse(null));
        entity.setLastSuccessAt(node.lastSuccessAt().orElse(null));
        entity.setLastFailureAt(node.lastFailureAt().orElse(null));
        entity.setLastError(node.lastError().orElse(null));
        entity.setConsecutiveFailureCount(node.failureCount());
        entity.setRecoverySuccessCount(node.successCount());
        entity.setLastHealthCheckAt(node.lastHealthAt().orElse(null));
        entity.setCircuitOpenUntil(node.circuitOpenUntil().orElse(null));
        entity.setLastManualRecoveryAt(node.lastManualRecoveryAt().orElse(null));
        entity.setCreatedAt(node.createdAt());
        entity.setUpdatedAt(node.updatedAt());
        return entity;
    }

    /**
     * 将持久化实体转换为领域 OCR 节点。
     *
     * @param entity OCR 节点实体
     * @return OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private OcrNode toDomain(OcrNodeEntity entity) {
        return new OcrNode(entity.getId(), entity.getModelKey(), deploymentType(entity), entity.getName(),
                entity.getHost(), entity.getPort(), Optional.ofNullable(entity.getChannelKey()),
                Optional.ofNullable(entity.getProviderModel()), Optional.ofNullable(entity.getCredentialRef()),
                entity.isCredentialConfigured(), entity.isEnabled(), entity.isParticipateGlobal(),
                entity.getWeight(), entity.getMaxConcurrency(), OcrNodeStatus.valueOf(entity.getStatus()),
                failureCount(entity), successCount(entity), entity.getAvgLatencyMs(), entity.getP95LatencyMs(),
                lastHealthAt(entity), Optional.ofNullable(entity.getLastSuccessAt()),
                Optional.ofNullable(entity.getLastFailureAt()), Optional.ofNullable(entity.getLastError()),
                Optional.ofNullable(entity.getCircuitOpenUntil()),
                Optional.ofNullable(entity.getLastManualRecoveryAt()), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    /**
     * 读取连续失败次数并兼容历史列。
     *
     * @param entity OCR 节点实体
     * @return 连续失败次数
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private long failureCount(OcrNodeEntity entity) {
        if (entity.getConsecutiveFailureCount() > 0) {
            return entity.getConsecutiveFailureCount();
        } else {
            return entity.getFailureCount();
        }
    }

    /**
     * 读取恢复成功次数并兼容历史列。
     *
     * @param entity OCR 节点实体
     * @return 恢复成功次数
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private long successCount(OcrNodeEntity entity) {
        if (entity.getRecoverySuccessCount() > 0) {
            return entity.getRecoverySuccessCount();
        } else {
            return entity.getSuccessCount();
        }
    }

    /**
     * 读取最近健康检查时间并兼容历史列。
     *
     * @param entity OCR 节点实体
     * @return 最近健康检查时间
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private Optional<OffsetDateTime> lastHealthAt(OcrNodeEntity entity) {
        if (entity.getLastHealthCheckAt() != null) {
            return Optional.of(entity.getLastHealthCheckAt());
        } else {
            return Optional.ofNullable(entity.getLastHealthAt());
        }
    }

    /**
     * 读取节点部署类型并兼容历史数据。
     *
     * @param entity OCR 节点实体
     * @return 节点部署类型
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private OcrNodeDeploymentType deploymentType(OcrNodeEntity entity) {
        if (entity.getDeploymentType() == null || entity.getDeploymentType().isBlank()) {
            return OcrNodeDeploymentType.OFFLINE;
        } else {
            return OcrNodeDeploymentType.valueOf(entity.getDeploymentType());
        }
    }
}
