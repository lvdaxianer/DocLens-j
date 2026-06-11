package io.github.lvdaxianer.doclens.j.storage;

import java.util.List;

/**
 * 上传文件和生成产物的对象存储抽象。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public interface ObjectStorage {

    /**
     * 按相对对象键写入字节。
     *
     * @param objectKey 对象键
     * @param content 待存储字节
     * @return 存储 URI
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    String writeBytes(String objectKey, byte[] content);

    /**
     * 从存储 URI 读取字节。
     *
     * @param storageUri 存储 URI
     * @return 已存储字节
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    byte[] readBytes(String storageUri);

    /**
     * 删除存储 URI 对应对象。
     *
     * @param storageUri 存储 URI
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    default void delete(String storageUri) {
        throw new UnsupportedOperationException("object delete is not supported");
    }

    /**
     * 批量删除存储 URI 对应对象。
     *
     * @param storageUris 存储 URI 集合
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    default void deleteAll(List<String> storageUris) {
        storageUris.forEach(this::delete);
    }
}
