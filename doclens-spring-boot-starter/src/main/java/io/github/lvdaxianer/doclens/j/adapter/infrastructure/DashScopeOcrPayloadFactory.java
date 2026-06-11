package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrRequest;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import java.io.IOException;
import java.util.Base64;

/**
 * DashScope 在线 OCR 请求载荷工厂。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
final class DashScopeOcrPayloadFactory {

    private static final String OCR_PROMPT = "请仅输出图像中的文本内容。";
    private static final String FIELD_MODEL = "model";
    private static final String FIELD_MESSAGES = "messages";
    private static final String FIELD_ROLE = "role";
    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_URL = "url";
    private static final String ROLE_USER = "user";
    private static final String CONTENT_TYPE_TEXT = "text";
    private static final String CONTENT_TYPE_IMAGE_URL = "image_url";
    private static final String MIME_IMAGE_JPEG = "image/jpeg";
    private static final String MIME_IMAGE_PNG = "image/png";
    private static final String MIME_IMAGE_WEBP = "image/webp";
    private static final byte[] PERMISSION_PROBE_IMAGE = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x04, 0x00, 0x00, 0x00, (byte) 0xB5, 0x1C, 0x0C,
            0x02, 0x00, 0x00, 0x00, 0x0B, 0x49, 0x44, 0x41,
            0x54, 0x78, (byte) 0xDA, 0x63, (byte) 0xFC, (byte) 0xFF, 0x1F, 0x00,
            0x03, 0x03, 0x02, 0x00, (byte) 0xEF, (byte) 0xBF, 0x55, (byte) 0x9D,
            0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
            (byte) 0xAE, 0x42, 0x60, (byte) 0x82
    };

    private final ObjectMapper objectMapper;

    /**
     * 创建 DashScope 在线 OCR 请求载荷工厂。
     *
     * @param objectMapper JSON 映射器
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    DashScopeOcrPayloadFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 构建 DashScope compatible chat completions 请求体。
     *
     * @param node OCR 运行时节点
     * @param request 图片 OCR 请求
     * @return JSON 请求体
     * @throws IOException JSON 序列化失败
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    String buildRequestBody(OcrRuntimeNode node, ImageOcrRequest request) throws IOException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put(FIELD_MODEL, providerModel(node));
        body.set(FIELD_MESSAGES, messages(request));
        return objectMapper.writeValueAsString(body);
    }

    /**
     * 创建在线权限探测使用的最小 OCR 请求。
     *
     * @return 探测请求
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    ImageOcrRequest permissionProbeRequest() {
        return new ImageOcrRequest("health-check", "health-check", "probe.png", 1, PERMISSION_PROBE_IMAGE,
                JsonPayload.empty());
    }

    /**
     * 读取在线 OCR 模型名称。
     *
     * @param node OCR 运行时节点
     * @return 在线 OCR 模型名称
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    String providerModel(OcrRuntimeNode node) {
        return node.node().providerModel()
                .orElseThrow(() -> new IllegalStateException("online OCR provider model is not configured"));
    }

    /**
     * 构建包含固定 OCR 提示词和图片内容的消息。
     *
     * @param request 图片 OCR 请求
     * @return compatible messages
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private ArrayNode messages(ImageOcrRequest request) {
        ArrayNode messages = objectMapper.createArrayNode();
        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put(FIELD_ROLE, ROLE_USER);
        userMessage.set(FIELD_CONTENT, messageContent(request));
        messages.add(userMessage);
        return messages;
    }

    /**
     * 构建多模态消息内容。
     *
     * @param request 图片 OCR 请求
     * @return 多模态消息内容
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private ArrayNode messageContent(ImageOcrRequest request) {
        ArrayNode content = objectMapper.createArrayNode();
        content.add(textPart());
        content.add(imagePart(request));
        return content;
    }

    /**
     * 构建固定文本提示词片段。
     *
     * @return 文本提示词片段
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private ObjectNode textPart() {
        ObjectNode textPart = objectMapper.createObjectNode();
        textPart.put(FIELD_TYPE, CONTENT_TYPE_TEXT);
        textPart.put(CONTENT_TYPE_TEXT, OCR_PROMPT);
        return textPart;
    }

    /**
     * 构建 base64 data URL 图片片段。
     *
     * @param request 图片 OCR 请求
     * @return 图片片段
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private ObjectNode imagePart(ImageOcrRequest request) {
        ObjectNode imagePart = objectMapper.createObjectNode();
        ObjectNode imageUrl = objectMapper.createObjectNode();
        imagePart.put(FIELD_TYPE, CONTENT_TYPE_IMAGE_URL);
        imageUrl.put(FIELD_URL, dataUrl(request));
        imagePart.set(CONTENT_TYPE_IMAGE_URL, imageUrl);
        return imagePart;
    }

    /**
     * 构建图片 data URL。
     *
     * @param request 图片 OCR 请求
     * @return 图片 data URL
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private String dataUrl(ImageOcrRequest request) {
        String encodedImage = Base64.getEncoder().encodeToString(request.imageContent());
        return "data:" + mimeType(request.fileName()) + ";base64," + encodedImage;
    }

    /**
     * 根据文件名推断图片 MIME 类型。
     *
     * @param fileName 文件名
     * @return MIME 类型
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private String mimeType(String fileName) {
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            // jpg 与 jpeg 后缀统一按 image/jpeg 上报。
            return MIME_IMAGE_JPEG;
        } else if (lowerName.endsWith(".webp")) {
            // webp 后缀需要声明对应 MIME，避免多模态服务误判。
            return MIME_IMAGE_WEBP;
        } else {
            // 其他输入默认按 PNG 探测，兼容当前上传路径。
            return MIME_IMAGE_PNG;
        }
    }
}
