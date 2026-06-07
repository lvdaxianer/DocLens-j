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
 * MyBatis-Plus implementation of OCR result repository.
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
     * Creates MyBatis-Plus OCR result repository.
     *
     * @param jsonCodec JSON codec
     * @param objectMapper Jackson mapper
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public MybatisPlusOcrResultRepository(JsonCodec jsonCodec, ObjectMapper objectMapper) {
        this.jsonCodec = jsonCodec;
        this.objectMapper = objectMapper;
    }

    /**
     * Saves one OCR result.
     *
     * @param result OCR result
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Override
    public void save(OcrResult result) {
        super.save(toEntity(result));
    }

    /**
     * Saves OCR results in batch.
     *
     * @param results OCR results
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Override
    public void saveAll(List<OcrResult> results) {
        saveBatch(results.stream().map(this::toEntity).toList());
    }

    /**
     * Finds one OCR result by document id with explicit single-row limit.
     *
     * @param documentId document id
     * @return optional OCR result
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
     * Converts domain OCR result to persistence entity.
     *
     * @param result OCR result
     * @return OCR result persistence entity
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    private OcrResultEntity toEntity(OcrResult result) {
        OcrResultEntity entity = new OcrResultEntity();
        entity.setResultId(result.resultId());
        entity.setDocumentId(result.documentId());
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
     * Converts persistence entity to domain OCR result.
     *
     * @param entity OCR result persistence entity
     * @return OCR result
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    private OcrResult toDomain(OcrResultEntity entity) {
        return new OcrResult(entity.getResultId(), entity.getDocumentId(),
                jsonCodec.parseObject(entity.getRawVendorOutput()),
                jsonCodec.parseObject(entity.getStructuredDocument()),
                readValue(entity.getPageText(), LIST_OF_OBJECTS),
                readValue(entity.getLayoutBlocks(), LIST_OF_OBJECTS),
                readValue(entity.getTables(), LIST_OF_OBJECTS),
                readValue(entity.getImages(), LIST_OF_OBJECTS),
                entity.getConfidence(), readValue(entity.getWarnings(), LIST_OF_STRINGS), entity.getCreatedAt());
    }

    /**
     * Reads JSON payload as the requested type.
     *
     * @param payload JSON payload
     * @param typeReference target type reference
     * @param <T> target type
     * @return parsed value
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
