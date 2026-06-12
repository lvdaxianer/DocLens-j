package io.github.lvdaxianer.doclens.j.adapter.domain;

import java.util.List;
import java.util.Objects;

/**
 * 系统已支持 OCR 模型定义。
 *
 * @param identity 模型基础身份信息
 * @param capability 模型能力配置
 * @param runtime 模型运行时默认配置
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
public record OcrModelDefinition(
        Identity identity,
        Capability capability,
        RuntimeDefaults runtime
) {

    /**
     * 创建 OCR 模型定义。
     *
     * @param command OCR 模型定义创建命令
     * @return OCR 模型定义
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public static OcrModelDefinition create(CreateCommand command) {
        return new OcrModelDefinition(
                normalizeIdentity(command.identity()),
                normalizeCapability(command.capability()),
                normalizeRuntime(command.runtime()));
    }

    /**
     * 读取 OCR 模型标识。
     *
     * @return OCR 模型标识
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public String modelKey() {
        return identity.modelKey();
    }

    /**
     * 读取 OCR 模型名称。
     *
     * @return OCR 模型名称
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public String name() {
        return identity.name();
    }

    /**
     * 读取 OCR 模型描述。
     *
     * @return OCR 模型描述
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public String description() {
        return identity.description();
    }

    /**
     * 读取支持的输入类型。
     *
     * @return 支持的输入类型
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public List<String> supportedInputs() {
        return capability.supportedInputs();
    }

    /**
     * 读取 OCR 请求固定路径。
     *
     * @return OCR 请求固定路径
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public String ocrPath() {
        return capability.ocrPath();
    }

    /**
     * 读取健康检查固定路径。
     *
     * @return 健康检查固定路径
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public String healthPath() {
        return capability.healthPath();
    }

    /**
     * 读取默认服务端口。
     *
     * @return 默认服务端口
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public int defaultPort() {
        return runtime.defaultPort();
    }

    /**
     * 读取默认厂商模型。
     *
     * @return 默认厂商模型
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public String providerModel() {
        return runtime.providerModel();
    }

    /**
     * 读取默认通道标识。
     *
     * @return 默认通道标识
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public String channelKey() {
        return runtime.channelKey();
    }

    /**
     * 读取是否参与默认调度。
     *
     * @return 是否参与默认调度
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public boolean participateDefault() {
        return runtime.participateDefault();
    }

    /**
     * 创建已支持 OCR 模型命令。
     *
     * @param identity 模型基础身份信息
     * @param capability 模型能力配置
     * @param runtime 模型运行时默认配置
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public record CreateCommand(
            Identity identity,
            Capability capability,
            RuntimeDefaults runtime
    ) {
    }

    /**
     * OCR 模型基础身份信息。
     *
     * @param modelKey OCR 模型标识
     * @param name OCR 模型名称
     * @param description OCR 模型描述
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public record Identity(String modelKey, String name, String description) {
    }

    /**
     * OCR 模型能力配置。
     *
     * @param supportedInputs 支持的输入类型
     * @param ocrPath OCR 请求固定路径
     * @param healthPath 健康检查固定路径
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public record Capability(List<String> supportedInputs, String ocrPath, String healthPath) {
    }

    /**
     * OCR 模型运行时默认配置。
     *
     * @param defaultPort 默认服务端口
     * @param providerModel 默认厂商模型
     * @param channelKey 默认通道标识
     * @param participateDefault 是否参与默认调度
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public record RuntimeDefaults(int defaultPort, String providerModel, String channelKey, boolean participateDefault) {
    }

    /**
     * 标准化模型基础身份信息。
     *
     * @param identity 模型基础身份信息
     * @return 标准化后的模型基础身份信息
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private static Identity normalizeIdentity(Identity identity) {
        Identity checkedIdentity = Objects.requireNonNull(identity, "ocr model identity is required");
        return new Identity(requiredText(checkedIdentity.modelKey(), "ocr model key is required"),
                requiredText(checkedIdentity.name(), "ocr model name is required"),
                requiredText(checkedIdentity.description(), "ocr model description is required"));
    }

    /**
     * 标准化模型能力配置。
     *
     * @param capability 模型能力配置
     * @return 标准化后的模型能力配置
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private static Capability normalizeCapability(Capability capability) {
        Capability checkedCapability = Objects.requireNonNull(capability, "ocr model capability is required");
        return new Capability(List.copyOf(checkedCapability.supportedInputs()),
                requiredPath(checkedCapability.ocrPath(), "ocr path is required"),
                requiredPath(checkedCapability.healthPath(), "health path is required"));
    }

    /**
     * 标准化模型运行时默认配置。
     *
     * @param runtime 模型运行时默认配置
     * @return 标准化后的模型运行时默认配置
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private static RuntimeDefaults normalizeRuntime(RuntimeDefaults runtime) {
        RuntimeDefaults checkedRuntime = Objects.requireNonNull(runtime, "ocr model runtime is required");
        return new RuntimeDefaults(checkedRuntime.defaultPort(), optionalText(checkedRuntime.providerModel()),
                optionalText(checkedRuntime.channelKey()), checkedRuntime.participateDefault());
    }

    /**
     * 标准化可选文本。
     *
     * @param value 文本值
     * @return 标准化后的文本
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private static String optionalText(String value) {
        if (value == null) {
            return "";
        } else {
            return value.trim();
        }
    }

    /**
     * 校验必填文本。
     *
     * @param value 文本值
     * @param message 校验失败消息
     * @return 标准化后的文本
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static String requiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        } else {
            return value.trim();
        }
    }

    /**
     * 校验固定接口路径。
     *
     * @param value 接口路径
     * @param message 校验失败消息
     * @return 标准化后的接口路径
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static String requiredPath(String value, String message) {
        String path = requiredText(value, message);
        if (path.startsWith("/")) {
            return path;
        } else {
            throw new IllegalArgumentException(message);
        }
    }
}
