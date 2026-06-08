package io.github.lvdaxianer.doclens.j.query.application;

import io.github.lvdaxianer.doclens.j.processing.domain.DocumentType;
import java.util.EnumMap;
import java.util.Map;

/**
 * Dashboard 文件类型处理轨道配置。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
record ProcessingTrackProfile(
        boolean hasConversion,
        boolean hasPageRendering,
        boolean hasOcr,
        boolean hasMerge
) {

    /**
     * 创建文件类型轨道配置。
     *
     * @return 文件类型轨道配置
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    static Map<DocumentType, ProcessingTrackProfile> trackProfiles() {
        EnumMap<DocumentType, ProcessingTrackProfile> profiles = new EnumMap<>(DocumentType.class);
        profiles.put(DocumentType.MARKDOWN, textLike());
        profiles.put(DocumentType.TEXT, textLike());
        profiles.put(DocumentType.IMAGE, new ProcessingTrackProfile(false, false, true, true));
        profiles.put(DocumentType.PDF, new ProcessingTrackProfile(false, true, true, true));
        profiles.put(DocumentType.WORD, new ProcessingTrackProfile(true, true, true, true));
        return Map.copyOf(profiles);
    }

    /**
     * 创建文本直通类文件轨道配置。
     *
     * @return 文本直通类轨道配置
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    static ProcessingTrackProfile textLike() {
        return new ProcessingTrackProfile(false, false, false, false);
    }
}
