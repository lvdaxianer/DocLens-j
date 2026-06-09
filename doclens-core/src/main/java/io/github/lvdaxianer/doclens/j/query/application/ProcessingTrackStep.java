package io.github.lvdaxianer.doclens.j.query.application;

/**
 * Dashboard 文档处理轨道步骤定义。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
enum ProcessingTrackStep {
    UPLOAD("上传", 0),
    TYPE_RECOGNITION("类型识别", 1),
    CONVERSION("转换", 2),
    RENDERING("渲染页图", 3),
    OCR("OCR", 4),
    MERGE("合并文本", 5),
    LLM_MARKDOWN("LLM 排版", 6),
    SAVE("入库/落盘", 7);

    private final String label;
    private final int rank;

    /**
     * 创建处理轨道步骤。
     *
     * @param label 展示名称
     * @param rank 步骤序号
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    ProcessingTrackStep(String label, int rank) {
        this.label = label;
        this.rank = rank;
    }

    /**
     * 获取展示名称。
     *
     * @return 展示名称
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    String label() {
        return label;
    }

    /**
     * 获取步骤序号。
     *
     * @return 步骤序号
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    int rank() {
        return rank;
    }
}
