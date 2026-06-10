package io.github.lvdaxianer.doclens.j.query.application;

import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.ProcessingStage;
import java.util.List;
import java.util.Map;

/**
 * Dashboard 阶段分布和图片进度读模型组装器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
class DashboardStageMetricsAssembler {

    private static final int RATIO_SCALE = 10000;
    private static final int PERCENT_SCALE = 100;

    /**
     * 组装细粒度阶段分布。
     *
     * @param documents 文档任务集合
     * @return 阶段分布读模型
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    List<Map<String, Object>> stageStatusCounts(List<DocumentJob> documents) {
        return List.of(
                stageStatusRow("queued", "待解析", List.of(ProcessingStage.QUEUED), documents),
                stageStatusRow("direct_text_saved", "直通文本保存", List.of(ProcessingStage.DIRECT_TEXT_SAVED),
                        documents),
                stageStatusRow("word_to_pdf", "Word 转 PDF", List.of(ProcessingStage.WORD_TO_PDF), documents),
                stageStatusRow("word_to_pdf_completed", "Word 转 PDF 完成",
                        List.of(ProcessingStage.WORD_TO_PDF_COMPLETED), documents),
                stageStatusRow("pdf_to_images", "PDF 转图片", List.of(ProcessingStage.PDF_TO_IMAGES), documents),
                stageStatusRow("pdf_to_images_completed", "PDF 转图片完成",
                        List.of(ProcessingStage.PDF_TO_IMAGES_COMPLETED), documents),
                stageStatusRow("ocr_images", "OCR 图片解析", List.of(ProcessingStage.OCR_IMAGES), documents),
                stageStatusRow("merge_text", "文本合并", List.of(ProcessingStage.MERGE_TEXT), documents),
                stageStatusRow("save_text", "文本保存", List.of(ProcessingStage.SAVE_TEXT), documents),
                stageStatusRow("completed", "解析完成", List.of(ProcessingStage.COMPLETED), documents),
                failedStageStatusRow(documents)
        );
    }

    /**
     * 组装全局图片页进度。
     *
     * @param documents 文档任务集合
     * @return 图片页进度读模型
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    Map<String, Object> imageProgress(List<DocumentJob> documents) {
        List<DocumentJob> imageDocuments = imageDocuments(documents);
        long completedImages = completedImages(imageDocuments);
        long totalImages = totalImages(imageDocuments);
        return Map.of("completed_images", completedImages, "total_images", totalImages,
                "progress_percent", ratio(completedImages, totalImages));
    }

    /**
     * 组装单个阶段分布行。
     *
     * @param stage 阶段编码
     * @param label 阶段名称
     * @param stages 归一化后的阶段集合
     * @param documents 文档任务集合
     * @return 阶段分布行
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private Map<String, Object> stageStatusRow(
            String stage,
            String label,
            List<ProcessingStage> stages,
            List<DocumentJob> documents
    ) {
        List<DocumentJob> matchedDocuments = stageDocuments(documents, stages);
        List<DocumentJob> imageDocuments = imageDocuments(matchedDocuments);
        return Map.of("stage", stage, "label", label, "document_count", (long) matchedDocuments.size(),
                "completed_images", completedImages(imageDocuments), "total_images", totalImages(imageDocuments));
    }

    /**
     * 组装失败阶段分布行。
     *
     * @param documents 文档任务集合
     * @return 失败阶段分布行
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private Map<String, Object> failedStageStatusRow(List<DocumentJob> documents) {
        long failedDocuments = documents.stream()
                .filter(document -> document.status().isFailureLike())
                .count();
        return Map.of("stage", "failed", "label", "解析失败", "document_count", failedDocuments,
                "completed_images", 0L, "total_images", 0L);
    }

    /**
     * 筛选指定阶段文档。
     *
     * @param documents 文档任务集合
     * @param stages 归一化后的阶段集合
     * @return 指定阶段文档集合
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private List<DocumentJob> stageDocuments(List<DocumentJob> documents, List<ProcessingStage> stages) {
        return documents.stream()
                .filter(document -> stages.contains(ProcessingTrackAssembler.normalizeStage(document.stage())))
                .toList();
    }

    /**
     * 筛选存在图片页进度的文档。
     *
     * @param documents 文档任务集合
     * @return 图片页进度文档集合
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private List<DocumentJob> imageDocuments(List<DocumentJob> documents) {
        return documents.stream()
                .filter(document -> ProcessingTrackAssembler.isImageProgressStage(document.stage()))
                .toList();
    }

    /**
     * 汇总已完成图片页数。
     *
     * @param documents 文档任务集合
     * @return 已完成图片页数
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private long completedImages(List<DocumentJob> documents) {
        return documents.stream().mapToLong(DocumentJob::currentPage).sum();
    }

    /**
     * 汇总总图片页数。
     *
     * @param documents 文档任务集合
     * @return 总图片页数
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private long totalImages(List<DocumentJob> documents) {
        return documents.stream().mapToLong(DocumentJob::totalPages).sum();
    }

    /**
     * 计算百分比。
     *
     * @param numerator 分子
     * @param denominator 分母
     * @return 百分比数值
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private double ratio(long numerator, long denominator) {
        double value;
        if (denominator > 0) {
            // 有分母时按百分比保留两位小数。
            value = Math.round(numerator * (double) RATIO_SCALE / denominator) / (double) PERCENT_SCALE;
        } else {
            // 空集合场景展示 0，避免除零异常。
            value = 0D;
        }
        return value;
    }

}
