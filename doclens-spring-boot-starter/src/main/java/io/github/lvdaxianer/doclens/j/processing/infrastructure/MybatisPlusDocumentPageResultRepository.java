package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageResult;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageResultRepository;
import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.JsonCodec;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.MybatisPlusPages;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 文档页 OCR 结果仓储的 MyBatis-Plus 实现。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
@Repository
public class MybatisPlusDocumentPageResultRepository
        extends ServiceImpl<DocumentPageResultMapper, DocumentPageResultEntity>
        implements DocumentPageResultRepository {

    private static final TypeReference<List<Map<String, Object>>> LIST_OF_OBJECTS = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> LIST_OF_STRINGS = new TypeReference<>() {
    };

    private final JsonCodec jsonCodec;
    private final ObjectMapper objectMapper;

    /**
     * 创建 MyBatis-Plus 文档页 OCR 结果仓储。
     *
     * @param jsonCodec JSON 编解码器
     * @param objectMapper Jackson 映射器
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public MybatisPlusDocumentPageResultRepository(JsonCodec jsonCodec, ObjectMapper objectMapper) {
        this.jsonCodec = jsonCodec;
        this.objectMapper = objectMapper;
    }

    /**
     * 新增或更新页 OCR 结果。
     *
     * @param result 页 OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public void upsert(DocumentPageResult result) {
        saveOrUpdate(toEntity(result));
    }

    /**
     * 按文档与页码查询页 OCR 结果。
     *
     * @param documentId 文档 ID
     * @param pageNo 页码
     * @return 可选页 OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public Optional<DocumentPageResult> findByDocumentIdAndPageNo(String documentId, int pageNo) {
        LambdaQueryWrapper<DocumentPageResultEntity> wrapper = byDocument(documentId)
                .eq(DocumentPageResultEntity::getPageNo, pageNo);
        return page(MybatisPlusPages.one(), wrapper).getRecords().stream().findFirst().map(this::toDomain);
    }

    /**
     * 按文档查询全部页 OCR 结果。
     *
     * @param documentId 文档 ID
     * @return 页 OCR 结果集合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public List<DocumentPageResult> listByDocumentId(String documentId) {
        LambdaQueryWrapper<DocumentPageResultEntity> wrapper = byDocument(documentId)
                .orderByAsc(DocumentPageResultEntity::getPageNo);
        return page(MybatisPlusPages.listLimit(), wrapper).getRecords().stream().map(this::toDomain).toList();
    }

    /**
     * 按文档集合批量查询页 OCR 结果。
     *
     * @param documentIds 文档 ID 集合
     * @return 页 OCR 结果集合
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public List<DocumentPageResult> listByDocumentIds(List<String> documentIds) {
        if (documentIds.isEmpty()) {
            return List.of();
        } else {
            LambdaQueryWrapper<DocumentPageResultEntity> wrapper = new LambdaQueryWrapper<DocumentPageResultEntity>()
                    .in(DocumentPageResultEntity::getDocumentId, documentIds)
                    .orderByAsc(DocumentPageResultEntity::getDocumentId)
                    .orderByAsc(DocumentPageResultEntity::getPageNo);
            return page(MybatisPlusPages.limit(documentIds.size() * DocLensConstants.DEFAULT_QUERY_LIMIT), wrapper)
                    .getRecords().stream().map(this::toDomain).toList();
        }
    }

    /**
     * 按文档删除全部页 OCR 结果。
     *
     * @param documentId 文档 ID
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    @Override
    public void deleteByDocumentId(String documentId) {
        remove(byDocument(documentId));
    }

    /**
     * 构造文档维度查询条件。
     *
     * @param documentId 文档 ID
     * @return 查询条件
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private LambdaQueryWrapper<DocumentPageResultEntity> byDocument(String documentId) {
        return new LambdaQueryWrapper<DocumentPageResultEntity>()
                .eq(DocumentPageResultEntity::getDocumentId, documentId);
    }

    /**
     * 将领域页结果转换为持久化实体。
     *
     * @param result 页 OCR 结果
     * @return 持久化实体
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private DocumentPageResultEntity toEntity(DocumentPageResult result) {
        DocumentPageResultEntity entity = new DocumentPageResultEntity();
        entity.setPageResultId(pageResultId(result.documentId(), result.pageNo()));
        entity.setDocumentId(result.documentId());
        entity.setPageNo(result.pageNo());
        entity.setRawOutput(jsonCodec.toJson(result.rawOutput()));
        entity.setPageText(result.pageText());
        entity.setLayoutBlocks(jsonCodec.toJson(result.layoutBlocks()));
        entity.setConfidence(result.confidence());
        entity.setWarnings(jsonCodec.toJson(result.warnings()));
        entity.setNodeId(result.nodeId());
        entity.setCreatedAt(result.createdAt());
        entity.setUpdatedAt(result.updatedAt());
        return entity;
    }

    /**
     * 将持久化实体转换为领域页结果。
     *
     * @param entity 持久化实体
     * @return 页 OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private DocumentPageResult toDomain(DocumentPageResultEntity entity) {
        return new DocumentPageResult(entity.getDocumentId(), entity.getPageNo(),
                jsonCodec.parseObject(entity.getRawOutput()), entity.getPageText(),
                readValue(entity.getLayoutBlocks(), LIST_OF_OBJECTS), entity.getConfidence(),
                readValue(entity.getWarnings(), LIST_OF_STRINGS), entity.getNodeId(), entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    /**
     * 生成页结果主键。
     *
     * @param documentId 文档 ID
     * @param pageNo 页码
     * @return 页结果 ID
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private String pageResultId(String documentId, int pageNo) {
        return documentId + "#" + pageNo;
    }

    /**
     * 按请求类型读取 JSON 载荷。
     *
     * @param payload JSON 载荷
     * @param typeReference 目标类型引用
     * @param <T> 目标类型
     * @return 解析后的值
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private <T> T readValue(String payload, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(payload, typeReference);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("failed to deserialize stored page result JSON", ex);
        }
    }
}
