package io.github.lvdaxianer.doclens.j.health.infrastructure;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.infrastructure.OcrHealthClient;
import io.github.lvdaxianer.doclens.j.health.application.ModelHeartbeatProbe;
import io.github.lvdaxianer.doclens.j.health.application.ModelHeartbeatProbeResult;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthFailureType;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OCR 节点模型心跳探针。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public class OcrNodeHeartbeatProbe implements ModelHeartbeatProbe {

    private static final Logger LOGGER = LoggerFactory.getLogger(OcrNodeHeartbeatProbe.class);
    private static final String BAD_RESPONSE_MESSAGE = "ocr node health check returned unhealthy";
    private static final String TIMEOUT_MESSAGE = "ocr node heartbeat timeout";
    private static final String CONNECTION_REFUSED_MESSAGE = "ocr node heartbeat connection refused";
    private static final String UNKNOWN_MESSAGE = "ocr node heartbeat failed";

    private final OcrNode node;
    private final OcrHealthClient healthClient;

    /**
     * 创建 OCR 节点模型心跳探针。
     *
     * @param node OCR 节点
     * @param healthClient OCR 健康检查客户端
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public OcrNodeHeartbeatProbe(OcrNode node, OcrHealthClient healthClient) {
        this.node = Objects.requireNonNull(node, "ocr heartbeat node is required");
        this.healthClient = Objects.requireNonNull(healthClient, "ocr health client is required");
    }

    /**
     * 执行一次 OCR 节点心跳探测。
     *
     * @return 心跳探测结果
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public ModelHeartbeatProbeResult probe() {
        try {
            return resultOf(healthClient.isHealthy(node));
        } catch (RuntimeException exception) {
            LOGGER.warn("[模型心跳] OCR节点心跳探测异常, nodeId={}, modelKey={}, exceptionType={}",
                    node.id(), node.modelKey(), exception.getClass().getName());
            return failureOf(exception);
        }
    }

    /**
     * 将布尔健康结果转换为心跳结果。
     *
     * @param healthy 是否健康
     * @return 心跳探测结果
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ModelHeartbeatProbeResult resultOf(boolean healthy) {
        // OCR 健康客户端确认节点健康时返回成功心跳结果。
        if (healthy) {
            return ModelHeartbeatProbeResult.success();
        } else {
            // OCR 健康客户端明确返回不健康时归一化为坏响应。
            return ModelHeartbeatProbeResult.failure(ModelHealthFailureType.BAD_RESPONSE, BAD_RESPONSE_MESSAGE);
        }
    }

    /**
     * 将异常链转换为心跳失败结果。
     *
     * @param exception 运行时异常
     * @return 心跳失败结果
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ModelHeartbeatProbeResult failureOf(RuntimeException exception) {
        // 异常链包含超时异常时归一化为 TIMEOUT。
        if (hasCause(exception, HttpTimeoutException.class) || hasCause(exception, SocketTimeoutException.class)) {
            return ModelHeartbeatProbeResult.failure(ModelHealthFailureType.TIMEOUT, TIMEOUT_MESSAGE);
        } else if (hasCause(exception, ConnectException.class)) {
            // 异常链包含连接拒绝时归一化为 CONNECTION_REFUSED。
            return ModelHeartbeatProbeResult.failure(ModelHealthFailureType.CONNECTION_REFUSED,
                    CONNECTION_REFUSED_MESSAGE);
        } else {
            // 其他未识别异常统一归一化为 UNKNOWN，避免暴露底层细节。
            return ModelHeartbeatProbeResult.failure(ModelHealthFailureType.UNKNOWN, UNKNOWN_MESSAGE);
        }
    }

    /**
     * 判断异常链中是否包含指定异常类型。
     *
     * @param throwable 异常
     * @param causeType 异常类型
     * @return 是否包含
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private boolean hasCause(Throwable throwable, Class<? extends Throwable> causeType) {
        Throwable current = throwable;
        while (current != null) {
            // 找到指定异常类型即可完成归类。
            if (causeType.isInstance(current)) {
                return true;
            } else {
                current = current.getCause();
            }
        }
        return false;
    }
}
