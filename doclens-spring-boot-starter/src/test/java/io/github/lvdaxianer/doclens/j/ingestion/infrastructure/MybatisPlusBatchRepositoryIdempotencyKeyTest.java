package io.github.lvdaxianer.doclens.j.ingestion.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.ingestion.domain.Batch;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchStatus;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.JsonCodec;
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

    @Autowired
    private BatchRepository repository;

    /**
     * 配置 H2 与 Flyway 测试数据库。
     *
     * @param registry 动态属性注册表
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:batch_idempotency_lookup;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
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
