package io.github.lvdaxianer.doclens.j;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.api.DocLensEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Application smoke tests.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@SpringBootTest
class DocLensApplicationTests {

    @Autowired
    private DocLensEngine docLensEngine;

    /**
     * Verifies Spring context can start.
     *
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Test
    void contextLoads() {
        assertThat(docLensEngine).isNotNull();
    }
}
