package com.doclens.processing.infrastructure;

import com.doclens.processing.domain.DocumentJob;
import com.doclens.processing.domain.DocumentJobRepository;
import com.doclens.processing.domain.DocumentStatus;
import com.doclens.processing.domain.DocumentType;
import com.doclens.processing.domain.PdfMode;
import com.doclens.processing.domain.ProcessingStage;
import com.doclens.shared.domain.DocLensConstants;
import com.doclens.shared.domain.JsonPayload;
import com.doclens.shared.infrastructure.JsonCodec;
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
 * JDBC implementation of document job repository.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@Repository
public class JdbcDocumentJobRepository implements DocumentJobRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JsonCodec jsonCodec;

    /**
     * Creates JDBC document job repository.
     *
     * @param jdbcTemplate named parameter JDBC template
     * @param jsonCodec JSON codec
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public JdbcDocumentJobRepository(NamedParameterJdbcTemplate jdbcTemplate, JsonCodec jsonCodec) {
        this.jdbcTemplate = jdbcTemplate;
        this.jsonCodec = jsonCodec;
    }

    @Override
    public void save(DocumentJob document) {
        jdbcTemplate.update(insertSql(), toParams(document));
    }

    @Override
    public void saveAll(List<DocumentJob> documents) {
        jdbcTemplate.batchUpdate(insertSql(), batchParams(documents));
    }

    @Override
    public void update(DocumentJob document) {
        jdbcTemplate.update(updateSql(), toParams(document));
    }

    @Override
    public void updateAll(List<DocumentJob> documents) {
        jdbcTemplate.batchUpdate(updateSql(), batchParams(documents));
    }

    @Override
    public Optional<DocumentJob> findById(String documentId) {
        String sql = "SELECT * FROM ocr_documents WHERE document_id = :document_id LIMIT 1";
        List<DocumentJob> documents = jdbcTemplate.query(sql, Map.of("document_id", documentId), this::mapRow);
        if (documents.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(documents.getFirst());
        }
    }

    @Override
    public List<DocumentJob> listByBatchId(String batchId) {
        String sql = """
                SELECT * FROM ocr_documents
                WHERE batch_id = :batch_id
                ORDER BY sort_order ASC, document_id ASC
                LIMIT :limit
                """;
        return jdbcTemplate.query(sql, Map.of("batch_id", batchId, "limit", DocLensConstants.DEFAULT_QUERY_LIMIT), this::mapRow);
    }

    private String insertSql() {
        return """
                INSERT INTO ocr_documents (
                    document_id, batch_id, file_name, file_type, file_size, page_count,
                    storage_uri, status, stage, progress_percent, current_page, total_pages,
                    adapter_name, pdf_mode, metadata, result_id, error_code, error_message,
                    sort_order, created_at, updated_at
                ) VALUES (
                    :document_id, :batch_id, :file_name, :file_type, :file_size, :page_count,
                    :storage_uri, :status, :stage, :progress_percent, :current_page, :total_pages,
                    :adapter_name, :pdf_mode, :metadata, :result_id, :error_code, :error_message,
                    :sort_order, :created_at, :updated_at
                )
                """;
    }

    private String updateSql() {
        return """
                UPDATE ocr_documents
                SET status = :status,
                    stage = :stage,
                    progress_percent = :progress_percent,
                    current_page = :current_page,
                    total_pages = :total_pages,
                    result_id = :result_id,
                    error_code = :error_code,
                    error_message = :error_message,
                    updated_at = :updated_at
                WHERE document_id = :document_id
                """;
    }

    private SqlParameterSource[] batchParams(List<DocumentJob> documents) {
        return documents.stream().map(this::toParams).toArray(SqlParameterSource[]::new);
    }

    private MapSqlParameterSource toParams(DocumentJob document) {
        return new MapSqlParameterSource()
                .addValue("document_id", document.documentId())
                .addValue("batch_id", document.batchId())
                .addValue("file_name", document.fileName())
                .addValue("file_type", document.fileType().name().toLowerCase())
                .addValue("file_size", document.fileSize())
                .addValue("page_count", document.pageCount())
                .addValue("storage_uri", document.storageUri())
                .addValue("status", document.status().name().toLowerCase())
                .addValue("stage", document.stage().name().toLowerCase())
                .addValue("progress_percent", document.progressPercent())
                .addValue("current_page", document.currentPage())
                .addValue("total_pages", document.totalPages())
                .addValue("adapter_name", document.adapterName())
                .addValue("pdf_mode", document.pdfMode().map(mode -> mode.name().toLowerCase()).orElse(null))
                .addValue("metadata", jsonCodec.toJson(document.metadata().values()))
                .addValue("result_id", document.resultId().orElse(null))
                .addValue("error_code", document.errorCode().orElse(null))
                .addValue("error_message", document.errorMessage().orElse(null))
                .addValue("sort_order", document.sortOrder())
                .addValue("created_at", document.createdAt())
                .addValue("updated_at", document.updatedAt());
    }

    private DocumentJob mapRow(ResultSet rs, int rowNum) throws SQLException {
        String pdfMode = rs.getString("pdf_mode");
        return new DocumentJob(
                rs.getString("document_id"),
                rs.getString("batch_id"),
                rs.getString("file_name"),
                DocumentType.valueOf(rs.getString("file_type").toUpperCase()),
                rs.getLong("file_size"),
                rs.getInt("page_count"),
                rs.getString("storage_uri"),
                DocumentStatus.valueOf(rs.getString("status").toUpperCase()),
                ProcessingStage.valueOf(rs.getString("stage").toUpperCase()),
                rs.getInt("progress_percent"),
                rs.getInt("current_page"),
                rs.getInt("total_pages"),
                rs.getString("adapter_name"),
                Optional.ofNullable(pdfMode).map(value -> PdfMode.valueOf(value.toUpperCase())),
                new JsonPayload(jsonCodec.parseObject(rs.getString("metadata"))),
                Optional.ofNullable(rs.getString("result_id")),
                Optional.ofNullable(rs.getString("error_code")),
                Optional.ofNullable(rs.getString("error_message")),
                rs.getInt("sort_order"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class)
        );
    }
}
