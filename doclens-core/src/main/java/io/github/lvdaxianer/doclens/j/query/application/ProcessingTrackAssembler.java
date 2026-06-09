package io.github.lvdaxianer.doclens.j.query.application;

import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentStatus;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentType;
import io.github.lvdaxianer.doclens.j.processing.domain.ProcessingStage;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 文档处理轨道读模型组装器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
class ProcessingTrackAssembler {

    private static final int STEP_UPLOAD = 0;
    private static final int STEP_TYPE_RECOGNITION = 1;
    private static final int STEP_CONVERSION = 2;
    private static final int STEP_RENDERING = 3;
    private static final int STEP_OCR = 4;
    private static final int STEP_MERGE = 5;
    private static final int STEP_LLM_MARKDOWN = 6;
    private static final int STEP_SAVE = 7;
    private static final Map<DocumentType, ProcessingTrackProfile> TRACK_PROFILES =
            ProcessingTrackProfile.trackProfiles();
    private static final Map<ProcessingStage, ProcessingStage> NORMALIZED_STAGES = normalizedStages();
    private static final Map<ProcessingStage, Integer> STAGE_STEP_RANKS = stageStepRanks();

    /**
     * 组装文档八步处理轨道。
     *
     * @param document 文档任务
     * @return 八步处理轨道节点
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    List<Map<String, Object>> assemble(DocumentJob document) {
        ProcessingTrackProfile profile = profile(document.fileType());
        return List.of(
                node(document, ProcessingTrackStep.UPLOAD, true),
                node(document, ProcessingTrackStep.TYPE_RECOGNITION, true),
                node(document, ProcessingTrackStep.CONVERSION, profile.hasConversion()),
                node(document, ProcessingTrackStep.RENDERING, profile.hasPageRendering()),
                node(document, ProcessingTrackStep.OCR, profile.hasOcr()),
                node(document, ProcessingTrackStep.MERGE, profile.hasMerge()),
                node(document, ProcessingTrackStep.LLM_MARKDOWN, profile.hasMerge()),
                node(document, ProcessingTrackStep.SAVE, true)
        );
    }

    /**
     * 归一化历史处理阶段。
     *
     * @param stage 原始处理阶段
     * @return 当前读模型使用的处理阶段
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    static ProcessingStage normalizeStage(ProcessingStage stage) {
        return NORMALIZED_STAGES.getOrDefault(stage, stage);
    }

    /**
     * 判断阶段是否参与图片页进度统计。
     *
     * @param stage 处理阶段
     * @return true 表示阶段存在图片页进度
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    static boolean isImageProgressStage(ProcessingStage stage) {
        ProcessingStage normalizedStage = normalizeStage(stage);
        return normalizedStage == ProcessingStage.PDF_TO_IMAGES_COMPLETED
                || normalizedStage == ProcessingStage.OCR_IMAGES
                || normalizedStage == ProcessingStage.MERGE_TEXT
                || normalizedStage == ProcessingStage.SAVE_TEXT
                || normalizedStage == ProcessingStage.COMPLETED;
    }

    /**
     * 创建单个轨道节点。
     *
     * @param document 文档任务
     * @param step 轨道步骤
     * @param applicable 是否适用于当前文件类型
     * @return 轨道节点读模型
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private Map<String, Object> node(DocumentJob document, ProcessingTrackStep step, boolean applicable) {
        ProcessingTrackState state = nodeState(document, step.rank(), applicable);
        return Map.of("name", step.label(), "active", state.active(), "state", state.wireValue(),
                "description", state.description());
    }

    /**
     * 计算轨道节点状态。
     *
     * @param document 文档任务
     * @param stepRank 步骤序号
     * @param applicable 是否适用于当前文件类型
     * @return 轨道节点状态
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private ProcessingTrackState nodeState(DocumentJob document, int stepRank, boolean applicable) {
        ProcessingTrackState state;
        if (!applicable) {
            // 不适用当前文件类型的步骤展示为跳过。
            state = ProcessingTrackState.SKIPPED;
        } else {
            // 适用步骤继续按文档生命周期计算状态。
            state = applicableNodeState(document, stepRank);
        }
        return state;
    }

    /**
     * 计算适用步骤的节点状态。
     *
     * @param document 文档任务
     * @param stepRank 步骤序号
     * @return 适用步骤节点状态
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private ProcessingTrackState applicableNodeState(DocumentJob document, int stepRank) {
        Map<DocumentStatus, TrackStateResolver> resolvers = statusResolvers();
        return resolvers.getOrDefault(document.status(), this::processingNodeState).resolve(document, stepRank);
    }

    /**
     * 计算失败任务的节点状态。
     *
     * @param document 文档任务
     * @param stepRank 步骤序号
     * @return 失败任务节点状态
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private ProcessingTrackState failedNodeState(DocumentJob document, int stepRank) {
        int failedRank = failedStepRank(document);
        return rankedState(stepRank, failedRank, ProcessingTrackState.FAILED);
    }

    /**
     * 计算失败任务发生在哪个步骤。
     *
     * @param document 文档任务
     * @return 失败步骤序号
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private int failedStepRank(DocumentJob document) {
        int rank;
        if (isLegacyFailedStage(document.stage())) {
            // 历史失败阶段缺少失败前阶段，只能按页进度兼容推断。
            rank = legacyFailedStepRank(document);
        } else {
            // 新失败任务会保留失败前阶段，可直接定位失败步骤。
            rank = stepRankForStage(normalizeStage(document.stage()));
        }
        return rank;
    }

    /**
     * 兼容历史失败阶段的步骤推断。
     *
     * @param document 文档任务
     * @return 推断出的失败步骤序号
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private int legacyFailedStepRank(DocumentJob document) {
        ProcessingTrackProfile profile = profile(document.fileType());
        boolean hasUnfinishedOcr = profile.hasOcr() && document.currentPage() < document.totalPages();
        return hasUnfinishedOcr ? STEP_OCR : STEP_SAVE;
    }

    /**
     * 计算处理中任务的节点状态。
     *
     * @param document 文档任务
     * @param stepRank 步骤序号
     * @return 处理中任务节点状态
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private ProcessingTrackState processingNodeState(DocumentJob document, int stepRank) {
        int currentRank = stepRankForStage(normalizeStage(document.stage()));
        return rankedState(stepRank, currentRank, currentState(document, stepRank));
    }

    /**
     * 计算当前步骤的展示状态。
     *
     * @param document 文档任务
     * @param stepRank 步骤序号
     * @return 当前步骤展示状态
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private ProcessingTrackState currentState(DocumentJob document, int stepRank) {
        boolean savedDirectText = normalizeStage(document.stage()) == ProcessingStage.DIRECT_TEXT_SAVED
                && stepRank == STEP_SAVE;
        return savedDirectText ? ProcessingTrackState.DONE : ProcessingTrackState.CURRENT;
    }

    /**
     * 按步骤序号计算完成、当前和待处理状态。
     *
     * @param stepRank 节点步骤序号
     * @param currentRank 当前步骤序号
     * @param currentState 当前步骤状态
     * @return 节点状态
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private ProcessingTrackState rankedState(int stepRank, int currentRank, ProcessingTrackState currentState) {
        ProcessingTrackState state;
        if (stepRank < currentRank) {
            // 当前步骤之前的节点均视为已完成。
            state = ProcessingTrackState.DONE;
        } else if (stepRank == currentRank) {
            // 当前步骤使用调用方指定的处理中或失败状态。
            state = currentState;
        } else {
            // 当前步骤之后的节点仍未开始。
            state = ProcessingTrackState.PENDING;
        }
        return state;
    }

    /**
     * 读取文件类型对应的轨道配置。
     *
     * @param fileType 文件类型
     * @return 轨道配置
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private ProcessingTrackProfile profile(DocumentType fileType) {
        return TRACK_PROFILES.getOrDefault(fileType, ProcessingTrackProfile.textLike());
    }

    /**
     * 判断是否为历史失败阶段。
     *
     * @param stage 处理阶段
     * @return true 表示需要使用兼容推断
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private boolean isLegacyFailedStage(ProcessingStage stage) {
        return stage == ProcessingStage.FAILED || stage == ProcessingStage.OCR_FAILED;
    }

    /**
     * 获取阶段对应步骤序号。
     *
     * @param stage 处理阶段
     * @return 步骤序号
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private int stepRankForStage(ProcessingStage stage) {
        return STAGE_STEP_RANKS.getOrDefault(stage, STEP_TYPE_RECOGNITION);
    }

    /**
     * 创建状态解析器映射。
     *
     * @return 状态解析器映射
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private Map<DocumentStatus, TrackStateResolver> statusResolvers() {
        EnumMap<DocumentStatus, TrackStateResolver> resolvers = new EnumMap<>(DocumentStatus.class);
        resolvers.put(DocumentStatus.COMPLETED, (document, stepRank) -> ProcessingTrackState.DONE);
        resolvers.put(DocumentStatus.FAILED, this::failedNodeState);
        return resolvers;
    }

    /**
     * 创建历史阶段归一化映射。
     *
     * @return 历史阶段归一化映射
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static Map<ProcessingStage, ProcessingStage> normalizedStages() {
        EnumMap<ProcessingStage, ProcessingStage> stages = new EnumMap<>(ProcessingStage.class);
        stages.put(ProcessingStage.OCR_PROCESSING, ProcessingStage.OCR_IMAGES);
        stages.put(ProcessingStage.OCR_COMPLETED, ProcessingStage.COMPLETED);
        stages.put(ProcessingStage.OCR_FAILED, ProcessingStage.FAILED);
        stages.put(ProcessingStage.RENDERING, ProcessingStage.PDF_TO_IMAGES);
        stages.put(ProcessingStage.NORMALIZING, ProcessingStage.MERGE_TEXT);
        return Map.copyOf(stages);
    }

    /**
     * 创建处理阶段到轨道步骤的映射。
     *
     * @return 阶段步骤映射
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static Map<ProcessingStage, Integer> stageStepRanks() {
        EnumMap<ProcessingStage, Integer> ranks = new EnumMap<>(ProcessingStage.class);
        ranks.put(ProcessingStage.WORD_TO_PDF, STEP_CONVERSION);
        ranks.put(ProcessingStage.WORD_TO_PDF_COMPLETED, STEP_RENDERING);
        ranks.put(ProcessingStage.PDF_TO_IMAGES, STEP_RENDERING);
        ranks.put(ProcessingStage.PDF_TO_IMAGES_COMPLETED, STEP_OCR);
        ranks.put(ProcessingStage.OCR_IMAGES, STEP_OCR);
        ranks.put(ProcessingStage.MERGE_TEXT, STEP_MERGE);
        ranks.put(ProcessingStage.DIRECT_TEXT_SAVED, STEP_SAVE);
        ranks.put(ProcessingStage.SAVE_TEXT, STEP_LLM_MARKDOWN);
        ranks.put(ProcessingStage.COMPLETED, STEP_SAVE);
        return Map.copyOf(ranks);
    }


    /**
     * 文档状态轨道节点解析器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @FunctionalInterface
    private interface TrackStateResolver {

        /**
         * 解析文档在指定步骤的轨道状态。
         *
         * @param document 文档任务
         * @param stepRank 步骤序号
         * @return 轨道节点状态
         * @author lvdaxianerplus
         * @date 2026-06-08
         */
        ProcessingTrackState resolve(DocumentJob document, int stepRank);
    }
}
