package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCall;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallStatus;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutingMode;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.MybatisPlusPages;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * OCR 节点图片调用仓储 MyBatis-Plus 实现。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
@Repository
public class MybatisPlusOcrNodeCallRepository
        extends ServiceImpl<OcrNodeCallMapper, OcrNodeCallEntity>
        implements OcrNodeCallRepository {

    /**
     * 保存图片级 OCR 调用记录。
     *
     * @param call OCR 调用记录
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Override
    public void save(OcrNodeCall call) {
        super.save(toEntity(call));
    }

    /**
     * 按文档 ID 查询 OCR 调用记录。
     *
     * @param documentId 文档 ID
     * @return OCR 调用记录集合
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Override
    public List<OcrNodeCall> listByDocumentId(String documentId) {
        LambdaQueryWrapper<OcrNodeCallEntity> wrapper = new LambdaQueryWrapper<OcrNodeCallEntity>()
                .eq(OcrNodeCallEntity::getDocumentId, documentId)
                .orderByAsc(OcrNodeCallEntity::getStartedAt)
                .orderByAsc(OcrNodeCallEntity::getId);
        return page(MybatisPlusPages.listLimit(), wrapper).getRecords().stream().map(this::toDomain).toList();
    }

    /**
     * 按批次 ID 查询 OCR 调用记录。
     *
     * @param batchId 批次 ID
     * @return OCR 调用记录集合
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Override
    public List<OcrNodeCall> listByBatchId(String batchId) {
        LambdaQueryWrapper<OcrNodeCallEntity> wrapper = new LambdaQueryWrapper<OcrNodeCallEntity>()
                .eq(OcrNodeCallEntity::getBatchId, batchId)
                .orderByAsc(OcrNodeCallEntity::getStartedAt)
                .orderByAsc(OcrNodeCallEntity::getId);
        return page(MybatisPlusPages.listLimit(), wrapper).getRecords().stream().map(this::toDomain).toList();
    }

    /**
     * 按节点 ID 查询最近 OCR 调用记录。
     *
     * @param nodeId OCR 节点 ID
     * @param limit 最大返回数量
     * @return OCR 调用记录集合
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Override
    public List<OcrNodeCall> listRecentByNodeId(String nodeId, int limit) {
        LambdaQueryWrapper<OcrNodeCallEntity> wrapper = new LambdaQueryWrapper<OcrNodeCallEntity>()
                .eq(OcrNodeCallEntity::getNodeId, nodeId)
                .orderByDesc(OcrNodeCallEntity::getStartedAt)
                .orderByDesc(OcrNodeCallEntity::getId);
        return page(MybatisPlusPages.limit(limit), wrapper).getRecords().stream().map(this::toDomain).toList();
    }

    /**
     * 将领域调用记录转换为持久化实体。
     *
     * @param call OCR 调用记录
     * @return OCR 调用实体
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private OcrNodeCallEntity toEntity(OcrNodeCall call) {
        OcrNodeCallEntity entity = new OcrNodeCallEntity();
        entity.setId(call.id());
        entity.setBatchId(call.batchId());
        entity.setDocumentId(call.documentId());
        entity.setPageNo(call.pageNo());
        entity.setModelKey(call.modelKey());
        entity.setNodeId(call.nodeId());
        entity.setRoutingMode(call.routingMode().name());
        entity.setStatus(call.status().name());
        entity.setRetryCount(call.retryCount());
        entity.setElapsedMs(call.elapsedMs());
        entity.setErrorCode(call.errorCode().orElse(null));
        entity.setErrorMessage(call.errorMessage().orElse(null));
        entity.setStartedAt(call.startedAt());
        entity.setFinishedAt(call.finishedAt().orElse(null));
        return entity;
    }

    /**
     * 将持久化实体转换为领域调用记录。
     *
     * @param entity OCR 调用实体
     * @return OCR 调用记录
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private OcrNodeCall toDomain(OcrNodeCallEntity entity) {
        return new OcrNodeCall(entity.getId(), entity.getBatchId(), entity.getDocumentId(), entity.getPageNo(),
                entity.getModelKey(), entity.getNodeId(), OcrRoutingMode.valueOf(entity.getRoutingMode()),
                OcrNodeCallStatus.valueOf(entity.getStatus()), entity.getRetryCount(), entity.getElapsedMs(),
                Optional.ofNullable(entity.getErrorCode()), Optional.ofNullable(entity.getErrorMessage()),
                entity.getStartedAt(), Optional.ofNullable(entity.getFinishedAt()));
    }
}
