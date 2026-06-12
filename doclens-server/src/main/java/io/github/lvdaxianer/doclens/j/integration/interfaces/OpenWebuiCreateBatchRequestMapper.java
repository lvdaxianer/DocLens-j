package io.github.lvdaxianer.doclens.j.integration.interfaces;

import io.github.lvdaxianer.doclens.j.api.CreateBatchRequest;
import io.github.lvdaxianer.doclens.j.api.DocumentInput;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

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
    private static final String DEFAULT_FILE_NAME = "uploaded.bin";

    /**
     * 将 Open WebUI multipart 请求转换为 DocLens 创建请求。
     *
     * @param files 上传文件集合
     * @param request HTTP 请求
     * @param metadata Open WebUI metadata
     * @return DocLens 创建批次请求
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public CreateBatchRequest toRequest(
            List<MultipartFile> files,
            HttpServletRequest request,
            OpenWebuiMetadata metadata
    ) {
        return new CreateBatchRequest(toDocumentInputs(files), metadata.values(), request.getParameter(CALLBACK_URL_PARAM),
                request.getParameter(IDEMPOTENCY_KEY_PARAM), request.getParameter(ADAPTER_OVERRIDE_PARAM),
                request.getParameter(PDF_MODE_PARAM));
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
