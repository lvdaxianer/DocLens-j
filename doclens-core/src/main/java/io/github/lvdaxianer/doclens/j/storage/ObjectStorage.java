package io.github.lvdaxianer.doclens.j.storage;

/**
 * Object storage abstraction for uploaded files and generated artifacts.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public interface ObjectStorage {

    /**
     * Writes bytes under a relative object key.
     *
     * @param objectKey object key
     * @param content bytes to store
     * @return storage URI
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    String writeBytes(String objectKey, byte[] content);

    /**
     * Reads bytes from a storage URI.
     *
     * @param storageUri storage URI
     * @return stored bytes
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    byte[] readBytes(String storageUri);
}
