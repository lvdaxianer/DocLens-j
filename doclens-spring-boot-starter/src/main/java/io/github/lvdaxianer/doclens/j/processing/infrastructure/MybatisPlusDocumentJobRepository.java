package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutePolicy;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutingMode;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentType;
import io.github.lvdaxianer.doclens.j.processing.domain.PdfMode;
import io.github.lvdaxianer.doclens.j.processing.domain.ProcessingStage;
import io.github.lvdaxianer.doclens.j.processing.application.ChunkStrategy;
import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.JsonCodec;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.MybatisPlusPages;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.stereotype.Repository;

/**
 * 文档任务仓储的 MyBatis-Plus 实现。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@Repository
public class MybatisPlusDocumentJobRepository
        extends ServiceImpl<DocumentJobMapper, DocumentJobEntity>
        implements DocumentJobRepository {

    private static final Map<OcrRoutingMode, Function<DocumentJobEntity, OcrRoutePolicy>> ROUTE_POLICY_FACTORIES =
            routePolicyFactories();

    private final JsonCodec jsonCodec;

    /**
     * 创建 MyBatis-Plus 文档任务仓储。
     *
     * @param jsonCodec JSON 编解码器
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public MybatisPlusDocumentJobRepository(JsonCodec jsonCodec) {
        this.jsonCodec = jsonCodec;
    }

    /**
     * 保存单个文档任务。
     *
     * @param document 文档任务
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Override
    public void save(DocumentJob document) {
        super.save(toEntity(document));
    }

    /**
     * 批量保存文档任务。
     *
     * @param documents 文档任务集合
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Override
    public void saveAll(List<DocumentJob> documents) {
        saveBatch(documents.stream().map(this::toEntity).toList());
    }

    /**
     * 更新单个文档任务。
     *
     * @param document 文档任务
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Override
    public void update(DocumentJob document) {
        updateById(toEntity(document));
    }

    /**
     * 批量更新文档任务。
     *
     * @param documents 文档任务集合
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Override
    public void updateAll(List<DocumentJob> documents) {
        updateBatchById(documents.stream().map(this::toEntity).toList());
    }

    /**
     * 根据 ID 查找单个文档任务。
     *
     * @param documentId 文档 ID
     * @return 可选文档任务
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Override
    public Optional<DocumentJob> findById(String documentId) {
        return Optional.ofNullable(getById(documentId)).map(this::toDomain);
    }

    /**
     * 按上传顺序列出文档任务，并限制分页大小。
     *
     * @param batchId 批次 ID
     * @return 文档任务集合
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Override
    public List<DocumentJob> listByBatchId(String batchId) {
        LambdaQueryWrapper<DocumentJobEntity> wrapper = new LambdaQueryWrapper<DocumentJobEntity>()
                .eq(DocumentJobEntity::getBatchId, batchId)
                .orderByAsc(DocumentJobEntity::getSortOrder)
                .orderByAsc(DocumentJobEntity::getDocumentId);
        return page(MybatisPlusPages.listLimit(), wrapper).getRecords().stream().map(this::toDomain).toList();
    }

    /**
     * 按批次 ID 集合批量列出文档任务。
     *
     * @param batchIds 批次 ID 集合
     * @return 文档任务集合
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Override
    public List<DocumentJob> listByBatchIds(List<String> batchIds) {
        if (batchIds.isEmpty()) {
            return List.of();
        } else {
            LambdaQueryWrapper<DocumentJobEntity> wrapper = new LambdaQueryWrapper<DocumentJobEntity>()
                    .in(DocumentJobEntity::getBatchId, batchIds)
                    .orderByAsc(DocumentJobEntity::getBatchId)
                    .orderByAsc(DocumentJobEntity::getSortOrder);
            return page(MybatisPlusPages.limit(batchIds.size() * DocLensConstants.DEFAULT_QUERY_LIMIT), wrapper)
                    .getRecords().stream().map(this::toDomain).toList();
        }
    }

    /**
     * 按更新时间倒序列出最近文档任务。
     *
     * @param limit 最大返回数量
     * @return 最近文档任务集合
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Override
    public List<DocumentJob> listRecent(int limit) {
        LambdaQueryWrapper<DocumentJobEntity> wrapper = new LambdaQueryWrapper<DocumentJobEntity>()
                .orderByDesc(DocumentJobEntity::getUpdatedAt)
                .orderByDesc(DocumentJobEntity::getDocumentId);
        return page(MybatisPlusPages.limit(limit), wrapper).getRecords().stream().map(this::toDomain).toList();
    }

    /**
     * 按状态列出文档任务。
     *
     * @param status 文档状态
     * @param limit 最大返回数量
     * @return 目标状态文档集合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public List<DocumentJob> listByStatus(DocumentStatus status, int limit) {
        LambdaQueryWrapper<DocumentJobEntity> wrapper = new LambdaQueryWrapper<DocumentJobEntity>()
                .eq(DocumentJobEntity::getStatus, status.name().toLowerCase())
                .orderByAsc(DocumentJobEntity::getUpdatedAt)
                .orderByAsc(DocumentJobEntity::getDocumentId);
        return page(MybatisPlusPages.limit(limit), wrapper).getRecords().stream().map(this::toDomain).toList();
    }

    /**
     * 根据文档 ID 删除文档任务。
     *
     * @param documentId 文档 ID
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public void deleteById(String documentId) {
        removeById(documentId);
    }

    /**
     * 根据文档 ID 集合批量删除文档任务。
     *
     * @param documentIds 文档 ID 集合
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public void deleteByIds(List<String> documentIds) {
        if (documentIds.isEmpty()) {
            // 空集合无需发起删除语句。
        } else {
            removeByIds(documentIds);
        }
    }

    /**
     * 将领域文档任务转换为持久化实体。
     *
     * @param document 文档任务
     * @return 文档持久化实体
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    private DocumentJobEntity toEntity(DocumentJob document) {
        DocumentJobEntity entity = new DocumentJobEntity();
        entity.setDocumentId(document.documentId());
        entity.setBatchId(document.batchId());
        entity.setFileName(document.fileName());
        entity.setFileType(document.fileType().name().toLowerCase());
        entity.setFileSize(document.fileSize());
        entity.setPageCount(document.pageCount());
        entity.setStorageUri(document.storageUri());
        entity.setStatus(document.status().name().toLowerCase());
        entity.setStage(document.stage().name().toLowerCase());
        entity.setProgressPercent(document.progressPercent());
        entity.setCurrentPage(document.currentPage());
        entity.setTotalPages(document.totalPages());
        entity.setAdapterName(document.adapterName());
        entity.setPdfMode(document.pdfMode().map(mode -> mode.name().toLowerCase()).orElse(null));
        entity.setChunkStrategy(document.chunkStrategy().name());
        entity.setOcrRoutingMode(document.ocrRoutePolicy().routingMode().name());
        entity.setOcrModelKey(document.ocrRoutePolicy().modelKey().orElse(null));
        entity.setOcrNodeId(document.ocrRoutePolicy().nodeId().orElse(null));
        entity.setOcrLoadBalanceStrategy(document.ocrRoutePolicy().loadBalanceStrategy().orElse(null));
        entity.setMetadata(jsonCodec.toJson(document.metadata().values()));
        entity.setResultId(document.resultId().orElse(null));
        entity.setErrorCode(document.errorCode().orElse(null));
        entity.setErrorMessage(document.errorMessage().orElse(null));
        entity.setSortOrder(document.sortOrder());
        entity.setCreatedAt(document.createdAt());
        entity.setUpdatedAt(document.updatedAt());
        return entity;
    }

    /**
     * 将持久化实体转换为领域文档任务。
     *
     * @param entity 文档持久化实体
     * @return 文档任务
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    private DocumentJob toDomain(DocumentJobEntity entity) {
        String pdfMode = entity.getPdfMode();
        return new DocumentJob(entity.getDocumentId(), entity.getBatchId(), entity.getFileName(),
                DocumentType.valueOf(entity.getFileType().toUpperCase()), entity.getFileSize(), entity.getPageCount(),
                entity.getStorageUri(), DocumentStatus.valueOf(entity.getStatus().toUpperCase()),
                ProcessingStage.valueOf(entity.getStage().toUpperCase()), entity.getProgressPercent(),
                entity.getCurrentPage(), entity.getTotalPages(), entity.getAdapterName(),
                Optional.ofNullable(pdfMode).map(value -> PdfMode.valueOf(value.toUpperCase())),
                ChunkStrategy.from(entity.getChunkStrategy()),
                toOcrRoutePolicy(entity),
                new JsonPayload(jsonCodec.parseObject(entity.getMetadata())), Optional.ofNullable(entity.getResultId()),
                Optional.ofNullable(entity.getErrorCode()), Optional.ofNullable(entity.getErrorMessage()),
                entity.getSortOrder(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    /**
     * 将实体中的 OCR 路由字段还原为领域策略。
     *
     * @param entity 文档持久化实体
     * @return OCR 路由策略
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private OcrRoutePolicy toOcrRoutePolicy(DocumentJobEntity entity) {
        OcrRoutingMode routingMode = toOcrRoutingMode(entity.getOcrRoutingMode());
        return ROUTE_POLICY_FACTORIES.get(routingMode).apply(entity);
    }

    /**
     * 解析持久化 OCR 路由模式。
     *
     * @param routingMode OCR 路由模式文本
     * @return OCR 路由模式
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private OcrRoutingMode toOcrRoutingMode(String routingMode) {
        if (routingMode == null || routingMode.isBlank()) {
            return OcrRoutingMode.DEFAULT;
        } else {
            return OcrRoutingMode.valueOf(routingMode);
        }
    }

    /**
     * 创建 OCR 路由策略还原工厂。
     *
     * @return OCR 路由策略还原工厂
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static Map<OcrRoutingMode, Function<DocumentJobEntity, OcrRoutePolicy>> routePolicyFactories() {
        Map<OcrRoutingMode, Function<DocumentJobEntity, OcrRoutePolicy>> factories =
                new EnumMap<>(OcrRoutingMode.class);
        factories.put(OcrRoutingMode.DEFAULT, entity -> OcrRoutePolicy.defaultPolicy());
        factories.put(OcrRoutingMode.GLOBAL_LOAD_BALANCE,
                entity -> OcrRoutePolicy.globalLoadBalance(entity.getOcrLoadBalanceStrategy()));
        factories.put(OcrRoutingMode.MODEL_LOAD_BALANCE,
                entity -> OcrRoutePolicy.modelLoadBalance(entity.getOcrModelKey(),
                        entity.getOcrLoadBalanceStrategy()));
        factories.put(OcrRoutingMode.SPECIFIC_NODE,
                entity -> OcrRoutePolicy.specificNode(entity.getOcrModelKey(), entity.getOcrNodeId()));
        return Map.copyOf(factories);
    }
}
