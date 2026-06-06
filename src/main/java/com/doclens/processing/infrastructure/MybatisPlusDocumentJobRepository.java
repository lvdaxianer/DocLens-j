package com.doclens.processing.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.doclens.processing.domain.DocumentJob;
import com.doclens.processing.domain.DocumentJobRepository;
import com.doclens.processing.domain.DocumentStatus;
import com.doclens.processing.domain.DocumentType;
import com.doclens.processing.domain.PdfMode;
import com.doclens.processing.domain.ProcessingStage;
import com.doclens.shared.domain.DocLensConstants;
import com.doclens.shared.domain.JsonPayload;
import com.doclens.shared.infrastructure.JsonCodec;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * MyBatis-Plus implementation of document job repository.
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
     * Creates MyBatis-Plus document job repository.
     *
     * @param jsonCodec JSON codec
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public MybatisPlusDocumentJobRepository(JsonCodec jsonCodec) {
        this.jsonCodec = jsonCodec;
    }

    @Override
    public void save(DocumentJob document) {
        super.save(toEntity(document));
    }

    @Override
    public void saveAll(List<DocumentJob> documents) {
        saveBatch(documents.stream().map(this::toEntity).toList());
    }

    @Override
    public void update(DocumentJob document) {
        updateById(toEntity(document));
    }

    @Override
    public void updateAll(List<DocumentJob> documents) {
        updateBatchById(documents.stream().map(this::toEntity).toList());
    }

    @Override
    public Optional<DocumentJob> findById(String documentId) {
        return Optional.ofNullable(getById(documentId)).map(this::toDomain);
    }

    @Override
    public List<DocumentJob> listByBatchId(String batchId) {
        LambdaQueryWrapper<DocumentJobEntity> wrapper = new LambdaQueryWrapper<DocumentJobEntity>()
                .eq(DocumentJobEntity::getBatchId, batchId)
                .orderByAsc(DocumentJobEntity::getSortOrder)
                .orderByAsc(DocumentJobEntity::getDocumentId);
        Page<DocumentJobEntity> page = Page.of(DocLensConstants.FIRST_PAGE_NO, DocLensConstants.DEFAULT_QUERY_LIMIT);
        return page(page, wrapper).getRecords().stream().map(this::toDomain).toList();
    }

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
