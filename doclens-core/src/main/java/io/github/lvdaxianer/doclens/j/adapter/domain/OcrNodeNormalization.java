package io.github.lvdaxianer.doclens.j.adapter.domain;

import java.util.Optional;

/**
 * OCR 节点字段标准化规则。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
final class OcrNodeNormalization {

    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65535;
    private static final String ALIYUN_BAILIAN_DASHSCOPE = "aliyun_bailian_dashscope";

    /**
     * 禁止实例化 OCR 节点标准化工具。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private OcrNodeNormalization() {
    }

    /**
     * 校验必填文本。
     *
     * @param value 文本值
     * @param message 校验失败消息
     * @return 标准化后的文本
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    static String requiredText(String value, String message) {
        return normalize(Optional.ofNullable(value)).orElseThrow(() -> new IllegalArgumentException(message));
    }

    /**
     * 按部署类型标准化主机。
     *
     * @param deploymentType 节点部署类型
     * @param host 主机文本
     * @return 标准化后的主机文本
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    static String normalizeHost(OcrNodeDeploymentType deploymentType, String host) {
        if (deploymentType == OcrNodeDeploymentType.OFFLINE) {
            // 离线节点需要真实主机参与健康检查和请求路由。
            return validHost(host);
        } else {
            // 在线节点通过渠道与模型路由，不使用 host 字段。
            return "";
        }
    }

    /**
     * 按部署类型标准化端口。
     *
     * @param deploymentType 节点部署类型
     * @param port 节点端口
     * @return 标准化后的端口
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    static int normalizePort(OcrNodeDeploymentType deploymentType, int port) {
        if (deploymentType == OcrNodeDeploymentType.OFFLINE) {
            // 离线节点必须提供可访问端口。
            validatePort(port);
            return port;
        } else {
            // 在线节点不使用本地端口，统一归零避免误展示。
            return 0;
        }
    }

    /**
     * 按部署类型标准化在线渠道。
     *
     * @param deploymentType 节点部署类型
     * @param channelKey 在线渠道标识
     * @return 标准化后的在线渠道
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    static Optional<String> normalizeChannel(OcrNodeDeploymentType deploymentType, Optional<String> channelKey) {
        if (deploymentType == OcrNodeDeploymentType.ONLINE) {
            // 在线节点必须指定已支持渠道，才能找到对应客户端。
            String normalizedChannel = requiredText(optionalText(channelKey), "ocr online channel key is required");
            validateSupportedChannel(normalizedChannel);
            return Optional.of(normalizedChannel);
        } else {
            // 离线节点不允许残留在线渠道，避免混合路由。
            return Optional.empty();
        }
    }

    /**
     * 按部署类型标准化在线模型名称。
     *
     * @param deploymentType 节点部署类型
     * @param providerModel 在线模型名称
     * @return 标准化后的在线模型名称
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    static Optional<String> normalizeProviderModel(
            OcrNodeDeploymentType deploymentType,
            Optional<String> providerModel
    ) {
        if (deploymentType == OcrNodeDeploymentType.ONLINE) {
            // 在线节点必须保留供应商模型名称供请求构造使用。
            return Optional.of(requiredText(optionalText(providerModel), "ocr online provider model is required"));
        } else {
            // 离线节点不需要供应商模型，统一清空。
            return Optional.empty();
        }
    }

    /**
     * 校验正整数。
     *
     * @param value 待校验值
     * @param message 校验失败消息
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    static void validatePositive(int value, String message) {
        if (value > 0) {
            // 正整数满足节点调度权重和并发语义。
            return;
        } else {
            // 非正数会导致调度权重或并发槽位失真，直接拒绝。
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 标准化可选文本。
     *
     * @param value 可选文本
     * @return 标准化后的可选文本
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    static Optional<String> normalize(Optional<String> value) {
        return value == null ? Optional.empty() : value.map(String::trim).filter(text -> !text.isBlank());
    }

    /**
     * 标准化可选对象。
     *
     * @param value 可选对象
     * @param <T> 对象类型
     * @return 非空可选对象
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    static <T> Optional<T> normalizeOptional(Optional<T> value) {
        return value == null ? Optional.empty() : value;
    }

    /**
     * 校验主机只包含 host，不包含 URL 结构。
     *
     * @param host 主机文本
     * @return 标准化后的主机文本
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private static String validHost(String host) {
        String normalizedHost = requiredText(host, "ocr node host is required");
        if (containsUrlPart(normalizedHost)) {
            // host 字段只接受主机名或 IP，避免保存完整 URL 后拼接错误。
            throw new IllegalArgumentException("ocr node host must not include scheme, path, query or fragment");
        } else {
            // 标准主机文本可直接参与 URL 组装。
            return normalizedHost;
        }
    }

    /**
     * 校验在线渠道是否已支持。
     *
     * @param channelKey 在线渠道标识
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private static void validateSupportedChannel(String channelKey) {
        if (ALIYUN_BAILIAN_DASHSCOPE.equals(channelKey)) {
            // 当前仅内置百炼 DashScope 在线 OCR 渠道。
            return;
        } else {
            // 未支持渠道缺少客户端实现，必须在入库前拒绝。
            throw new IllegalArgumentException("unsupported ocr online channel key");
        }
    }

    /**
     * 判断主机是否包含 URL 结构。
     *
     * @param host 主机文本
     * @return 是否包含 URL 结构
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private static boolean containsUrlPart(String host) {
        return host.contains("://") || host.contains("/") || host.contains("?") || host.contains("#");
    }

    /**
     * 校验端口范围。
     *
     * @param port 节点端口
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private static void validatePort(int port) {
        if (port >= MIN_PORT && port <= MAX_PORT) {
            // 合法 TCP 端口可以参与离线服务请求。
            return;
        } else {
            // 越界端口无法建立有效连接，直接拒绝。
            throw new IllegalArgumentException("ocr node port must be between 1 and 65535");
        }
    }

    /**
     * 读取可空 Optional 中的文本。
     *
     * @param value 可选文本
     * @return 文本内容
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private static String optionalText(Optional<String> value) {
        return value == null ? "" : value.orElse("");
    }
}
