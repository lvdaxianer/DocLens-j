package io.github.lvdaxianer.doclens.j;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.api.DocLensEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 应用冒烟测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@SpringBootTest
class DocLensApplicationTests {

    @Autowired
    private DocLensEngine docLensEngine;

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
}
