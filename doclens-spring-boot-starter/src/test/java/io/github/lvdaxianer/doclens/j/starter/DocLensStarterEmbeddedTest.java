package io.github.lvdaxianer.doclens.j.starter;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.api.CreateBatchRequest;
import io.github.lvdaxianer.doclens.j.api.DocLensEngine;
import io.github.lvdaxianer.doclens.j.api.DocumentInput;
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
        assertThat(docLensEngine.getBatch(batchId)).containsEntry("progress_percent", 100);
        assertThat(docLensEngine.getDocument(documentId)).containsEntry("document_id", documentId);
        assertThat(docLensEngine.getDocumentResult(documentId)).containsEntry("document_id", documentId);
        assertThat(docLensEngine.getEvents(batchId)).containsKey("events");
        assertThat(docLensEngine.listAdapters().get("adapters")).isNotEmpty();
    }

    private CreateBatchRequest createRequest() {
        return new CreateBatchRequest(
                List.of(new DocumentInput("embedded.pdf", "%PDF-embedded".getBytes())),
                Map.of("source", "starter-test"),
                "",
                "idem-starter-" + UUID.randomUUID(),
                "",
                "page_image_fallback"
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
