package io.github.lvdaxianer.doclens.j.storage;

import io.github.lvdaxianer.doclens.j.shared.config.DocLensProperties;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

/**
 * Local filesystem implementation of object storage.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@Component
public class LocalObjectStorage implements ObjectStorage {

    private final Path storageRoot;

    /**
     * Creates local object storage.
     *
     * @param properties DocLens runtime properties
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public LocalObjectStorage(DocLensProperties properties) {
        this.storageRoot = Path.of(properties.storageRoot()).toAbsolutePath().normalize();
    }

    @Override
    public String writeBytes(String objectKey, byte[] content) {
        try {
            Path target = resolveObjectKey(objectKey);
            Files.createDirectories(target.getParent());
            Files.write(target, content);
            return "local://" + objectKey;
        } catch (IOException ex) {
            throw new IllegalStateException("failed to write object storage bytes", ex);
        }
    }

    @Override
    public byte[] readBytes(String storageUri) {
        try {
            return Files.readAllBytes(resolveUri(storageUri));
        } catch (IOException ex) {
            throw new IllegalStateException("failed to read object storage bytes", ex);
        }
    }

    private Path resolveObjectKey(String objectKey) {
        Path resolved = storageRoot.resolve(objectKey).normalize();
        if (resolved.startsWith(storageRoot)) {
            return resolved;
        } else {
            throw new IllegalArgumentException("invalid object key");
        }
    }

    private Path resolveUri(String storageUri) {
        URI uri = URI.create(storageUri);
        String objectKey = uri.getHost() == null ? uri.getPath() : uri.getHost() + uri.getPath();
        return resolveObjectKey(objectKey.startsWith("/") ? objectKey.substring(1) : objectKey);
    }
}
