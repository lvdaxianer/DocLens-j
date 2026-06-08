package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrResult;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrBlock;
import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * PaddleOCR 原生响应到归一化结果的映射器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public class PaddleOcrNativeResponseMapper {

    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    /**
     * 追加 OCR 块所需的上下文。
     *
     * @param blocks OCR 块集合
     * @param scores 置信度数组
     * @param boxes 文本框数组
     * @param polys 多边形数组
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private record BlockAppendContext(
            List<OcrBlock> blocks,
            JsonNode scores,
            JsonNode boxes,
            JsonNode polys
    ) {
    }

    /**
     * 创建响应映射器。
     *
     * @param objectMapper JSON 映射器
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public PaddleOcrNativeResponseMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 映射 PaddleOCR 响应。
     *
     * @param pageNo 页码
     * @param response 响应 JSON
     * @return 图片 OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public ImageOcrResult map(int pageNo, JsonNode response) {
        int errorCode = response.path("errorCode").asInt(-1);
        if (errorCode != 0) {
            throw new IllegalStateException("PaddleOCR native API failed: "
                    + response.path("errorMsg").asText("unknown error"));
        } else {
            List<OcrBlock> blocks = blocks(pageNo, response);
            List<String> warnings = blocks.isEmpty() ? List.of("PaddleOCR returned no OCR results") : List.of();
            return ImageOcrResult.fromBlocks(pageNo, objectMapper.convertValue(response, OBJECT_MAP), blocks, warnings);
        }
    }

    /**
     * 从响应中提取 OCR 块列表。
     *
     * @param pageNo 页码
     * @param response PaddleOCR 响应
     * @return OCR 块列表
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private List<OcrBlock> blocks(int pageNo, JsonNode response) {
        JsonNode ocrResults = response.path("result").path("ocrResults");
        List<OcrBlock> blocks = new ArrayList<>(Math.max(ocrResults.size(), 0));
        if (!ocrResults.isArray()) {
            return blocks;
        } else {
            for (JsonNode ocrResult : ocrResults) {
                appendBlocks(pageNo, blocks, ocrResult.path("prunedResult"));
            }
            return blocks;
        }
    }

    /**
     * 从单个 OCR 结果追加识别文本块。
     *
     * @param pageNo 页码
     * @param blocks OCR 块集合
     * @param prunedResult PaddleOCR 裁剪结果
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private void appendBlocks(int pageNo, List<OcrBlock> blocks, JsonNode prunedResult) {
        JsonNode texts = prunedResult.path("rec_texts");
        JsonNode scores = prunedResult.path("rec_scores");
        JsonNode boxes = prunedResult.path("rec_boxes");
        JsonNode polys = prunedResult.path("rec_polys");
        if (!texts.isArray()) {
            return;
        } else {
            BlockAppendContext context = new BlockAppendContext(blocks, scores, boxes, polys);
            for (int index = 0; index < texts.size(); index++) {
                appendBlock(pageNo, context, textAt(texts, index), index);
            }
        }
    }

    /**
     * 追加单个非空文本块。
     *
     * @param pageNo 页码
     * @param context 块追加上下文
     * @param text 识别文本
     * @param index 文本索引
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private void appendBlock(int pageNo, BlockAppendContext context, String text, int index) {
        if (text.isBlank()) {
            // 空文本不进入归一化结果。
        } else {
            context.blocks().add(new OcrBlock(pageNo, text, scoreAt(context.scores(), index),
                    intList(context.boxes(), index), polygon(context.polys(), index), DocLensConstants.DEFAULT_ADAPTER_KEY));
        }
    }

    /**
     * 读取指定索引文本。
     *
     * @param texts 文本数组
     * @param index 文本索引
     * @return 文本内容
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private String textAt(JsonNode texts, int index) {
        return texts.get(index).asText("");
    }

    /**
     * 读取指定索引置信度。
     *
     * @param scores 置信度数组
     * @param index 文本索引
     * @return 置信度
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private double scoreAt(JsonNode scores, int index) {
        if (scores.isArray() && scores.size() > index) {
            return scores.get(index).asDouble(0D);
        } else {
            return 0D;
        }
    }

    /**
     * 读取指定索引的整数数组。
     *
     * @param array 外层 JSON 数组
     * @param index 元素索引
     * @return 整数数组
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private List<Integer> intList(JsonNode array, int index) {
        if (!array.isArray() || array.size() <= index || !array.get(index).isArray()) {
            return List.of();
        } else {
            List<Integer> values = new ArrayList<>(array.get(index).size());
            array.get(index).forEach(value -> values.add(value.asInt()));
            return values;
        }
    }

    /**
     * 读取指定索引的多边形坐标。
     *
     * @param array 外层 JSON 数组
     * @param index 元素索引
     * @return 多边形坐标
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private List<List<Integer>> polygon(JsonNode array, int index) {
        if (!array.isArray() || array.size() <= index || !array.get(index).isArray()) {
            return List.of();
        } else {
            List<List<Integer>> points = new ArrayList<>(array.get(index).size());
            array.get(index).forEach(point -> {
                if (point.isArray()) {
                    List<Integer> values = new ArrayList<>(point.size());
                    point.forEach(value -> values.add(value.asInt()));
                    points.add(values);
                } else {
                    // 非数组坐标点无法构成多边形，保持忽略。
                }
            });
            return points;
        }
    }
}
