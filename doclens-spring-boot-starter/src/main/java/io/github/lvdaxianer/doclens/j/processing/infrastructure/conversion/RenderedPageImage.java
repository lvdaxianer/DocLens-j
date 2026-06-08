package io.github.lvdaxianer.doclens.j.processing.infrastructure.conversion;

/**
 * PDF 页渲染后的图片。
 *
 * @param pageNo 页码
 * @param content 图片字节
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public record RenderedPageImage(int pageNo, byte[] content) {
}
