package io.github.lvdaxianer.doclens.j.ingestion.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.ingestion.domain.Batch;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchStatus;
import io.github.lvdaxianer.doclens.j.ingestion.domain.CallerIdentity;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.JsonCodec;
import io.github.lvdaxianer.doclens.j.testsupport.PostgreSqlTestContainerSupport;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * 批次仓储幂等键回查测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-16
 */
@SpringBootTest(classes = MybatisPlusBatchRepositoryIdempotencyKeyTest.TestApplication.class)
class MybatisPlusBatchRepositoryIdempotencyKeyTest {

    private static final OffsetDateTime OLDER_TIME = OffsetDateTime.parse("2026-06-16T10:00:00+08:00");
    private static final OffsetDateTime NEWER_TIME = OffsetDateTime.parse("2026-06-16T10:05:00+08:00");
    private static final String IDEMPOTENCY_KEY = "idem-pass-through";
    private static final String TEST_CLIENT_ID = "rag-flow";
    private static final String TEST_SOURCE_APP = "knowledge-base";
    private static final String TEST_TENANT_KEY = "tenant-east";
    private static final PostgreSQLContainer<?> POSTGRESQL =
            PostgreSqlTestContainerSupport.createStartedContainer("batch_idempotency_lookup");

    @Autowired
    private BatchRepository repository;

    /**
     * 配置 PostgreSQL 与 Flyway 测试数据库。
     *
     * @param registry 动态属性注册表
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry registry) {
        PostgreSqlTestContainerSupport.registerDatasource(registry, POSTGRESQL);
    }

    /**
     * 重复幂等键回查应返回更新时间最新的匹配批次。
     *
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    @Test
    void findByIdempotencyKeyReturnsLatestMatchingBatch() {
        repository.save(batch("batch-older", OLDER_TIME));
        repository.save(batch("batch-newer", NEWER_TIME));

        assertThat(repository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .get()
                .extracting(Batch::batchId)
                .isEqualTo("batch-newer");
    }

    /**
     * 批次仓储应持久化并水合调用方归因。
     *
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void saveAndFindByIdHydratesCallerIdentity() {
        repository.save(attributedBatch("batch-caller", NEWER_TIME));

        Batch found = repository.findById("batch-caller").orElseThrow();

        assertThat(found.callerIdentity().clientId()).isEqualTo(TEST_CLIENT_ID);
        assertThat(found.callerIdentity().sourceApp()).isEqualTo(TEST_SOURCE_APP);
        assertThat(found.callerIdentity().tenantKey()).contains(TEST_TENANT_KEY);
    }

    /**
     * 创建测试批次。
     *
     * @param batchId 批次 ID
     * @param now 时间
     * @return 测试批次
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    private Batch batch(String batchId, OffsetDateTime now) {
        return new Batch(batchId, BatchStatus.QUEUED, 1, 0, 0, Optional.empty(), Optional.empty(), "queued",
                JsonPayload.empty(), Optional.empty(), Optional.of(IDEMPOTENCY_KEY), now, now);
    }

    /**
     * 创建带调用方归因的测试批次。
     *
     * @param batchId 批次 ID
     * @param now 时间
     * @return 测试批次
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private Batch attributedBatch(String batchId, OffsetDateTime now) {
        CallerIdentity caller = new CallerIdentity(TEST_CLIENT_ID, TEST_SOURCE_APP, Optional.of(TEST_TENANT_KEY));
        return new Batch(batchId, BatchStatus.QUEUED, 1, 0, 0, Optional.empty(), Optional.empty(), "queued",
                JsonPayload.empty(), Optional.empty(), Optional.of(IDEMPOTENCY_KEY), now, now, caller);
    }

    /**
     * 批次仓储测试应用。
     *
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration(excludeName = {
            "io.github.lvdaxianer.doclens.j.autoconfigure.DocLensAutoConfiguration",
            "io.github.lvdaxianer.doclens.j.autoconfigure.DocLensPaddleOcrAutoConfiguration",
            "io.github.lvdaxianer.doclens.j.autoconfigure.DocLensExtractionAutoConfiguration",
            "io.github.lvdaxianer.doclens.j.autoconfigure.DocLensProcessingAutoConfiguration",
            "io.github.lvdaxianer.doclens.j.autoconfigure.DocLensCallbackDeliveryAutoConfiguration"
    })
    @MapperScan(basePackageClasses = BatchMapper.class, annotationClass = Mapper.class)
    @Import({
            io.github.lvdaxianer.doclens.j.shared.config.MybatisPlusConfiguration.class,
            MybatisPlusBatchRepository.class
    })
    static class TestApplication {

        /**
         * 创建 JSON 编解码器。
         *
         * @return JSON 编解码器
         * @author lvdaxianerplus
         * @date 2026-06-16
         */
        @Bean
        JsonCodec jsonCodec() {
            return new JsonCodec(new ObjectMapper());
        }
    }
}
