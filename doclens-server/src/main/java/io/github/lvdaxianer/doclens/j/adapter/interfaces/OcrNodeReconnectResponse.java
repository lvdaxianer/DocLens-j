package io.github.lvdaxianer.doclens.j.adapter.interfaces;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * OCR 节点手动重连响应 DTO。
 *
 * @param healthy 是否恢复健康
 * @param attempts 实际尝试次数
 * @param status 节点当前状态
 * @param circuitOpenUntil 熔断结束时间
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public record OcrNodeReconnectResponse(
        boolean healthy,
        int attempts,
        String status,
        @JsonProperty("circuit_open_until") String circuitOpenUntil
) {

    /**
     * 创建手动重连响应。
     *
     * @param healthy 是否恢复健康
     * @param attempts 实际尝试次数
     * @param status 节点当前状态
     * @param circuitOpenUntil 熔断结束时间
     * @return 手动重连响应
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public static OcrNodeReconnectResponse of(
            boolean healthy,
            int attempts,
            String status,
            Optional<OffsetDateTime> circuitOpenUntil
    ) {
        return new OcrNodeReconnectResponse(healthy, attempts, status,
                circuitOpenUntil.map(OffsetDateTime::toString).orElse(""));
    }
}
