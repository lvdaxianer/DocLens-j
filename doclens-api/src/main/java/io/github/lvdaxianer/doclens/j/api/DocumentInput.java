package io.github.lvdaxianer.doclens.j.api;

/**
 * Embedded SDK document input.
 *
 * @param fileName original file name
 * @param content document bytes
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public record DocumentInput(String fileName, byte[] content) {
}
