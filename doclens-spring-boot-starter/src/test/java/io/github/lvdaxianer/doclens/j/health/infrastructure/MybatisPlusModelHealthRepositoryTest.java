package io.github.lvdaxianer.doclens.j.health.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthCounters;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthFailure;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthRepository;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthSnapshot;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthStatus;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthTargetId;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthTargetType;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthTimeline;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.JsonCodec;
import java.time.OffsetDateTime;
import java.util.List;
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
 * 模型健康 MyBatis-Plus 仓储集成测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
@SpringBootTest(classes = MybatisPlusModelHealthRepositoryTest.TestApplication.class)
class MybatisPlusModelHealthRepositoryTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-10T10:00:00+08:00");
    private static final int LAST_ERROR_MAX_LENGTH = 1024;
    private static final int TOO_LONG_ERROR_LENGTH = 1200;
    private static final long DOWN_FAILURE_COUNT = 3L;
    private static final long ONE_SUCCESS_COUNT = 1L;
    private static final long ONE_FAILURE_COUNT = 1L;
    private static final String TIMEOUT_ERROR = "timeout";

    @Autowired
    private ModelHealthRepository repository;

    /**
     * 配置 H2 与 Flyway 测试数据库。
     *
     * @param registry 动态属性注册表
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:model_health_repo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
    }

    /**
     * 批量写入后应能读取模型健康快照。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void upsertAllPersistsAndReloadsHealthSnapshots() {
        ModelHealthTargetId target = target();
        ModelHealthSnapshot snapshot = downSnapshot(target);

        repository.upsertAll(List.of(snapshot));

        assertThat(snapshotsOf(target)).singleElement()
                .satisfies(saved -> {
                    assertThat(saved.targetId()).isEqualTo(target);
                    assertThat(saved.status()).isEqualTo(ModelHealthStatus.DOWN);
                    assertThat(saved.consecutiveFailures()).isEqualTo(DOWN_FAILURE_COUNT);
                    assertThat(saved.lastError()).contains(TIMEOUT_ERROR);
                });
    }

    /**
     * 重复写入同一目标应更新既有快照而不是新增重复记录。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void upsertAllUpdatesExistingHealthSnapshot() {
        ModelHealthTargetId target = target();

        repository.upsertAll(List.of(downSnapshot(target)));
        repository.upsertAll(List.of(upSnapshot(target)));

        assertThat(snapshotsOf(target)).singleElement()
                .satisfies(saved -> {
                    assertThat(saved.status()).isEqualTo(ModelHealthStatus.UP);
                    assertThat(saved.consecutiveFailures()).isZero();
                    assertThat(saved.consecutiveSuccesses()).isEqualTo(ONE_SUCCESS_COUNT);
                });
    }

    /**
     * 过长错误摘要应在持久化前截断。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void upsertAllTruncatesLongErrorMessage() {
        ModelHealthTargetId target = new ModelHealthTargetId(ModelHealthTargetType.OCR_NODE, "paddle_ocr", "paddle-2");
        String longError = "x".repeat(TOO_LONG_ERROR_LENGTH);

        repository.upsertAll(List.of(errorSnapshot(target, longError)));

        assertThat(snapshotsOf(target)).singleElement()
                .extracting(snapshot -> snapshot.lastError().orElseThrow())
                .asString()
                .hasSize(LAST_ERROR_MAX_LENGTH);
    }

    /**
     * 含分隔符的目标标识不应发生持久化主键碰撞。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void upsertAllKeepsTargetsWithSeparatorDistinct() {
        ModelHealthTargetId first = new ModelHealthTargetId(ModelHealthTargetType.OCR_NODE, "a|b", "c");
        ModelHealthTargetId second = new ModelHealthTargetId(ModelHealthTargetType.OCR_NODE, "a", "b|c");

        repository.upsertAll(List.of(downSnapshot(first), upSnapshot(second)));

        assertThat(snapshotsOf(first)).singleElement()
                .extracting(ModelHealthSnapshot::status)
                .isEqualTo(ModelHealthStatus.DOWN);
        assertThat(snapshotsOf(second)).singleElement()
                .extracting(ModelHealthSnapshot::status)
                .isEqualTo(ModelHealthStatus.UP);
    }

    /**
     * 查询指定目标的健康快照。
     *
     * @param target 模型健康目标
     * @return 健康快照集合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private List<ModelHealthSnapshot> snapshotsOf(ModelHealthTargetId target) {
        return repository.listAll().stream()
                .filter(snapshot -> snapshot.targetId().equals(target))
                .toList();
    }

    /**
     * 创建测试目标。
     *
     * @return 模型健康目标
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ModelHealthTargetId target() {
        return new ModelHealthTargetId(ModelHealthTargetType.OCR_NODE, "paddle_ocr", "paddle-1");
    }

    /**
     * 创建不可用快照。
     *
     * @param target 模型健康目标
     * @return 不可用健康快照
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ModelHealthSnapshot downSnapshot(ModelHealthTargetId target) {
        return new ModelHealthSnapshot(target, ModelHealthStatus.DOWN, new ModelHealthCounters(DOWN_FAILURE_COUNT, 0L),
                new ModelHealthTimeline(Optional.of(BASE_TIME), Optional.empty(), Optional.of(BASE_TIME), BASE_TIME),
                ModelHealthFailure.of(null, TIMEOUT_ERROR));
    }

    /**
     * 创建可用快照。
     *
     * @param target 模型健康目标
     * @return 可用健康快照
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ModelHealthSnapshot upSnapshot(ModelHealthTargetId target) {
        return new ModelHealthSnapshot(target, ModelHealthStatus.UP, new ModelHealthCounters(0L, ONE_SUCCESS_COUNT),
                new ModelHealthTimeline(Optional.of(BASE_TIME.plusSeconds(1)), Optional.of(BASE_TIME.plusSeconds(1)),
                        Optional.empty(), BASE_TIME.plusSeconds(1)),
                ModelHealthFailure.empty());
    }

    /**
     * 创建带错误摘要的快照。
     *
     * @param target 模型健康目标
     * @param error 错误摘要
     * @return 健康快照
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ModelHealthSnapshot errorSnapshot(ModelHealthTargetId target, String error) {
        return new ModelHealthSnapshot(target, ModelHealthStatus.DOWN, new ModelHealthCounters(ONE_FAILURE_COUNT, 0L),
                new ModelHealthTimeline(Optional.of(BASE_TIME), Optional.empty(), Optional.of(BASE_TIME), BASE_TIME),
                ModelHealthFailure.of(null, error));
    }

    /**
     * 模型健康仓储测试应用。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration(excludeName = {
            "io.github.lvdaxianer.doclens.j.autoconfigure.DocLensAutoConfiguration",
            "io.github.lvdaxianer.doclens.j.autoconfigure.DocLensPaddleOcrAutoConfiguration",
            "io.github.lvdaxianer.doclens.j.autoconfigure.DocLensExtractionAutoConfiguration",
            "io.github.lvdaxianer.doclens.j.autoconfigure.DocLensProcessingAutoConfiguration"
    })
    @MapperScan(basePackageClasses = ModelHealthMapper.class, annotationClass = Mapper.class)
    @Import({
            io.github.lvdaxianer.doclens.j.shared.config.MybatisPlusConfiguration.class,
            MybatisPlusModelHealthRepository.class
    })
    static class TestApplication {

        /**
         * 为测试上下文补齐仓储依赖的 JSON 编解码器。
         *
         * @return JSON 编解码器
         * @author lvdaxianerplus
         * @date 2026-06-17
         */
        @Bean
        JsonCodec jsonCodec() {
            return new JsonCodec(new ObjectMapper());
        }
    }
}
