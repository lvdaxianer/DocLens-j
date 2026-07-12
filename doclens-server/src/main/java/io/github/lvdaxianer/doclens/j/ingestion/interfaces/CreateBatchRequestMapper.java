package io.github.lvdaxianer.doclens.j.ingestion.interfaces;

import io.github.lvdaxianer.doclens.j.api.CreateBatchRequest;
import io.github.lvdaxianer.doclens.j.api.DocumentInput;
import io.github.lvdaxianer.doclens.j.ingestion.domain.CallerIdentity;
import io.github.lvdaxianer.doclens.j.ingestion.infrastructure.CallerPartitionResolver;
import io.github.lvdaxianer.doclens.j.processing.application.ChunkStrategy;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.JsonCodec;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 将 multipart HTTP 请求映射为批次创建命令。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@Component
public class CreateBatchRequestMapper {

    private static final String METADATA_PARAM = "metadata";
    private static final String CALLBACK_URL_PARAM = "callback_url";
    private static final String IDEMPOTENCY_KEY_PARAM = "idempotency_key";
    private static final String ADAPTER_OVERRIDE_PARAM = "adapter_override";
    private static final String PDF_MODE_PARAM = "pdf_mode";
    private static final String OCR_ROUTING_MODE_PARAM = "ocrRoutingMode";
    private static final String OCR_MODEL_KEY_PARAM = "ocrModelKey";
    private static final String OCR_NODE_ID_PARAM = "ocrNodeId";
    private static final String OCR_LOAD_BALANCE_STRATEGY_PARAM = "ocrLoadBalanceStrategy";
    private static final String OCR_ROUTING_MODE_SNAKE_PARAM = "ocr_routing_mode";
    private static final String OCR_MODEL_KEY_SNAKE_PARAM = "ocr_model_key";
    private static final String OCR_NODE_ID_SNAKE_PARAM = "ocr_node_id";
    private static final String OCR_LOAD_BALANCE_STRATEGY_SNAKE_PARAM = "ocr_load_balance_strategy";
    private static final String LLM_ORCHESTRATED_PARAM = "llmOrchestrated";
    private static final String LLM_ORCHESTRATED_SNAKE_PARAM = "llm_orchestrated";
    private static final String CALLER_PARTITION_HEADER = "X-Doclens-Key";
    private static final String HTTP_SCHEME = "http";
    private static final String HTTPS_SCHEME = "https";
    private static final String DEFAULT_FILE_NAME = "uploaded.bin";
    private static final int MAX_UPLOAD_FILE_COUNT = 30;

    private final JsonCodec jsonCodec;
    private final CallerPartitionResolver callerPartitionResolver;

    /**
     * 创建请求映射器。
     *
     * @param jsonCodec JSON 编解码器
     * @param callerPartitionResolver caller 分区键解析器
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public CreateBatchRequestMapper(JsonCodec jsonCodec, CallerPartitionResolver callerPartitionResolver) {
        this.jsonCodec = jsonCodec;
        this.callerPartitionResolver = callerPartitionResolver;
    }

    /**
     * 将 multipart 输入转换为应用命令。
     *
     * @param files 已上传文件集合
     * @param request 携带可选表单字段的 Servlet 请求
     * @return 创建批次请求
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public CreateBatchRequest toRequest(List<MultipartFile> files, HttpServletRequest request) {
        CreateBatchForm form = toForm(files, request);
        validateForm(form);
        List<DocumentInput> uploadFiles = form.files().stream()
                .map(this::toDocumentInput)
                .toList();
        CallerIdentity caller = callerPartitionResolver.resolve(request.getHeader(CALLER_PARTITION_HEADER));
        return new CreateBatchRequest(uploadFiles, jsonCodec.parseObject(form.metadata()), form.callbackUrl(),
                form.idempotencyKey(), form.adapterOverride(), form.pdfMode(), form.chunkStrategy(),
                Boolean.parseBoolean(form.llmOrchestrated()), form.ocrRoutingMode(), form.ocrModelKey(), form.ocrNodeId(), form.ocrLoadBalanceStrategy(),
                caller.clientId(),
                caller.sourceApp(), caller.tenantKey().orElse(""));
    }

    /**
     * 从 multipart 请求读取创建批次表单。
     *
     * @param files 已上传文件集合
     * @param request HTTP 请求
     * @return 创建批次表单
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    private CreateBatchForm toForm(List<MultipartFile> files, HttpServletRequest request) {
        return new CreateBatchForm(files, request.getParameter(METADATA_PARAM),
                request.getParameter(CALLBACK_URL_PARAM), request.getParameter(IDEMPOTENCY_KEY_PARAM),
                request.getParameter(ADAPTER_OVERRIDE_PARAM), request.getParameter(PDF_MODE_PARAM),
                parameter(request, "chunkStrategy", "chunk_strategy"),
                parameter(request, LLM_ORCHESTRATED_PARAM, LLM_ORCHESTRATED_SNAKE_PARAM),
                parameter(request, OCR_ROUTING_MODE_PARAM, OCR_ROUTING_MODE_SNAKE_PARAM),
                parameter(request, OCR_MODEL_KEY_PARAM, OCR_MODEL_KEY_SNAKE_PARAM),
                parameter(request, OCR_NODE_ID_PARAM, OCR_NODE_ID_SNAKE_PARAM),
                parameter(request, OCR_LOAD_BALANCE_STRATEGY_PARAM, OCR_LOAD_BALANCE_STRATEGY_SNAKE_PARAM));
    }

    /**
     * 读取兼容 camelCase 与 snake_case 的表单参数。
     *
     * @param request HTTP 请求
     * @param primaryName 首选参数名
     * @param fallbackName 兼容参数名
     * @return 表单参数值
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private String parameter(HttpServletRequest request, String primaryName, String fallbackName) {
        String value = request.getParameter(primaryName);
        if (StringUtils.hasText(value)) {
            return value;
        } else {
            return request.getParameter(fallbackName);
        }
    }

    /**
     * 校验创建批次表单。
     *
     * @param form 创建批次表单
     * @author lvdaxianer@yeah.net
     * @date 2026-07-12
     */
    private void validateForm(CreateBatchForm form) {
        if (form.files() == null || form.files().isEmpty()) {
            // 上传文件是创建批次的必要输入。
            throw new IllegalArgumentException("files is required");
        } else {
            // 文件存在时继续校验批次规模和可选回调地址。
            validateFileCount(form.files());
            validateCallbackUrl(form.callbackUrl());
        }
    }

