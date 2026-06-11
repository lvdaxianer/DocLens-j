package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * 进程内批次命中跟踪器规范测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
class InMemoryOcrBatchHitTrackerSpecTest {

    private static final Path TRACKER_SOURCE = Path.of(
            "../doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure"
                    + "/InMemoryOcrBatchHitTracker.java");

    /**
     * 命中跟踪器生产代码不应通过 return null 触发映射删除。
     *
     * @throws IOException 读取源码失败
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void inMemoryBatchHitTrackerSourceDoesNotReturnNull() throws IOException {
        String source = Files.readString(TRACKER_SOURCE);

        assertThat(source).doesNotContain("return null;");
    }
}
