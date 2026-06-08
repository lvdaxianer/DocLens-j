package io.github.lvdaxianer.doclens.j.shared.infrastructure;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrCallIdGenerator;

/**
 * 基于通用 ID 生成器的 OCR 调用记录 ID 生成器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public class IdGeneratorOcrCallIdGenerator implements OcrCallIdGenerator {

    private final IdGenerator idGenerator;

    /**
     * 创建 OCR 调用记录 ID 生成器。
     *
     * @param idGenerator 通用 ID 生成器
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public IdGeneratorOcrCallIdGenerator(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    @Override
    public String newOcrCallId() {
        return idGenerator.newOcrCallId();
    }
}
