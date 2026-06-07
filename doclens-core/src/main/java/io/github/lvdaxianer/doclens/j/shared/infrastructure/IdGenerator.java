package io.github.lvdaxianer.doclens.j.shared.infrastructure;

import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * 领域标识生成器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public class IdGenerator {

    private static final int RANDOM_BYTES = 12;
    private final SecureRandom random = new SecureRandom();

    /**
     * 创建批次标识。
     *
     * @return 批次 ID
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public String newBatchId() {
        return prefixedId("batch");
    }

    /**
     * 创建文档标识。
     *
     * @return 文档 ID
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public String newDocumentId() {
        return prefixedId("doc");
    }

    /**
     * 创建结果标识。
     *
     * @return 结果 ID
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public String newResultId() {
        return prefixedId("result");
    }

    /**
     * 创建事件标识。
     *
     * @return 事件 ID
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public String newEventId() {
        return prefixedId("evt");
    }

    private String prefixedId(String prefix) {
        byte[] bytes = new byte[RANDOM_BYTES];
        random.nextBytes(bytes);
        return prefix + "_" + HexFormat.of().formatHex(bytes);
    }
}
