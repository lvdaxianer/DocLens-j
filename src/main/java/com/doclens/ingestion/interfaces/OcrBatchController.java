package com.doclens.ingestion.interfaces;

import com.doclens.ingestion.application.CreateBatchUseCase;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * OCR batch upload API controller.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@RestController
@RequestMapping("/api/v1")
public class OcrBatchController {

    private static final String FILES_PARAM = "files";

    private final CreateBatchUseCase createBatchUseCase;
    private final CreateBatchRequestMapper requestMapper;

    /**
     * Creates OCR batch controller.
     *
     * @param createBatchUseCase create batch use case
     * @param requestMapper multipart request mapper
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public OcrBatchController(CreateBatchUseCase createBatchUseCase, CreateBatchRequestMapper requestMapper) {
        this.createBatchUseCase = createBatchUseCase;
        this.requestMapper = requestMapper;
    }

    /**
     * Creates an OCR batch from multipart upload.
     *
     * @param files uploaded files
     * @param request servlet request carrying optional form fields
     * @return upload response
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @PostMapping("/batches")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> createBatch(
            @RequestParam(FILES_PARAM) List<MultipartFile> files,
            HttpServletRequest request
    ) {
        return createBatchUseCase.create(requestMapper.toCommand(files, request));
    }
}
