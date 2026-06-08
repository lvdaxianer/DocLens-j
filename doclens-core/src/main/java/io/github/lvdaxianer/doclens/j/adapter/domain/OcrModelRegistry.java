package io.github.lvdaxianer.doclens.j.adapter.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 已支持 OCR 模型注册表。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public class OcrModelRegistry {

    private final Map<String, OcrModelDefinition> definitions;

    /**
     * 创建 OCR 模型注册表。
     *
     * @param definitions OCR 模型定义集合
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public OcrModelRegistry(List<OcrModelDefinition> definitions) {
        this.definitions = definitions.stream()
                .collect(Collectors.toMap(OcrModelDefinition::modelKey, Function.identity(),
                        OcrModelRegistry::rejectDuplicate, LinkedHashMap::new));
    }

    /**
     * 查询所有已支持 OCR 模型。
     *
     * @return 已支持 OCR 模型列表
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public List<OcrModelDefinition> list() {
        return List.copyOf(definitions.values());
    }

    /**
     * 根据模型标识查询 OCR 模型。
     *
     * @param modelKey OCR 模型标识
     * @return OCR 模型定义
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public Optional<OcrModelDefinition> find(String modelKey) {
        return Optional.ofNullable(definitions.get(modelKey));
    }

    /**
     * 校验 OCR 模型是否已支持。
     *
     * @param modelKey OCR 模型标识
     * @return OCR 模型定义
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public OcrModelDefinition requireSupported(String modelKey) {
        return find(modelKey).orElseThrow(() -> new IllegalArgumentException("unsupported ocr model key"));
    }

    /**
     * 拒绝重复模型定义。
     *
     * @param left 已存在模型定义
     * @param right 新模型定义
     * @return 不会返回
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static OcrModelDefinition rejectDuplicate(OcrModelDefinition left, OcrModelDefinition right) {
        throw new IllegalArgumentException("duplicate ocr model key");
    }
}
