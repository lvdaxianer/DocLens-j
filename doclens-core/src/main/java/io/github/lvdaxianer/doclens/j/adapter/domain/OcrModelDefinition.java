package io.github.lvdaxianer.doclens.j.adapter.domain;

import java.util.List;

/**
 * 系统已支持 OCR 模型定义。
 *
 * @param modelKey OCR 模型标识
 * @param name OCR 模型名称
 * @param description OCR 模型描述
 * @param supportedInputs 支持的输入类型
 * @param ocrPath OCR 请求固定路径
 * @param healthPath 健康检查固定路径
 * @param participateDefault 是否参与默认调度
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public record OcrModelDefinition(
        String modelKey,
        String name,
        String description,
        List<String> supportedInputs,
        String ocrPath,
        String healthPath,
        boolean participateDefault
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
        return new OcrModelDefinition(requiredText(command.modelKey(), "ocr model key is required"),
                requiredText(command.name(), "ocr model name is required"),
                requiredText(command.description(), "ocr model description is required"),
                List.copyOf(command.supportedInputs()),
                requiredPath(command.ocrPath(), "ocr path is required"),
                requiredPath(command.healthPath(), "health path is required"),
                command.participateDefault());
    }

    /**
     * 创建已支持 OCR 模型命令。
     *
     * @param modelKey OCR 模型标识
     * @param name OCR 模型名称
     * @param description OCR 模型描述
     * @param supportedInputs 支持的输入类型
     * @param ocrPath OCR 请求固定路径
     * @param healthPath 健康检查固定路径
     * @param participateDefault 是否参与默认调度
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public record CreateCommand(
            String modelKey,
            String name,
            String description,
            List<String> supportedInputs,
            String ocrPath,
            String healthPath,
            boolean participateDefault
    ) {
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
