package io.github.lvdaxianer.doclens.j;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.api.DocLensEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

/**
 * 应用冒烟测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@SpringBootTest
class DocLensApplicationTests {

    private static final String MULTIPART_MAX_FILE_SIZE_PROPERTY = "spring.servlet.multipart.max-file-size";
    private static final String MULTIPART_MAX_REQUEST_SIZE_PROPERTY = "spring.servlet.multipart.max-request-size";
    private static final String MULTIPART_UPLOAD_LIMIT = "500MB";

    @Autowired
    private DocLensEngine docLensEngine;

    @Autowired
    private Environment environment;

    /**
     * 验证 Spring 上下文可以启动。
     *
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Test
    void contextLoads() {
        assertThat(docLensEngine).isNotNull();
    }

    /**
     * 验证 multipart 上传限制允许 500MB 文件。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void multipartUploadLimitShouldAllowFiveHundredMegabytes() {
        assertThat(environment.getProperty(MULTIPART_MAX_FILE_SIZE_PROPERTY))
                .isEqualTo(MULTIPART_UPLOAD_LIMIT);
        assertThat(environment.getProperty(MULTIPART_MAX_REQUEST_SIZE_PROPERTY))
                .isEqualTo(MULTIPART_UPLOAD_LIMIT);
    }
}
