package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResult;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResultRepository;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.JsonCodec;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.MybatisPlusPages;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * OCR 结果仓储的 MyBatis-Plus 实现。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@Repository
public class MybatisPlusOcrResultRepository
        extends ServiceImpl<OcrResultMapper, OcrResultEntity>
        implements OcrResultRepository {

    private static final TypeReference<List<Map<String, Object>>> LIST_OF_OBJECTS = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> LIST_OF_STRINGS = new TypeReference<>() {
    };

    private final JsonCodec jsonCodec;
    private final ObjectMapper objectMapper;

    /**
     * 创建 MyBatis-Plus OCR 结果仓储。
     *
     * @param jsonCodec JSON 编解码器
     * @param objectMapper Jackson 映射器
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public MybatisPlusOcrResultRepository(JsonCodec jsonCodec, ObjectMapper objectMapper) {
        this.jsonCodec = jsonCodec;
        this.objectMapper = objectMapper;
    }

    /**
     * 保存单个 OCR 结果。
     *
     * @param result OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Override
    public void save(OcrResult result) {
        super.save(toEntity(result));
    }

    /**
     * 批量保存 OCR 结果。
     *
     * @param results OCR 结果集合
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Override
    public void saveAll(List<OcrResult> results) {
        saveBatch(results.stream().map(this::toEntity).toList());
    }

    /**
     * 根据文档 ID 查找单个 OCR 结果，并显式限制单行结果。
     *
     * @param documentId 文档 ID
     * @return 可选 OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Override
    public Optional<OcrResult> findByDocumentId(String documentId) {
        LambdaQueryWrapper<OcrResultEntity> wrapper = new LambdaQueryWrapper<OcrResultEntity>()
                .eq(OcrResultEntity::getDocumentId, documentId);
        return page(MybatisPlusPages.one(), wrapper).getRecords().stream().findFirst().map(this::toDomain);
    }

    /**
     * 根据文档 ID 集合批量查找 OCR 结果。
     *
     * @param documentIds 文档 ID 集合
     * @return OCR 结果集合
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public List<OcrResult> findByDocumentIds(List<String> documentIds) {
        if (documentIds.isEmpty()) {
            return List.of();
        } else {
            LambdaQueryWrapper<OcrResultEntity> wrapper = new LambdaQueryWrapper<OcrResultEntity>()
                    .in(OcrResultEntity::getDocumentId, documentIds);
            return list(wrapper).stream().map(this::toDomain).toList();
        }
    }

    /**
     * 根据文档 ID 删除 OCR 结果。
     *
     * @param documentId 文档 ID
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public void deleteByDocumentId(String documentId) {
        LambdaQueryWrapper<OcrResultEntity> wrapper = new LambdaQueryWrapper<OcrResultEntity>()
                .eq(OcrResultEntity::getDocumentId, documentId);
        remove(wrapper);
    }

    /**
     * 根据文档 ID 集合批量删除 OCR 结果。
     *
     * @param documentIds 文档 ID 集合
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public void deleteByDocumentIds(List<String> documentIds) {
        if (documentIds.isEmpty()) {
            // 空集合无需发起删除语句。
        } else {
            LambdaQueryWrapper<OcrResultEntity> wrapper = new LambdaQueryWrapper<OcrResultEntity>()
                    .in(OcrResultEntity::getDocumentId, documentIds);
            remove(wrapper);
        }
    }

    /**
     * 将领域 OCR 结果转换为持久化实体。
     *
     * @param result OCR 结果
     * @return OCR 结果持久化实体
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    private OcrResultEntity toEntity(OcrResult result) {
        OcrResultEntity entity = new OcrResultEntity();
        entity.setResultId(result.resultId());
        entity.setDocumentId(result.documentId());
        entity.setFinalText(result.finalText());
        entity.setMarkdownStorageUri(result.markdownStorageUri());
        entity.setRawVendorOutput(jsonCodec.toJson(result.rawVendorOutput()));
        entity.setStructuredDocument(jsonCodec.toJson(result.structuredDocument()));
        entity.setPageText(jsonCodec.toJson(result.pageText()));
        entity.setLayoutBlocks(jsonCodec.toJson(result.layoutBlocks()));
        entity.setTables(jsonCodec.toJson(result.tables()));
        entity.setImages(jsonCodec.toJson(result.images()));
        entity.setConfidence(result.confidence());
        entity.setWarnings(jsonCodec.toJson(result.warnings()));
        entity.setCreatedAt(result.createdAt());
        return entity;
    }

    /**
     * 将持久化实体转换为领域 OCR 结果。
     *
     * @param entity OCR 结果持久化实体
     * @return OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    private OcrResult toDomain(OcrResultEntity entity) {
        return new OcrResult(entity.getResultId(), entity.getDocumentId(), entity.getFinalText(),
                entity.getMarkdownStorageUri(),
                jsonCodec.parseObject(entity.getRawVendorOutput()),
                jsonCodec.parseObject(entity.getStructuredDocument()),
                readValue(entity.getPageText(), LIST_OF_OBJECTS),
                readValue(entity.getLayoutBlocks(), LIST_OF_OBJECTS),
                readValue(entity.getTables(), LIST_OF_OBJECTS),
                readValue(entity.getImages(), LIST_OF_OBJECTS),
                entity.getConfidence(), readValue(entity.getWarnings(), LIST_OF_STRINGS), entity.getCreatedAt());
    }

    /**
     * 按请求类型读取 JSON 载荷。
     *
     * @param payload JSON 载荷
     * @param typeReference 目标类型引用
     * @param <T> 目标类型
     * @return 解析后的值
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    private <T> T readValue(String payload, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(payload, typeReference);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("failed to deserialize stored JSON", ex);
        }
    }
}
