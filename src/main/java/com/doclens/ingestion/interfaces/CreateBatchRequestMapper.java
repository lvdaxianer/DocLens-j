package com.doclens.ingestion.interfaces;

import com.doclens.ingestion.application.CreateBatchCommand;
import com.doclens.ingestion.application.UploadFileCommand;
import com.doclens.shared.infrastructure.JsonCodec;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Maps multipart HTTP requests to batch creation commands.
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
    private static final String DEFAULT_FILE_NAME = "uploaded.bin";

    private final JsonCodec jsonCodec;

    /**
     * Creates request mapper.
     *
     * @param jsonCodec JSON codec
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public CreateBatchRequestMapper(JsonCodec jsonCodec) {
        this.jsonCodec = jsonCodec;
    }

    /**
     * Converts multipart input into an application command.
     *
     * @param files uploaded files
     * @param request servlet request carrying optional form fields
     * @return create batch command
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public CreateBatchCommand toCommand(List<MultipartFile> files, HttpServletRequest request) {
        CreateBatchForm form = toForm(files, request);
        validateForm(form);
        List<UploadFileCommand> uploadFiles = form.files().stream()
                .map(this::toUploadFileCommand)
                .toList();
        return new CreateBatchCommand(uploadFiles, jsonCodec.parseObject(form.metadata()), form.callbackUrl(),
                form.idempotencyKey(), form.adapterOverride(), form.pdfMode());
    }

    private CreateBatchForm toForm(List<MultipartFile> files, HttpServletRequest request) {
        return new CreateBatchForm(files, request.getParameter(METADATA_PARAM),
                request.getParameter(CALLBACK_URL_PARAM), request.getParameter(IDEMPOTENCY_KEY_PARAM),
                request.getParameter(ADAPTER_OVERRIDE_PARAM), request.getParameter(PDF_MODE_PARAM));
    }

    private void validateForm(CreateBatchForm form) {
        if (form.files() == null || form.files().isEmpty()) {
            throw new IllegalArgumentException("files is required");
        } else if (StringUtils.hasText(form.callbackUrl()) && !form.callbackUrl().startsWith("http")) {
            throw new IllegalArgumentException("callback_url must be http or https URL");
        } else {
            // Form contains files and optional callback URL is acceptable.
        }
    }

    private UploadFileCommand toUploadFileCommand(MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename() == null ? DEFAULT_FILE_NAME : file.getOriginalFilename();
            return new UploadFileCommand(fileName, file.getBytes());
        } catch (IOException ex) {
            throw new IllegalArgumentException("failed to read uploaded file", ex);
        }
    }
}
