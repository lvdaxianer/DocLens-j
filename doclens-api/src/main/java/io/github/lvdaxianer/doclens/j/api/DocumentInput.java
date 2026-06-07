package io.github.lvdaxianer.doclens.j.api;

/**
 * 嵌入式 SDK 文档输入。
 *
 * @param fileName 原始文件名
 * @param content 文档字节
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public record DocumentInput(String fileName, byte[] content) {
}
