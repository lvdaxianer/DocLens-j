package com.doclens.processing.infrastructure;

import com.doclens.processing.domain.OcrEvent;
import com.doclens.processing.domain.OcrEventRepository;
import com.doclens.shared.domain.DocLensConstants;
import com.doclens.shared.domain.JsonPayload;
import com.doclens.shared.infrastructure.JsonCodec;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * JDBC implementation of OCR event repository.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@Repository
public class JdbcOcrEventRepository implements OcrEventRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JsonCodec jsonCodec;

    /**
     * Creates JDBC OCR event repository.
     *
     * @param jdbcTemplate named parameter JDBC template
     * @param jsonCodec JSON codec
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public JdbcOcrEventRepository(NamedParameterJdbcTemplate jdbcTemplate, JsonCodec jsonCodec) {
        this.jdbcTemplate = jdbcTemplate;
        this.jsonCodec = jsonCodec;
    }

    @Override
    public void save(OcrEvent event) {
        jdbcTemplate.update(insertSql(), toParams(event));
    }

    @Override
    public void saveAll(List<OcrEvent> events) {
        jdbcTemplate.batchUpdate(insertSql(), events.stream().map(this::toParams).toArray(SqlParameterSource[]::new));
    }

    @Override
    public List<OcrEvent> listByBatchId(String batchId) {
        String sql = """
                SELECT * FROM ocr_events
                WHERE batch_id = :batch_id
                ORDER BY occurred_at ASC, event_id ASC
                LIMIT :limit
                """;
        return jdbcTemplate.query(sql, Map.of("batch_id", batchId, "limit", DocLensConstants.DEFAULT_QUERY_LIMIT), this::mapRow);
    }

    private String insertSql() {
        return """
                INSERT INTO ocr_events (
                    event_id, event_type, batch_id, document_id, status, stage,
                    progress, metadata, result_id, result_summary, error, occurred_at
                ) VALUES (
                    :event_id, :event_type, :batch_id, :document_id, :status, :stage,
                    :progress, :metadata, :result_id, :result_summary, :error, :occurred_at
                )
                """;
    }

    private MapSqlParameterSource toParams(OcrEvent event) {
        return new MapSqlParameterSource()
                .addValue("event_id", event.eventId())
                .addValue("event_type", event.eventType())
                .addValue("batch_id", event.batchId())
                .addValue("document_id", event.documentId().orElse(null))
                .addValue("status", event.status())
                .addValue("stage", event.stage())
                .addValue("progress", jsonCodec.toJson(event.progress()))
                .addValue("metadata", jsonCodec.toJson(event.metadata().values()))
                .addValue("result_id", event.resultId().orElse(null))
                .addValue("result_summary", event.resultSummary().isEmpty() ? null : jsonCodec.toJson(event.resultSummary()))
                .addValue("error", event.error().isEmpty() ? null : jsonCodec.toJson(event.error()))
                .addValue("occurred_at", event.occurredAt());
    }

    private OcrEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
        String resultSummary = rs.getString("result_summary");
        String error = rs.getString("error");
        return new OcrEvent(
                rs.getString("event_id"),
                rs.getString("event_type"),
                rs.getString("batch_id"),
                Optional.ofNullable(rs.getString("document_id")),
                rs.getString("status"),
                rs.getString("stage"),
                jsonCodec.parseObject(rs.getString("progress")),
                new JsonPayload(jsonCodec.parseObject(rs.getString("metadata"))),
                Optional.ofNullable(rs.getString("result_id")),
                resultSummary == null ? null : jsonCodec.parseObject(resultSummary),
                error == null ? null : jsonCodec.parseObject(error),
                rs.getObject("occurred_at", OffsetDateTime.class)
        );
    }
}
