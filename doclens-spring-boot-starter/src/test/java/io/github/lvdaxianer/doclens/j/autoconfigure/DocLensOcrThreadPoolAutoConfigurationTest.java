package io.github.lvdaxianer.doclens.j.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * OCR 线程池隔离自动配置测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
@SpringBootTest(classes = DocLensOcrThreadPoolAutoConfigurationTest.TestApplication.class)
class DocLensOcrThreadPoolAutoConfigurationTest {

    @Autowired
    private Map<String, ExecutorService> executors;

    /**
     * 自动配置应创建四类隔离线程池。
     *
     * @throws Exception 线程任务执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void createsIsolatedExecutorsWithBusinessThreadNames() throws Exception {
        assertThreadName("doclensDocumentProcessingExecutor", "doclens-document-processing-");
        assertThreadName("doclensOcrRequestExecutor", "doclens-ocr-request-");
        assertThreadName("doclensOcrHealthExecutor", "doclens-ocr-health-");
        assertThreadName("doclensCallbackExecutor", "doclens-callback-");
    }

    /**
     * 断言指定线程池的线程名前缀。
     *
     * @param beanName 线程池 Bean 名称
     * @param prefix 线程名前缀
     * @throws Exception 线程任务执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private void assertThreadName(String beanName, String prefix) throws Exception {
        ExecutorService executor = executors.get(beanName);
        Future<String> threadName = executor.submit(() -> Thread.currentThread().getName());

        assertThat(executor).isNotNull();
        assertThat(threadName.get()).startsWith(prefix);
    }

    /**
     * OCR 线程池测试应用。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @SpringBootConfiguration
    @EnableConfigurationProperties(DocLensSpringProperties.class)
    @Import(DocLensOcrThreadPoolAutoConfiguration.class)
    static class TestApplication {
    }
}
