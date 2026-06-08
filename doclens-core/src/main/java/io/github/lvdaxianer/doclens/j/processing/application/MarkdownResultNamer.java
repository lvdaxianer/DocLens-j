package io.github.lvdaxianer.doclens.j.processing.application;

import java.util.UUID;

/**
 * 最终 Markdown 结果文件命名器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public final class MarkdownResultNamer {

    private MarkdownResultNamer() {
    }

    /**
     * 生成最终纯文本 Markdown 文件名。
     *
     * @param originalFileName 原始文件名
     * @param uuid 唯一标识
     * @return Markdown 文件名
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public static String markdownFileName(String originalFileName, UUID uuid) {
        String baseName = originalFileName == null || originalFileName.isBlank() ? "document" : originalFileName;
        int extensionIndex = baseName.lastIndexOf('.');
        if (extensionIndex > 0) {
            baseName = baseName.substring(0, extensionIndex);
        } else {
            // 无扩展名时直接使用完整文件名。
        }
        String safeName = baseName.replaceAll("[/\\\\\\p{Cntrl}]", "_");
        return safeName + "_" + uuid + ".md";
    }
}
