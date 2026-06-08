package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * OCR 结构化结果的 MyBatis-Plus 实体。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@Getter
@Setter
@TableName("ocr_results")
public class OcrResultEntity {

    @TableId("result_id")
    private String resultId;
    private String documentId;
    private String finalText;
    private String markdownStorageUri;
    private String rawVendorOutput;
    private String structuredDocument;
    private String pageText;
    private String layoutBlocks;
    private String tables;
    private String images;
    private double confidence;
    private String warnings;
    private OffsetDateTime createdAt;
}
