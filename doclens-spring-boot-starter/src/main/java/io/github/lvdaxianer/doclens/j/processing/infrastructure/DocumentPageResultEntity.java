package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 文档页 OCR 结果的 MyBatis-Plus 实体。
 * 每条记录对应一个 documentId + pageNo 的幂等页结果。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
@Getter
@Setter
@TableName("ocr_document_page_results")
public class DocumentPageResultEntity {

    /** 页结果主键，由 documentId 与 pageNo 组成。 */
    @TableId("page_result_id")
    private String pageResultId;
    /** 所属文档 ID。 */
    private String documentId;
    /** 文档内页码，从 1 开始。 */
    private int pageNo;
    /** 原始 OCR 输出 JSON。 */
    private String rawOutput;
    /** 当前页文本。 */
    private String pageText;
    /** 版面块 JSON。 */
    private String layoutBlocks;
    /** OCR 置信度。 */
    private double confidence;
    /** 归一化警告 JSON。 */
    private String warnings;
    /** 实际执行 OCR 的节点 ID。 */
    private String nodeId;
    /** 创建时间。 */
    private OffsetDateTime createdAt;
    /** 更新时间。 */
    private OffsetDateTime updatedAt;
}
