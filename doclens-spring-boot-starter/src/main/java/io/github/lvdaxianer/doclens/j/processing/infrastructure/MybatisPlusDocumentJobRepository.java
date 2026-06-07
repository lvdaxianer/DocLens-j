package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentType;
import io.github.lvdaxianer.doclens.j.processing.domain.PdfMode;
import io.github.lvdaxianer.doclens.j.processing.domain.ProcessingStage;
import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.JsonCodec;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.MybatisPlusPages;
import java.util.List;
import java.util.Optional;
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
                new JsonPayload(jsonCodec.parseObject(entity.getMetadata())), Optional.ofNullable(entity.getResultId()),
                Optional.ofNullable(entity.getErrorCode()), Optional.ofNullable(entity.getErrorMessage()),
                entity.getSortOrder(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
