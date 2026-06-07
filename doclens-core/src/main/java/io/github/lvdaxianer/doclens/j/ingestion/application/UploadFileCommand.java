package io.github.lvdaxianer.doclens.j.ingestion.application;

/**
 * Uploaded file command.
 *
 * @param fileName uploaded file name
 * @param content uploaded bytes
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public record UploadFileCommand(String fileName, byte[] content) {
}
