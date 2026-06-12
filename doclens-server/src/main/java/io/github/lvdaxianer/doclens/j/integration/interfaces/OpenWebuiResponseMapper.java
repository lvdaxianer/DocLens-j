package io.github.lvdaxianer.doclens.j.integration.interfaces;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Open WebUI OCR 响应映射器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
@Component
public class OpenWebuiResponseMapper {

    private static final String BATCH_ID_FIELD = "batch_id";
    private static final String STATUS_FIELD = "status";
    private static final String DOCUMENTS_FIELD = "documents";
    private static final String DOCUMENT_ID_FIELD = "document_id";
    private static final String CREATED_AT_FIELD = "created_at";
    private static final String UPDATED_AT_FIELD = "updated_at";
    private static final String FILE_NAME_FIELD = "file_name";
    private static final String TOTAL_FILES_FIELD = "total_files";
    private static final String COMPLETED_FILES_FIELD = "completed_files";
    private static final String FAILED_FILES_FIELD = "failed_files";
    private static final String TOTAL_DOCUMENTS_FIELD = "total_documents";
    private static final String COMPLETED_DOCUMENTS_FIELD = "completed_documents";
    private static final String FAILED_DOCUMENTS_FIELD = "failed_documents";
    private static final String FILENAME_FIELD = "filename";
    private static final String CONTENT_TYPE_FIELD = "content_type";
    private static final String TEXT_MARKDOWN_CONTENT_TYPE = "text/markdown";
    private static final String METADATA_FIELD = "metadata";
    private static final String RESULT_FIELD = "result";
    private static final String FINAL_TEXT_FIELD = "finalText";
    private static final String PAGE_TEXT_FIELD = "pageText";
    private static final String TEXT_FIELD = "text";
    private static final String PAGE_TEXT_RESULT_FIELD = "page_text";
    private static final String PAGE_COUNT_FIELD = "page_count";
    private static final String COMPLETED_PAGES_FIELD = "completed_pages";
    private static final String FAILED_PAGES_FIELD = "failed_pages";
    private static final String CURRENT_PAGE_FIELD = "current_page";
    private static final String TOTAL_PAGES_FIELD = "total_pages";
    private static final String EVENTS_FIELD = "events";
    private static final String STAGE_FIELD = "stage";
    private static final String ERROR_CODE_FIELD = "error_code";
    private static final String ERROR_MESSAGE_FIELD = "error_message";
    private static final String EMPTY_VALUE = "";
    private static final String QUEUED_STAGE = "QUEUED";
    private static final String PDF_EXTENSION = ".pdf";
    private static final String MARKDOWN_EXTENSION = ".md";
    private static final String MARKDOWN_LONG_EXTENSION = ".markdown";
    private static final String TXT_EXTENSION = ".txt";
    private static final String PNG_EXTENSION = ".png";
    private static final String JPG_EXTENSION = ".jpg";
    private static final String JPEG_EXTENSION = ".jpeg";
    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final String MARKDOWN_CONTENT_TYPE = "text/markdown";
    private static final String TEXT_CONTENT_TYPE = "text/plain";
    private static final String PNG_CONTENT_TYPE = "image/png";
    private static final String JPEG_CONTENT_TYPE = "image/jpeg";
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    /**
     * 将 DocLens 创建批次响应转换为 Open WebUI 创建响应。
     *
     * @param nativeResponse DocLens 原生响应
     * @return Open WebUI 创建响应
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public Map<String, Object> createdBatch(Map<String, Object> nativeResponse) {
        return Map.of(
                BATCH_ID_FIELD, nativeResponse.get(BATCH_ID_FIELD),
                STATUS_FIELD, nativeResponse.get(STATUS_FIELD),
                TOTAL_DOCUMENTS_FIELD, nativeResponse.get(TOTAL_FILES_FIELD),
                DOCUMENTS_FIELD, createdDocuments(nativeResponse),
                CREATED_AT_FIELD, createdAt(nativeResponse)
        );
    }

    /**
     * 将 DocLens 批次状态转换为 Open WebUI 响应。
     *
     * @param nativeResponse DocLens 批次状态
     * @return Open WebUI 批次状态
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public Map<String, Object> batch(Map<String, Object> nativeResponse) {
        return Map.ofEntries(
                Map.entry(BATCH_ID_FIELD, nativeResponse.get(BATCH_ID_FIELD)),
                Map.entry(STATUS_FIELD, nativeResponse.get(STATUS_FIELD)),
                Map.entry(TOTAL_DOCUMENTS_FIELD, nativeResponse.get(TOTAL_FILES_FIELD)),
                Map.entry(COMPLETED_DOCUMENTS_FIELD, nativeResponse.get(COMPLETED_FILES_FIELD)),
                Map.entry(FAILED_DOCUMENTS_FIELD, nativeResponse.get(FAILED_FILES_FIELD)),
                Map.entry(DOCUMENTS_FIELD, createdDocuments(nativeResponse)),
                Map.entry(CREATED_AT_FIELD, nativeResponse.get(CREATED_AT_FIELD)),
                Map.entry(UPDATED_AT_FIELD, nativeResponse.get(UPDATED_AT_FIELD))
        );
    }

    /**
     * 将 DocLens 文档状态转换为 Open WebUI 响应。
     *
     * @param nativeResponse DocLens 文档状态
     * @return Open WebUI 文档状态
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public Map<String, Object> document(Map<String, Object> nativeResponse) {
        String filename = String.valueOf(nativeResponse.get(FILE_NAME_FIELD));
        return Map.ofEntries(
                Map.entry(DOCUMENT_ID_FIELD, nativeResponse.get(DOCUMENT_ID_FIELD)),
                Map.entry(BATCH_ID_FIELD, nativeResponse.get(BATCH_ID_FIELD)),
                Map.entry(FILENAME_FIELD, filename),
                Map.entry(CONTENT_TYPE_FIELD, contentType(filename)),
                Map.entry(STATUS_FIELD, nativeResponse.get(STATUS_FIELD)),
                Map.entry(PAGE_COUNT_FIELD, nativeResponse.get(TOTAL_PAGES_FIELD)),
                Map.entry(COMPLETED_PAGES_FIELD, nativeResponse.get(CURRENT_PAGE_FIELD)),
                Map.entry(FAILED_PAGES_FIELD, failedPages(nativeResponse)),
                Map.entry(METADATA_FIELD, nativeResponse.get(METADATA_FIELD))
        );
    }

    /**
     * 将 DocLens 结果转换为 Open WebUI 响应。
     *
     * @param resultResponse DocLens 结果响应
     * @param documentResponse DocLens 文档响应
     * @return Open WebUI 结果响应
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public Map<String, Object> result(Map<String, Object> resultResponse, Map<String, Object> documentResponse) {
        Map<String, Object> result = resultPayload(resultResponse);
        return Map.of(
                DOCUMENT_ID_FIELD, resultResponse.get(DOCUMENT_ID_FIELD),
                CONTENT_TYPE_FIELD, TEXT_MARKDOWN_CONTENT_TYPE,
                TEXT_FIELD, result.get(FINAL_TEXT_FIELD),
                PAGE_TEXT_RESULT_FIELD, result.get(PAGE_TEXT_FIELD),
                METADATA_FIELD, documentResponse.get(METADATA_FIELD)
        );
    }

    /**
     * 将 DocLens 事件响应转换为 Open WebUI 响应。
     *
     * @param nativeResponse DocLens 事件响应
     * @return Open WebUI 事件响应
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public Map<String, Object> events(Map<String, Object> nativeResponse) {
        return Map.of(BATCH_ID_FIELD, nativeResponse.get(BATCH_ID_FIELD),
                EVENTS_FIELD, nativeResponse.getOrDefault(EVENTS_FIELD, List.of()));
    }

    /**
     * 将 DocLens 重试响应转换为 Open WebUI 响应。
     *
     * @param nativeResponse DocLens 重试响应
     * @return Open WebUI 重试响应
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public Map<String, Object> retry(Map<String, Object> nativeResponse) {
        return Map.ofEntries(
                Map.entry(DOCUMENT_ID_FIELD, value(nativeResponse, DOCUMENT_ID_FIELD)),
                Map.entry(STATUS_FIELD, value(nativeResponse, STATUS_FIELD)),
                Map.entry(STAGE_FIELD, QUEUED_STAGE),
                Map.entry(ERROR_CODE_FIELD, EMPTY_VALUE),
                Map.entry(ERROR_MESSAGE_FIELD, EMPTY_VALUE)
        );
    }

    /**
     * 转换创建响应中的文档列表。
     *
     * @param nativeResponse DocLens 原生响应
     * @return Open WebUI 文档列表
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private List<Map<String, Object>> createdDocuments(Map<String, Object> nativeResponse) {
        return documents(nativeResponse).stream().map(this::createdDocument).toList();
    }

    /**
     * 转换创建响应中的单个文档。
     *
     * @param document DocLens 原生文档摘要
     * @return Open WebUI 文档摘要
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private Map<String, Object> createdDocument(Map<String, Object> document) {
        String filename = String.valueOf(document.get(FILE_NAME_FIELD));
        return Map.of(
                DOCUMENT_ID_FIELD, document.get(DOCUMENT_ID_FIELD),
                FILENAME_FIELD, filename,
                CONTENT_TYPE_FIELD, contentType(filename),
                STATUS_FIELD, document.get(STATUS_FIELD)
        );
    }

    /**
     * 读取原生响应中的文档列表。
     *
     * @param nativeResponse DocLens 原生响应
     * @return 文档列表
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> documents(Map<String, Object> nativeResponse) {
        Object documents = nativeResponse.get(DOCUMENTS_FIELD);
        if (documents instanceof List<?> documentList) {
            return (List<Map<String, Object>>) documentList;
        } else {
            return List.of();
        }
    }

    /**
     * 读取创建时间，原生响应没有时使用当前时间兜底。
     *
     * @param nativeResponse DocLens 原生响应
     * @return 创建时间
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private String createdAt(Map<String, Object> nativeResponse) {
        Object createdAt = nativeResponse.get(CREATED_AT_FIELD);
        if (createdAt instanceof String text && !text.isBlank()) {
            return text;
        } else {
            return OffsetDateTime.now().toString();
        }
    }

    /**
     * 根据文件名推断响应 Content-Type。
     *
     * @param filename 文件名
     * @return Content-Type
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private String contentType(String filename) {
        String lowerName = filename.toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(PDF_EXTENSION)) {
            return PDF_CONTENT_TYPE;
        } else if (lowerName.endsWith(MARKDOWN_EXTENSION) || lowerName.endsWith(MARKDOWN_LONG_EXTENSION)) {
            return MARKDOWN_CONTENT_TYPE;
        } else if (lowerName.endsWith(TXT_EXTENSION)) {
            return TEXT_CONTENT_TYPE;
        } else if (lowerName.endsWith(PNG_EXTENSION)) {
            return PNG_CONTENT_TYPE;
        } else if (lowerName.endsWith(JPG_EXTENSION) || lowerName.endsWith(JPEG_EXTENSION)) {
            return JPEG_CONTENT_TYPE;
        } else {
            return DEFAULT_CONTENT_TYPE;
        }
    }

    /**
     * 根据文档状态计算失败页数。
     *
     * @param nativeResponse DocLens 文档状态
     * @return 失败页数
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private int failedPages(Map<String, Object> nativeResponse) {
        Object status = nativeResponse.get(STATUS_FIELD);
        if ("failed".equals(status)) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * 读取响应字段，缺失时返回空字符串。
     *
     * @param nativeResponse DocLens 原生响应
     * @param fieldName 字段名
     * @return 字段值或空字符串
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private Object value(Map<String, Object> nativeResponse, String fieldName) {
        Object value = nativeResponse.get(fieldName);
        if (value == null) {
            return EMPTY_VALUE;
        } else {
            return value;
        }
    }

    /**
     * 读取 DocLens 结果载荷。
     *
     * @param resultResponse DocLens 结果响应
     * @return 结果载荷
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> resultPayload(Map<String, Object> resultResponse) {
        Object result = resultResponse.get(RESULT_FIELD);
        if (result instanceof Map<?, ?> resultMap) {
            return (Map<String, Object>) resultMap;
        } else {
            return Map.of();
        }
    }
}
