package com.doclens.ingestion.infrastructure;

import com.doclens.ingestion.domain.Batch;
import com.doclens.ingestion.domain.BatchRepository;
import com.doclens.ingestion.domain.BatchStatus;
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
import org.springframework.stereotype.Repository;

/**
 * JDBC implementation of batch repository.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@Repository
public class JdbcBatchRepository implements BatchRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JsonCodec jsonCodec;

    /**
     * Creates JDBC batch repository.
     *
     * @param jdbcTemplate named parameter JDBC template
     * @param jsonCodec JSON codec
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public JdbcBatchRepository(NamedParameterJdbcTemplate jdbcTemplate, JsonCodec jsonCodec) {
        this.jdbcTemplate = jdbcTemplate;
        this.jsonCodec = jsonCodec;
    }

    @Override
    public void save(Batch batch) {
        String sql = """
                INSERT INTO ocr_batches (
                    batch_id, status, total_files, completed_files, failed_files,
                    current_document_id, current_document_name, current_stage, metadata,
                    callback_url, idempotency_key, created_at, updated_at
                ) VALUES (
                    :batch_id, :status, :total_files, :completed_files, :failed_files,
                    :current_document_id, :current_document_name, :current_stage, :metadata,
                    :callback_url, :idempotency_key, :created_at, :updated_at
                )
                """;
        jdbcTemplate.update(sql, toParams(batch));
    }

    @Override
    public Optional<Batch> findById(String batchId) {
        return findOne("SELECT * FROM ocr_batches WHERE batch_id = :batch_id LIMIT 1", Map.of("batch_id", batchId));
    }

    @Override
    public Optional<Batch> findByIdempotencyKey(String idempotencyKey) {
        return findOne("SELECT * FROM ocr_batches WHERE idempotency_key = :idempotency_key LIMIT 1",
                Map.of("idempotency_key", idempotencyKey));
    }

    @Override
    public void updateSummary(String batchId, int completedFiles, int failedFiles, BatchStatus status) {
        String sql = """
                UPDATE ocr_batches
                SET status = :status,
                    completed_files = :completed_files,
                    failed_files = :failed_files,
                    current_document_id = NULL,
                    current_document_name = NULL,
                    current_stage = :current_stage,
                    updated_at = :updated_at
                WHERE batch_id = :batch_id
                """;
        jdbcTemplate.update(sql, Map.of(
                "status", status.name().toLowerCase(),
                "completed_files", completedFiles,
                "failed_files", failedFiles,
                "current_stage", status.name().toLowerCase(),
                "updated_at", OffsetDateTime.now(),
                "batch_id", batchId
        ));
    }

    private Optional<Batch> findOne(String sql, Map<String, ?> params) {
        List<Batch> batches = jdbcTemplate.query(sql, params, this::mapRow);
        if (batches.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(batches.getFirst());
        }
    }

    private MapSqlParameterSource toParams(Batch batch) {
        return new MapSqlParameterSource()
                .addValue("batch_id", batch.batchId())
                .addValue("status", batch.status().name().toLowerCase())
                .addValue("total_files", batch.totalFiles())
                .addValue("completed_files", batch.completedFiles())
                .addValue("failed_files", batch.failedFiles())
                .addValue("current_document_id", batch.currentDocumentId().orElse(null))
                .addValue("current_document_name", batch.currentDocumentName().orElse(DocLensConstants.EMPTY_VALUE))
                .addValue("current_stage", batch.currentStage())
                .addValue("metadata", jsonCodec.toJson(batch.metadata().values()))
                .addValue("callback_url", batch.callbackUrl().orElse(null))
                .addValue("idempotency_key", batch.idempotencyKey().orElse(null))
                .addValue("created_at", batch.createdAt())
                .addValue("updated_at", batch.updatedAt());
    }

    private Batch mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Batch(
                rs.getString("batch_id"),
                BatchStatus.valueOf(rs.getString("status").toUpperCase()),
                rs.getInt("total_files"),
                rs.getInt("completed_files"),
                rs.getInt("failed_files"),
                Optional.ofNullable(rs.getString("current_document_id")),
                Optional.ofNullable(rs.getString("current_document_name")).filter(value -> !value.isBlank()),
                rs.getString("current_stage"),
                new JsonPayload(jsonCodec.parseObject(rs.getString("metadata"))),
                Optional.ofNullable(rs.getString("callback_url")),
                Optional.ofNullable(rs.getString("idempotency_key")),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class)
        );
    }
}
