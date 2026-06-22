package io.github.lvdaxianer.doclens.j.ingestion.interfaces;

import io.github.lvdaxianer.doclens.j.api.CreateBatchRequest;
import io.github.lvdaxianer.doclens.j.api.DocumentInput;
import io.github.lvdaxianer.doclens.j.ingestion.domain.CallerIdentity;
import io.github.lvdaxianer.doclens.j.ingestion.infrastructure.CallerCredentialResolver;
import io.github.lvdaxianer.doclens.j.processing.application.ChunkStrategy;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.JsonCodec;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
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
    private static final String DEFAULT_FILE_NAME = "uploaded.bin";

    private final JsonCodec jsonCodec;
    private final CallerCredentialResolver callerCredentialResolver;

    /**
     * 创建请求映射器。
     *
     * @param jsonCodec JSON 编解码器
     * @param callerCredentialResolver 接入方凭证解析器
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public CreateBatchRequestMapper(JsonCodec jsonCodec, CallerCredentialResolver callerCredentialResolver) {
        this.jsonCodec = jsonCodec;
        this.callerCredentialResolver = callerCredentialResolver;
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
        CallerIdentity caller = callerCredentialResolver.resolve(request.getHeader(CALLER_PARTITION_HEADER), "");
        return new CreateBatchRequest(uploadFiles, jsonCodec.parseObject(form.metadata()), form.callbackUrl(),
                form.idempotencyKey(), form.adapterOverride(), form.pdfMode(), form.chunkStrategy(),
                Boolean.parseBoolean(form.llmOrchestrated()), form.ocrRoutingMode(), form.ocrModelKey(), form.ocrNodeId(), form.ocrLoadBalanceStrategy(),
                caller.clientId(),
                caller.sourceApp(), caller.tenantKey().orElse(""));
    }

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

    private void validateForm(CreateBatchForm form) {
        if (form.files() == null || form.files().isEmpty()) {
            throw new IllegalArgumentException("files is required");
        } else if (StringUtils.hasText(form.callbackUrl()) && !form.callbackUrl().startsWith("http")) {
            throw new IllegalArgumentException("callback_url must be http or https URL");
        } else {
            // 表单包含文件，且可接受可选回调地址。
        }
    }

    private DocumentInput toDocumentInput(MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename() == null ? DEFAULT_FILE_NAME : file.getOriginalFilename();
            return new DocumentInput(fileName, file.getBytes());
        } catch (IOException ex) {
            throw new IllegalArgumentException("failed to read uploaded file", ex);
        }
    }
}
