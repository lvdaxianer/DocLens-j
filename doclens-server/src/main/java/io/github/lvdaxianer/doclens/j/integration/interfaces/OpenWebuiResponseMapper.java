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
    private static final String FILE_NAME_FIELD = "file_name";
    private static final String TOTAL_FILES_FIELD = "total_files";
    private static final String TOTAL_DOCUMENTS_FIELD = "total_documents";
    private static final String FILENAME_FIELD = "filename";
    private static final String CONTENT_TYPE_FIELD = "content_type";
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
}
