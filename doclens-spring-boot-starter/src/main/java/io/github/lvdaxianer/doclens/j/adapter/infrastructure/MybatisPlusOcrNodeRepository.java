package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
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
        entity.setName(node.name());
        entity.setHost(node.host());
        entity.setPort(node.port());
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
        return new OcrNode(entity.getId(), entity.getModelKey(), entity.getName(), entity.getHost(),
                entity.getPort(), entity.isEnabled(), entity.isParticipateGlobal(), entity.getWeight(),
                entity.getMaxConcurrency(), OcrNodeStatus.valueOf(entity.getStatus()), entity.getFailureCount(),
                entity.getSuccessCount(), entity.getAvgLatencyMs(), entity.getP95LatencyMs(),
                Optional.ofNullable(entity.getLastHealthAt()), Optional.ofNullable(entity.getLastSuccessAt()),
                Optional.ofNullable(entity.getLastFailureAt()), Optional.ofNullable(entity.getLastError()),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
