package io.github.lvdaxianer.doclens.j.ingestion.infrastructure;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.regex.Pattern;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OCR 批次 schema 幂等键约束测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-16
 */
class OcrBatchSchemaIdempotencyKeyTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-16T10:00:00+08:00");
    private static final String IDEMPOTENCY_KEY = "idem-pass-through";
    private static final String V1_MIGRATION = "/db/migration/V1__doclens_ocr_schema.sql";
    private static final Pattern V1_IDEMPOTENCY_UNIQUE_PATTERN =
            Pattern.compile("idempotency_key\\s+VARCHAR\\(256\\)\\s+UNIQUE", Pattern.CASE_INSENSITIVE);
    private static final String CALLER_CLIENT_ID_COLUMN = "CLIENT_ID";
    private static final String CALLER_SOURCE_APP_COLUMN = "SOURCE_APP";
    private static final String CALLER_TENANT_KEY_COLUMN = "TENANT_KEY";
    private static final String FRESH_JDBC_URL = "jdbc:h2:mem:fresh_batch_idempotency_schema;"
            + "MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    private static final String MIGRATED_JDBC_URL = "jdbc:h2:mem:migrated_batch_idempotency_schema;"
            + "MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";

    /**
     * V1 迁移文件已经发布，必须保持历史唯一约束文本以稳定 Flyway checksum。
     *
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    @Test
    void v1MigrationKeepsOriginalIdempotencyKeyDefinition() {
        // V16 负责删除唯一约束，V1 不能再被回改，否则本地既有库会 checksum mismatch。
        assertThat(readMigration(V1_MIGRATION)).containsPattern(V1_IDEMPOTENCY_UNIQUE_PATTERN);
    }

    /**
     * 数据库 schema 不应把 idempotency_key 作为唯一键。
     *
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    @Test
    void schemaAcceptsDuplicateIdempotencyKeys() throws SQLException {
        migrateFreshSchema();
        try (Connection connection = DriverManager.getConnection(FRESH_JDBC_URL, "sa", "")) {
            insertBatch(connection, "batch-1");
            insertBatch(connection, "batch-2");
        }
    }

    /**
     * 数据库 schema 应为批次保存调用方归因字段。
     *
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void schemaAddsCallerAttributionColumnsToBatches() throws SQLException {
        migrateFreshSchema();
        try (Connection connection = DriverManager.getConnection(FRESH_JDBC_URL, "sa", "")) {
            assertThat(batchColumnNames(connection))
                    .contains(CALLER_CLIENT_ID_COLUMN, CALLER_SOURCE_APP_COLUMN, CALLER_TENANT_KEY_COLUMN);
        }
    }

    /**
     * V16 迁移应移除既有库中的 idempotency_key 唯一约束。
     *
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    @Test
    void migrationRemovesExistingUniqueConstraint() throws SQLException {
        createLegacySchemaWithUniqueIdempotencyKey();
        migrateFromVersion15();
        try (Connection connection = DriverManager.getConnection(MIGRATED_JDBC_URL, "sa", "")) {
            insertBatch(connection, "batch-1");
            insertBatch(connection, "batch-2");
        }
    }

    /**
     * 执行 Flyway 迁移。
     *
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    private void migrateFreshSchema() {
        Flyway.configure()
                .dataSource(FRESH_JDBC_URL, "sa", "")
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    /**
     * 创建含旧唯一约束的历史 schema。
     *
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    private void createLegacySchemaWithUniqueIdempotencyKey() throws SQLException {
        try (Connection connection = DriverManager.getConnection(MIGRATED_JDBC_URL, "sa", "")) {
            connection.createStatement().execute("""
                    CREATE TABLE ocr_batches (
                        batch_id VARCHAR(80) PRIMARY KEY,
                        status VARCHAR(40) NOT NULL,
                        total_files INTEGER NOT NULL,
                        completed_files INTEGER NOT NULL,
                        failed_files INTEGER NOT NULL,
                        current_document_id VARCHAR(80),
                        current_document_name VARCHAR(512),
                        current_stage VARCHAR(80),
                        metadata TEXT NOT NULL,
                        callback_url VARCHAR(2048),
                        idempotency_key VARCHAR(256) UNIQUE,
                        created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                        updated_at TIMESTAMP WITH TIME ZONE NOT NULL
                    )
                    """);
        }
    }

    /**
     * 从 v15 基线执行后续迁移。
     *
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    private void migrateFromVersion15() {
        Flyway.configure()
                .dataSource(MIGRATED_JDBC_URL, "sa", "")
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("15")
                .load()
                .migrate();
    }

    /**
     * 插入测试批次。
     *
     * @param connection 数据库连接
     * @param batchId 批次 ID
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    private void insertBatch(Connection connection, String batchId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO ocr_batches (
                    batch_id, status, total_files, completed_files, failed_files,
                    current_document_id, current_document_name, current_stage,
                    metadata, callback_url, idempotency_key, created_at, updated_at
                ) VALUES (?, 'queued', 1, 0, 0, NULL, '', 'queued', '{}', NULL, ?, ?, ?)
                """)) {
            statement.setString(1, batchId);
            statement.setString(2, IDEMPOTENCY_KEY);
            statement.setObject(3, BASE_TIME);
            statement.setObject(4, BASE_TIME);
            statement.executeUpdate();
        }
    }

    /**
     * 查询批次表字段名称。
     *
     * @param connection 数据库连接
     * @return 字段名称集合
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private java.util.List<String> batchColumnNames(Connection connection) throws SQLException {
        java.util.List<String> columnNames = new java.util.ArrayList<>(16);
        try (java.sql.ResultSet columns = connection.getMetaData().getColumns(null, null, "ocr_batches", null)) {
            while (columns.next()) {
                columnNames.add(columns.getString("COLUMN_NAME").toUpperCase(java.util.Locale.ROOT));
            }
        }
        return columnNames;
    }

    /**
     * 读取迁移资源文本。
     *
     * @param migrationPath 迁移资源路径
     * @return 迁移 SQL 文本
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    private String readMigration(String migrationPath) {
        try (java.io.InputStream stream = getClass().getResourceAsStream(migrationPath)) {
            // 迁移文件缺失时直接让断言显示明确失败。
            assertThat(stream).isNotNull();
            return new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("failed to read migration " + migrationPath, exception);
        }
    }
}
