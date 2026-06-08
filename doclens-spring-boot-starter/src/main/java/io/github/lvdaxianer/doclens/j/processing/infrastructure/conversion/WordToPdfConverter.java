package io.github.lvdaxianer.doclens.j.processing.infrastructure.conversion;

/**
 * Word 转 PDF 转换端口。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public interface WordToPdfConverter {

    /**
     * 将 Word 字节转换为 PDF 字节。
     *
     * @param fileName 原始文件名
     * @param content Word 字节
     * @return PDF 字节
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    byte[] convert(String fileName, byte[] content);
}
