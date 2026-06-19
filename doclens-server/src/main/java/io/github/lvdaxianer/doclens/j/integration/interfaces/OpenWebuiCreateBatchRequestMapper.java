package io.github.lvdaxianer.doclens.j.integration.interfaces;

import io.github.lvdaxianer.doclens.j.api.CreateBatchRequest;
import io.github.lvdaxianer.doclens.j.api.DocumentInput;
import io.github.lvdaxianer.doclens.j.ingestion.domain.CallerIdentity;
import io.github.lvdaxianer.doclens.j.shared.web.CallerIdentityRequestResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

/**
 * Open WebUI OCR 创建批次请求映射器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
@Component
public class OpenWebuiCreateBatchRequestMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenWebuiCreateBatchRequestMapper.class);

    private static final String CALLBACK_URL_PARAM = "callback_url";
    private static final String IDEMPOTENCY_KEY_PARAM = "idempotency_key";
    private static final String ADAPTER_OVERRIDE_PARAM = "adapter_override";
    private static final String PDF_MODE_PARAM = "pdf_mode";
    private static final String FILES_PARAM = "files";
    private static final String DEFAULT_FILE_NAME = "uploaded.bin";

    private final CallerIdentityRequestResolver callerIdentityRequestResolver;

    /**
     * 将 Open WebUI multipart 请求转换为 DocLens 创建请求。
     *
     * @param callerIdentityRequestResolver caller 请求解析器
     * @param request HTTP 请求
     * @param metadata Open WebUI metadata
     * @return DocLens 创建批次请求
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public OpenWebuiCreateBatchRequestMapper(CallerIdentityRequestResolver callerIdentityRequestResolver) {
        this.callerIdentityRequestResolver = callerIdentityRequestResolver;
    }

    /**
     * 将 Open WebUI multipart 请求转换为 DocLens 创建请求。
     *
     * @param request HTTP 请求
     * @param metadata Open WebUI metadata
     * @return DocLens 创建批次请求
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public CreateBatchRequest toRequest(HttpServletRequest request, OpenWebuiMetadata metadata) {
        List<MultipartFile> files = extractFiles(request);
        CallerIdentity caller = callerIdentityRequestResolver.resolve(request);
        return new CreateBatchRequest(toDocumentInputs(files), metadata.values(), request.getParameter(CALLBACK_URL_PARAM),
                request.getParameter(IDEMPOTENCY_KEY_PARAM), request.getParameter(ADAPTER_OVERRIDE_PARAM),
                request.getParameter(PDF_MODE_PARAM), null, false, null, null, null, null, caller.clientId(),
                caller.sourceApp(), caller.tenantKey().orElse(""));
    }

    /**
     * 从已鉴权请求中提取上传文件集合。
     *
     * @param request HTTP 请求
     * @return 上传文件集合
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private List<MultipartFile> extractFiles(HttpServletRequest request) {
        if (request instanceof MultipartHttpServletRequest multipartRequest) {
            // 请求已通过鉴权且为 multipart 时读取文件字段。
            return multipartRequest.getFiles(FILES_PARAM);
        } else {
            // 非 multipart 请求视为参数错误，而不是绕过鉴权返回框架级 500。
            return List.of();
        }
    }

    /**
     * 将上传文件集合转换为 DocLens 文档输入集合。
     *
     * @param files 上传文件集合
     * @return DocLens 文档输入集合
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private List<DocumentInput> toDocumentInputs(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            // Open WebUI 必须至少上传一个待解析文件。
            throw OpenWebuiIntegrationException.badRequest("files is required");
        } else {
            // 文件集合存在时只做内存转换，不在循环内访问远程服务或数据库。
            return files.stream().map(this::toDocumentInput).toList();
        }
    }

    /**
     * 将单个上传文件转换为 DocLens 文档输入。
     *
     * @param file 上传文件
     * @return DocLens 文档输入
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private DocumentInput toDocumentInput(MultipartFile file) {
        try {
            return new DocumentInput(fileName(file), file.getBytes());
        } catch (IOException ex) {
            LOGGER.warn("[OpenWebUI集成] 读取上传文件失败, filename={}", fileName(file), ex);
            throw OpenWebuiIntegrationException.badRequest("failed to read uploaded file", ex);
        }
    }

    /**
     * 读取上传文件名。
     *
     * @param file 上传文件
     * @return 文件名
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private String fileName(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            // Open WebUI 未提供文件名时使用稳定默认名兜底。
            return DEFAULT_FILE_NAME;
        } else {
            // Open WebUI 提供文件名时保留原始文件名用于后续类型推断。
            return fileName;
        }
    }
}
