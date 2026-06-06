package com.doclens.shared.infrastructure;

import java.security.SecureRandom;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/**
 * Domain identifier generator.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@Component
public class IdGenerator {

    private static final int RANDOM_BYTES = 12;
    private final SecureRandom random = new SecureRandom();

    /**
     * Creates a batch identifier.
     *
     * @return batch id
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public String newBatchId() {
        return prefixedId("batch");
    }

    /**
     * Creates a document identifier.
     *
     * @return document id
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public String newDocumentId() {
        return prefixedId("doc");
    }

    /**
     * Creates a result identifier.
     *
     * @return result id
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public String newResultId() {
        return prefixedId("result");
    }

    /**
     * Creates an event identifier.
     *
     * @return event id
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
