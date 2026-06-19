package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * OCR 文档任务的 MyBatis-Plus 实体。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@Getter
@Setter
@TableName("ocr_documents")
public class DocumentJobEntity {

    @TableId("document_id")
    private String documentId;
    private String batchId;
    private String fileName;
    private String fileType;
    private long fileSize;
    private int pageCount;
    private String storageUri;
    private String status;
    private String stage;
    private int progressPercent;
    private int currentPage;
    private int totalPages;
    private String adapterName;
    private String pdfMode;
    private String chunkStrategy;
    private String ocrRoutingMode;
    private String ocrModelKey;
    private String ocrNodeId;
    private String ocrLoadBalanceStrategy;
    private String metadata;
    private String resultId;
    private String errorCode;
    private String errorMessage;
    private int sortOrder;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
