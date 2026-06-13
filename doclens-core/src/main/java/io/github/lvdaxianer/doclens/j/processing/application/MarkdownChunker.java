package io.github.lvdaxianer.doclens.j.processing.application;

import java.util.ArrayList;
import java.util.List;

/**
 * Markdown 文本分片器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-13
 */
public final class MarkdownChunker {

    private static final double CONTENT_BUDGET_RATIO = 0.8D;
    private static final double OVERLAP_RATIO = 0.1D;
    private static final int MIN_OVERLAP_TOKENS = 1;
    private static final String[] BOUNDARIES = {"\n\f\n", "\f", "\n---\n", "\n# ", "\n## ", "\n\n", "\n"};

    private final TokenEstimator tokenEstimator;

    /**
     * 创建 Markdown 文本分片器。
     *
     * @param tokenEstimator Token 估算器
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    public MarkdownChunker(TokenEstimator tokenEstimator) {
        this.tokenEstimator = tokenEstimator;
    }

    /**
     * 生成 Markdown 分片计划。
     *
     * @param text OCR 合并文本
     * @param maxContextTokens LLM 最大上下文 Token 数
     * @return 分片计划
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    public MarkdownChunkPlan plan(String text, int maxContextTokens) {
        String safeText = text == null ? "" : text;
        int contentBudgetTokens = contentBudgetTokens(maxContextTokens);
        int estimatedInputTokens = tokenEstimator.estimate(safeText);
        int overlapTokens = Math.max(MIN_OVERLAP_TOKENS, (int) Math.floor(contentBudgetTokens * OVERLAP_RATIO));
        ChunkPlanContext context = new ChunkPlanContext(safeText, estimatedInputTokens, contentBudgetTokens,
                overlapTokens);
        if (estimatedInputTokens <= contentBudgetTokens) {
            // 输入在预算内时不拆分，避免破坏原文结构。
            return singleChunkPlan(context);
        } else {
            // 输入超过预算时生成有序分片，重叠上下文不进入主内容。
            return chunkedPlan(context);
        }
    }

    /**
     * 创建单分片计划。
     *
     * @param context 分片计划上下文
     * @return 单分片计划
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private MarkdownChunkPlan singleChunkPlan(ChunkPlanContext context) {
        MarkdownChunk chunk = new MarkdownChunk(0, 1, "", context.text(), "", context.estimatedInputTokens());
        return new MarkdownChunkPlan(false, context.estimatedInputTokens(), context.contentBudgetTokens(),
                context.overlapTokens(), List.of(chunk));
    }

    /**
     * 创建多分片计划。
     *
     * @param context 分片计划上下文
     * @return 多分片计划
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private MarkdownChunkPlan chunkedPlan(ChunkPlanContext context) {
        List<Range> ranges = mainRanges(context);
        List<MarkdownChunk> chunks = chunksForRanges(new RangeBuildContext(context.text(), ranges,
                context.overlapTokens()));
        return new MarkdownChunkPlan(true, context.estimatedInputTokens(), context.contentBudgetTokens(),
                context.overlapTokens(), chunks);
    }

    /**
     * 计算主内容范围列表。
     *
     * @param context 分片计划上下文
     * @return 主内容范围列表
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private List<Range> mainRanges(ChunkPlanContext context) {
        int targetChars = targetChars(context);
        List<Range> ranges = new ArrayList<>((context.text().length() / targetChars) + 1);
        int start = 0;
        while (start < context.text().length()) {
            int hardEnd = Math.min(context.text().length(), start + targetChars);
            int end = hardEnd == context.text().length() ? hardEnd : preferredBoundary(context.text(), start, hardEnd);
            ranges.add(new Range(start, end));
            start = end;
        }
        return ranges;
    }

    /**
     * 按范围创建分片。
     *
     * @param context 范围构建上下文
     * @return 有序分片列表
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private List<MarkdownChunk> chunksForRanges(RangeBuildContext context) {
        List<MarkdownChunk> chunks = new ArrayList<>(context.ranges().size());
        for (int index = 0; index < context.ranges().size(); index++) {
            ChunkBuildContext chunkContext = new ChunkBuildContext(context.text(), context.ranges().get(index),
                    index, context.ranges().size(), context.overlapTokens());
            chunks.add(chunkForRange(chunkContext));
        }
        return chunks;
    }

    /**
     * 创建单个范围对应的分片。
     *
     * @param context 分片构建上下文
     * @return Markdown 分片
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private MarkdownChunk chunkForRange(ChunkBuildContext context) {
        String mainContent = context.text().substring(context.range().start(), context.range().end());
        String previousContext = previousContext(context.text(), context.range().start(), context.overlapTokens());
        String nextContext = nextContext(context.text(), context.range().end(), context.overlapTokens());
        return new MarkdownChunk(context.index(), context.total(), previousContext, mainContent, nextContext,
                tokenEstimator.estimate(mainContent));
    }

    /**
     * 计算内容预算 Token 数。
     *
     * @param maxContextTokens 最大上下文 Token 数
     * @return 主内容预算 Token 数
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private int contentBudgetTokens(int maxContextTokens) {
        if (maxContextTokens <= 0) {
            // 最大上下文必须为正数，否则无法安全分片。
            throw new IllegalArgumentException("llm markdown max context tokens must be greater than 0");
        } else {
            // 仅使用 80% 上下文作为主内容预算，预留提示词和响应空间。
            return Math.max(1, (int) Math.floor(maxContextTokens * CONTENT_BUDGET_RATIO));
        }
    }

    /**
     * 计算目标字符数。
     *
     * @param context 分片计划上下文
     * @return 目标字符数
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private int targetChars(ChunkPlanContext context) {
        double ratio = (double) context.contentBudgetTokens() / context.estimatedInputTokens();
        return Math.max(1, (int) Math.floor(context.text().length() * ratio));
    }

    /**
     * 查找优先切分边界。
     *
     * @param text 原始文本
     * @param start 起始位置
     * @param hardEnd 硬切结束位置
     * @return 切分结束位置
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private int preferredBoundary(String text, int start, int hardEnd) {
        int minimumEnd = start + Math.max(1, (hardEnd - start) / 2);
        for (String boundary : BOUNDARIES) {
            int boundaryEnd = boundaryEnd(new BoundarySearch(text, boundary, minimumEnd, hardEnd));
            if (boundaryEnd > minimumEnd) {
                // 命中优先边界时使用自然文档结构切分。
                return boundaryEnd;
            } else {
                // 当前边界类型未命中，继续尝试下一个优先级。
            }
        }
        return hardEnd;
    }

    /**
     * 查找指定边界的结束位置。
     *
     * @param search 边界搜索参数
     * @return 边界结束位置，未命中返回 -1
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private int boundaryEnd(BoundarySearch search) {
        int index = search.text().lastIndexOf(search.boundary(), search.hardEnd());
        if (index >= search.minimumEnd()) {
            // 标题边界保留标题到下一个分片开头，其余边界随上一分片结束。
            return search.boundary().startsWith("\n#") ? index + 1 : index + search.boundary().length();
        } else {
            // 未找到满足最小长度的边界时回退硬切。
            return -1;
        }
    }

    /**
     * 获取上文重叠内容。
     *
     * @param text 原始文本
     * @param start 主内容起始位置
     * @param overlapTokens 重叠 Token 预算
     * @return 上文内容
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private String previousContext(String text, int start, int overlapTokens) {
        int contextStart = boundedContextStart(new ContextWindow(text, start, overlapTokens));
        return text.substring(contextStart, start);
    }

    /**
     * 获取下文重叠内容。
     *
     * @param text 原始文本
     * @param end 主内容结束位置
     * @param overlapTokens 重叠 Token 预算
     * @return 下文内容
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private String nextContext(String text, int end, int overlapTokens) {
        int contextEnd = boundedContextEnd(new ContextWindow(text, end, overlapTokens));
        return text.substring(end, contextEnd);
    }

    /**
     * 计算上文开始位置。
     *
     * @param window 上下文窗口
     * @return 上文开始位置
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private int boundedContextStart(ContextWindow window) {
        int start = Math.max(0, window.anchor() - window.overlapTokens());
        while (needsMorePreviousContext(window, start)) {
            start--;
        }
        return start;
    }

    /**
     * 计算下文结束位置。
     *
     * @param window 上下文窗口
     * @return 下文结束位置
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private int boundedContextEnd(ContextWindow window) {
        int end = Math.min(window.text().length(), window.anchor() + window.overlapTokens());
        while (needsMoreNextContext(window, end)) {
            end++;
        }
        return end;
    }

    /**
     * 判断是否需要继续扩大上文窗口。
     *
     * @param window 上下文窗口
     * @param start 当前开始位置
     * @return 是否继续扩大
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private boolean needsMorePreviousContext(ContextWindow window, int start) {
        return start > 0 && tokenEstimator.estimate(window.text().substring(start, window.anchor()))
                < window.overlapTokens();
    }

    /**
     * 判断是否需要继续扩大下文窗口。
     *
     * @param window 上下文窗口
     * @param end 当前结束位置
     * @return 是否继续扩大
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private boolean needsMoreNextContext(ContextWindow window, int end) {
        return end < window.text().length() && tokenEstimator.estimate(window.text().substring(window.anchor(), end))
                < window.overlapTokens();
    }
}
