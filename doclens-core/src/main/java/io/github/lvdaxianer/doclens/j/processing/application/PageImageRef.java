package io.github.lvdaxianer.doclens.j.processing.application;

/**
 * 文档页图片引用。
 *
 * @param pageNo 文档内页码
 * @param imageStorageUri 页图片存储地址
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public record PageImageRef(
        int pageNo,
        String imageStorageUri
) {
}
