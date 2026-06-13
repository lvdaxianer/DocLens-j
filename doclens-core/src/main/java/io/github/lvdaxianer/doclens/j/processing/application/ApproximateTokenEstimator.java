package io.github.lvdaxianer.doclens.j.processing.application;

/**
 * 保守近似 Token 估算器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-13
 */
public final class ApproximateTokenEstimator implements TokenEstimator {

    private static final int NON_CJK_CHARS_PER_TOKEN = 4;

    /**
     * 估算文本 Token 数。
     *
     * @param text 待估算文本
     * @return 估算 Token 数
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    @Override
    public int estimate(String text) {
        String normalized = text == null ? "" : text.strip();
        if (normalized.isBlank()) {
            // 空白文本没有可发送内容。
            return 0;
        } else {
            // CJK 按字符计数，非 CJK 按常见 4 字符约 1 Token 保守估算。
            TokenCounts counts = countCharacters(normalized);
            return counts.cjkCount() + ceilDiv(counts.nonCjkCount(), NON_CJK_CHARS_PER_TOKEN)
                    + counts.lineBreakCount();
        }
    }

    /**
     * 统计不同文本类型字符数。
     *
     * @param text 标准化文本
     * @return 字符统计结果
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private TokenCounts countCharacters(String text) {
        int cjkCount = 0;
        int nonCjkCount = 0;
        int lineBreakCount = 0;
        for (int index = 0; index < text.length(); index++) {
            char value = text.charAt(index);
            if (value == '\n') {
                // 换行会影响 Markdown 结构，额外计入安全余量。
                lineBreakCount++;
            } else if (!Character.isWhitespace(value) && isCjk(value)) {
                // 中文、日文、韩文按单字符 Token 估算。
                cjkCount++;
            } else if (!Character.isWhitespace(value)) {
                // 非空白拉丁字符和标点合并估算。
                nonCjkCount++;
            }
        }
        return new TokenCounts(cjkCount, nonCjkCount, lineBreakCount);
    }

    /**
     * 判断字符是否属于常见 CJK 区间。
     *
     * @param value 字符
     * @return 是否为 CJK 字符
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private boolean isCjk(char value) {
        Character.UnicodeScript script = Character.UnicodeScript.of(value);
        return script == Character.UnicodeScript.HAN
                || script == Character.UnicodeScript.HIRAGANA
                || script == Character.UnicodeScript.KATAKANA
                || script == Character.UnicodeScript.HANGUL;
    }

    /**
     * 向上整除。
     *
     * @param value 被除数
     * @param divisor 除数
     * @return 向上整除结果
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private int ceilDiv(int value, int divisor) {
        return value == 0 ? 0 : ((value - 1) / divisor) + 1;
    }

    /**
     * Token 字符统计结果。
     *
     * @param cjkCount CJK 字符数
     * @param nonCjkCount 非 CJK 非空白字符数
     * @param lineBreakCount 换行数
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private record TokenCounts(int cjkCount, int nonCjkCount, int lineBreakCount) {
    }
}
