package io.github.lvdaxianer.doclens.j.ingestion.application;

/**
 * 上传文件命令。
 *
 * @param fileName 上传文件名
 * @param content 上传字节
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public record UploadFileCommand(String fileName, byte[] content) {
}
