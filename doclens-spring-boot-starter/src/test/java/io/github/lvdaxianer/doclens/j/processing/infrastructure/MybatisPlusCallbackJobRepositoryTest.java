package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackFailureReason;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJob;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobFailureRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobStatus;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.JsonCodec;
import java.time.OffsetDateTime;
import java.util.Map;
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
 * 回调任务 MyBatis-Plus 仓储集成测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-15
 */
@SpringBootTest(classes = MybatisPlusCallbackJobRepositoryTest.TestApplication.class)
class MybatisPlusCallbackJobRepositoryTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-15T10:00:00+08:00");
    private static final int QUERY_LIMIT = 10;

    @Autowired
    private CallbackJobRepository repository;

    /**
     * 配置 H2 与 Flyway 测试数据库。
     *
     * @param registry 动态属性注册表
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:callback_job_repo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
    }

    /**
     * 仓储应保存回调载荷并持久化失败原因。
     *
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    @Test
    void repositoryStoresPayloadAndFailureReason() {
        repository.save(CallbackJob.create(createRequest("callback-1")));

        assertThat(repository.findById("callback-1")).get().satisfies(this::assertPendingPayload);

        repository.save(CallbackJob.create(createRequest("callback-2")));
        repository.markSucceeded("callback-2");
        assertThat(repository.findById("callback-2")).get().satisfies(this::assertSucceeded);

        repository.markFailed(failureRequest());

        assertThat(repository.findById("callback-1")).get().satisfies(saved -> {
            assertThat(saved.payload()).containsEntry("text", "ocr text");
            assertThat(saved.status()).isEqualTo(CallbackJobStatus.RETRYING);
            assertThat(saved.retryCount()).isEqualTo(1);
            assertThat(saved.failureReason()).contains(CallbackFailureReason.HTTP_STATUS);
            assertThat(saved.failureDetail()).contains("HTTP 503");
        });
    }

    /**
     * 仓储应返回待投递任务和已到期重试任务。
     *
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    @Test
    void repositoryListsPendingAndDueRetryJobs() {
        repository.save(CallbackJob.create(createRequest("callback-3")));
        repository.save(CallbackJob.create(createRequest("callback-4")));
        repository.markFailed(dueRetryRequest("callback-4"));

        assertThat(repository.listPending(QUERY_LIMIT))
                .extracting(CallbackJob::callbackJobId)
                .contains("callback-3", "callback-4");
    }

    /**
     * 断言新建回调任务处于待投递状态。
     *
     * @param saved 已保存任务
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private void assertPendingPayload(CallbackJob saved) {
        assertThat(saved.status()).isEqualTo(CallbackJobStatus.PENDING);
        assertThat(saved.payload()).containsEntry("text", "ocr text");
    }

    /**
     * 断言回调任务处于成功状态。
     *
     * @param saved 已保存任务
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private void assertSucceeded(CallbackJob saved) {
        assertThat(saved.status()).isEqualTo(CallbackJobStatus.SUCCESS);
        assertThat(saved.failureReason()).isEmpty();
    }

    /**
     * 创建回调任务测试请求。
     *
     * @param callbackJobId 回调任务 ID
     * @return 回调任务创建请求
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private CallbackJobCreateRequest createRequest(String callbackJobId) {
        return new CallbackJobCreateRequest(callbackJobId, "event-" + callbackJobId, "batch-1", "doc-1",
                "https://callback.example.test/done", Map.of("text", "ocr text"), BASE_TIME);
    }

    /**
     * 创建失败状态测试请求。
     *
     * @return 回调任务失败请求
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private CallbackJobFailureRequest failureRequest() {
        return new CallbackJobFailureRequest("callback-1", CallbackFailureReason.HTTP_STATUS, "HTTP 503",
                1, BASE_TIME.plusSeconds(30), BASE_TIME.plusSeconds(1));
    }

    /**
     * 创建到期重试状态测试请求。
     *
     * @param callbackJobId 回调任务 ID
     * @return 回调任务失败请求
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private CallbackJobFailureRequest dueRetryRequest(String callbackJobId) {
        return new CallbackJobFailureRequest(callbackJobId, CallbackFailureReason.HTTP_STATUS, "HTTP 503",
                1, BASE_TIME.minusSeconds(1), BASE_TIME.plusSeconds(1));
    }

    /**
     * 回调任务仓储测试应用。
     *
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration(excludeName = {
            "io.github.lvdaxianer.doclens.j.autoconfigure.DocLensAutoConfiguration",
            "io.github.lvdaxianer.doclens.j.autoconfigure.DocLensPaddleOcrAutoConfiguration",
            "io.github.lvdaxianer.doclens.j.autoconfigure.DocLensExtractionAutoConfiguration",
            "io.github.lvdaxianer.doclens.j.autoconfigure.DocLensProcessingAutoConfiguration"
    })
    @MapperScan(basePackageClasses = CallbackJobMapper.class, annotationClass = Mapper.class)
    @Import({
            io.github.lvdaxianer.doclens.j.shared.config.MybatisPlusConfiguration.class,
            MybatisPlusCallbackJobRepository.class
    })
    static class TestApplication {

        /**
         * 测试应用只装配回调任务 Mapper 和仓储，避免启动完整 OCR 处理链路。
         */
        @Bean
        JsonCodec jsonCodec() {
            return new JsonCodec(new ObjectMapper());
        }
    }
}
