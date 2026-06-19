package io.github.lvdaxianer.doclens.j.starter;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.api.CreateBatchRequest;
import io.github.lvdaxianer.doclens.j.api.DocLensEngine;
import io.github.lvdaxianer.doclens.j.api.DocumentInput;
import io.github.lvdaxianer.doclens.j.processing.application.ChunkStrategy;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * 面向 SDK 使用方式的嵌入式 Spring Boot Starter 测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@SpringBootTest(classes = DocLensStarterEmbeddedTest.TestApplication.class)
class DocLensStarterEmbeddedTest {

    private static final int PROCESSING_WAIT_ATTEMPTS = 20;
    private static final int PROCESSING_WAIT_MILLIS = 100;

    @TempDir
    static java.nio.file.Path tempDir;

    @Autowired
    private DocLensEngine docLensEngine;

    /**
     * 配置隔离的 Starter 测试运行环境。
     *
     * @param registry 动态属性注册表
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:file:" + tempDir.resolve("starter-test") + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        registry.add("doclens.storage-root", () -> tempDir.resolve("storage").toString());
        registry.add("doclens.auto-process-on-upload", () -> "true");
        registry.add("doclens.worker-id", () -> "starter-test-worker");
        registry.add("doclens.callback.max-retries", () -> "3");
        registry.add("doclens.callback.timeout-seconds", () -> "10");
        registry.add("doclens.adapter.default-key", () -> "stub_ocr");
        registry.add("doclens.paddle-ocr.enabled", () -> "false");
    }

    /**
     * 验证宿主 Spring Boot 应用可以注入并调用 DocLensEngine。
     *
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Test
    void hostApplicationCanInjectAndUseDocLensEngine() {
        Map<String, Object> created = docLensEngine.createBatch(createRequest());
        String batchId = String.valueOf(created.get("batch_id"));
        List<?> documents = (List<?>) created.get("documents");
        Map<?, ?> firstDocument = (Map<?, ?>) documents.getFirst();
        String documentId = String.valueOf(firstDocument.get("document_id"));

        assertThat(batchId).startsWith("batch_");
        assertThat(created).containsEntry("status", "queued");
        Map<String, Object> completedBatch = waitForCompletedBatch(batchId);
        assertThat(completedBatch).containsEntry("progress_percent", 100);
        assertThat(docLensEngine.getDocument(documentId)).containsEntry("document_id", documentId);
        Map<String, Object> result = docLensEngine.getDocumentResult(documentId);
        assertThat(result).containsEntry("document_id", documentId);
        assertThat(String.valueOf(((Map<?, ?>) result.get("result")).get("finalText"))).isEqualTo("# Embedded\ncontent");
        assertThat(docLensEngine.getEvents(batchId)).containsKey("events");
        assertThat(docLensEngine.listAdapters().get("adapters")).isNotEmpty();
    }

    /**
     * 等待后台处理完成。
     *
     * @param batchId 批次 ID
     * @return 批次状态
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private Map<String, Object> waitForCompletedBatch(String batchId) {
        Map<String, Object> batch = docLensEngine.getBatch(batchId);
        for (int attempt = 0; attempt < PROCESSING_WAIT_ATTEMPTS; attempt++) {
            if (Integer.valueOf(100).equals(batch.get("progress_percent"))) {
                return batch;
            } else {
                sleepBeforeNextAttempt();
                batch = docLensEngine.getBatch(batchId);
            }
        }
        return batch;
    }

    /**
     * 等待下一次状态查询。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private void sleepBeforeNextAttempt() {
        try {
            Thread.sleep(PROCESSING_WAIT_MILLIS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private CreateBatchRequest createRequest() {
        return new CreateBatchRequest(
                List.of(new DocumentInput("embedded.md", "# Embedded\ncontent".getBytes())),
                Map.of("source", "starter-test"),
                "",
                "idem-starter-" + UUID.randomUUID(),
                "",
                "page_image_fallback",
                ChunkStrategy.GENERAL.name()
        );
    }

    /**
     * 用于激活 Spring Boot 自动配置的最小宿主应用。
     *
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @SpringBootApplication
    static class TestApplication {
    }
}