    /**
     * 校验单批上传文件数量。
     *
     * @param files 上传文件集合
     * @author lvdaxianer@yeah.net
     * @date 2026-07-12
     */
    private void validateFileCount(List<MultipartFile> files) {
        if (files.size() > MAX_UPLOAD_FILE_COUNT) {
            // 服务端必须兜底 Dashboard 的批次文件数量上限。
            throw new IllegalArgumentException("单个批次最多上传 30 个文件，请拆分后再上传");
        } else {
            // 文件数量满足批次上限。
        }
    }

    /**
     * 校验回调地址协议。
     *
     * @param callbackUrl 回调地址
     * @author lvdaxianer@yeah.net
     * @date 2026-07-12
     */
    private void validateCallbackUrl(String callbackUrl) {
        if (!StringUtils.hasText(callbackUrl)) {
            // 未配置回调地址时无需校验协议。
        } else if (callbackScheme(callbackUrl).filter(this::isHttpCallbackScheme).isEmpty()) {
            // 已配置回调地址时只允许 HTTP/HTTPS 目标。
            throw invalidCallbackUrlException();
        } else {
            // 回调协议合法时继续处理。
        }
    }

    /**
     * 解析回调地址协议。
     *
     * @param callbackUrl 回调地址
     * @return 回调地址协议
     * @author lvdaxianer@yeah.net
     * @date 2026-07-12
     */
    private Optional<String> callbackScheme(String callbackUrl) {
        try {
            return Optional.ofNullable(new URI(callbackUrl).getScheme());
        } catch (URISyntaxException ex) {
            // URI 解析失败时保留异常链，交给统一 validation 响应处理。
            throw invalidCallbackUrlException(ex);
        }
    }

    /**
     * 判断回调地址协议是否为 HTTP 协议。
     *
     * @param scheme 回调地址协议
     * @return 是否为 HTTP/HTTPS 协议
     * @author lvdaxianer@yeah.net
     * @date 2026-07-12
     */
    private boolean isHttpCallbackScheme(String scheme) {
        return HTTP_SCHEME.equalsIgnoreCase(scheme) || HTTPS_SCHEME.equalsIgnoreCase(scheme);
    }

    /**
     * 创建回调地址非法异常。
     *
     * @return 回调地址非法异常
     * @author lvdaxianer@yeah.net
     * @date 2026-07-12
     */
    private IllegalArgumentException invalidCallbackUrlException() {
        return new IllegalArgumentException("callback_url must be http or https URL");
    }

    /**
     * 创建保留原始异常的回调地址非法异常。
     *
     * @param cause 原始 URI 解析异常
     * @return 回调地址非法异常
     * @author lvdaxianer@yeah.net
     * @date 2026-07-12
     */
    private IllegalArgumentException invalidCallbackUrlException(URISyntaxException cause) {
        return new IllegalArgumentException("callback_url must be http or https URL", cause);
    }

    /**
     * 将 multipart 文件转换为 SDK 文档输入。
     *
     * @param file multipart 文件
     * @return SDK 文档输入
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    private DocumentInput toDocumentInput(MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename() == null ? DEFAULT_FILE_NAME : file.getOriginalFilename();
            return new DocumentInput(fileName, file.getBytes());
        } catch (IOException ex) {
            throw new IllegalArgumentException("failed to read uploaded file", ex);
        }
    }
}
