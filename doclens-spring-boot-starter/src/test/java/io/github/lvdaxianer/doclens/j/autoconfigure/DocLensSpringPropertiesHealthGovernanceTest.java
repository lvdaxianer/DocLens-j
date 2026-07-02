package io.github.lvdaxianer.doclens.j.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 校验 DocLens Spring 配置默认值。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
class DocLensSpringPropertiesHealthGovernanceTest {

    /**
     * 校验默认配置暴露加权调度与健康治理参数。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void defaultsExposeWeightedSchedulingAndHealthGovernance() {
        DocLensSpringProperties properties = new DocLensSpringProperties(
                "./var/storage",
                true,
                "local-worker",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        assertThat(properties.ocr().loadBalanceStrategy()).isEqualTo("weighted-idle");
        assertThat(properties.ocr().idleFactor()).isEqualTo(0.7D);
        assertThat(properties.ocr().weightFactor()).isEqualTo(0.3D);
        assertThat(properties.ocr().topBucketThreshold()).isEqualTo(0.15D);
        assertThat(properties.ocr().failureThreshold()).isEqualTo(3);
        assertThat(properties.ocr().probeIntervalSeconds()).isEqualTo(5);
        assertThat(properties.ocr().circuitOpenSeconds()).isEqualTo(86400);
        assertThat(properties.ocr().recoverySuccessThreshold()).isEqualTo(3);
        assertThat(properties.ocr().manualRecoveryAttempts()).isEqualTo(3);
        assertThat(properties.extraction().ocrConcurrency()).isEqualTo(4);
        assertThat(DocLensSpringProperties.defaultPaddleNode().weight()).isEqualTo(50);
        assertThat(DocLensSpringProperties.defaultPaddleNode().maxConcurrency()).isEqualTo(10);
    }
}
