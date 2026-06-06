package com.doclens.processing.infrastructure;

import com.doclens.processing.domain.OcrResult;
import com.doclens.processing.domain.OcrResultRepository;
import com.doclens.shared.infrastructure.JsonCodec;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

/**
 * JDBC implementation of OCR result repository.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@Repository
public class JdbcOcrResultRepository implements OcrResultRepository {

    private static final TypeReference<List<Map<String, Object>>> LIST_OF_OBJECTS = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> LIST_OF_STRINGS = new TypeReference<>() {
    };
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JsonCodec jsonCodec;
    private final ObjectMapper objectMapper;

    /**
     * Creates JDBC OCR result repository.
     *
     * @param jdbcTemplate named parameter JDBC template
     * @param jsonCodec JSON codec
     * @param objectMapper Jackson mapper
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public JdbcOcrResultRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            JsonCodec jsonCodec,
            ObjectMapper objectMapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.jsonCodec = jsonCodec;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(OcrResult result) {
        jdbcTemplate.update(insertSql(), toParams(result));
    }

    @Override
    public void saveAll(List<OcrResult> results) {
        jdbcTemplate.batchUpdate(insertSql(), results.stream().map(this::toParams).toArray(SqlParameterSource[]::new));
    }

    @Override
    public Optional<OcrResult> findByDocumentId(String documentId) {
        String sql = "SELECT * FROM ocr_results WHERE document_id = :document_id LIMIT 1";
        List<OcrResult> results = jdbcTemplate.query(sql, Map.of("document_id", documentId), this::mapRow);
        if (results.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(results.getFirst());
        }
    }

    private String insertSql() {
        return """
                INSERT INTO ocr_results (
                    result_id, document_id, raw_vendor_output, structured_document, page_text,
                    layout_blocks, tables, images, confidence, warnings, created_at
                ) VALUES (
                    :result_id, :document_id, :raw_vendor_output, :structured_document, :page_text,
                    :layout_blocks, :tables, :images, :confidence, :warnings, :created_at
                )
                """;
    }

    private MapSqlParameterSource toParams(OcrResult result) {
        return new MapSqlParameterSource()
                .addValue("result_id", result.resultId())
                .addValue("document_id", result.documentId())
                .addValue("raw_vendor_output", jsonCodec.toJson(result.rawVendorOutput()))
                .addValue("structured_document", jsonCodec.toJson(result.structuredDocument()))
                .addValue("page_text", jsonCodec.toJson(result.pageText()))
                .addValue("layout_blocks", jsonCodec.toJson(result.layoutBlocks()))
                .addValue("tables", jsonCodec.toJson(result.tables()))
                .addValue("images", jsonCodec.toJson(result.images()))
                .addValue("confidence", result.confidence())
                .addValue("warnings", jsonCodec.toJson(result.warnings()))
                .addValue("created_at", result.createdAt());
    }

    private OcrResult mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new OcrResult(
                rs.getString("result_id"),
                rs.getString("document_id"),
                jsonCodec.parseObject(rs.getString("raw_vendor_output")),
                jsonCodec.parseObject(rs.getString("structured_document")),
                readValue(rs.getString("page_text"), LIST_OF_OBJECTS),
                readValue(rs.getString("layout_blocks"), LIST_OF_OBJECTS),
                readValue(rs.getString("tables"), LIST_OF_OBJECTS),
                readValue(rs.getString("images"), LIST_OF_OBJECTS),
                rs.getDouble("confidence"),
                readValue(rs.getString("warnings"), LIST_OF_STRINGS),
                rs.getObject("created_at", OffsetDateTime.class)
        );
    }

    private <T> T readValue(String payload, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(payload, typeReference);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("failed to deserialize stored JSON", ex);
        }
    }
}
