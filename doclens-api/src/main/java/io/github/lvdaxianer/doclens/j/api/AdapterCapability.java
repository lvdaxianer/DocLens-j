package io.github.lvdaxianer.doclens.j.api;

import java.util.List;

/**
 * 公开 OCR 适配器能力描述。
 *
 * @param adapterKey 适配器键
 * @param supportedFileTypes 支持的文件类型
 * @param supportsDirectPdf 是否支持 PDF 直传 OCR
 * @param supportsAsyncPolling 是否支持厂商异步轮询
 * @param supportsImageOcr 是否支持图片 OCR
 * @param supportsTableStructure 是否支持表格抽取
 * @param supportsCoordinates 是否支持坐标
 * @param maxDirectPdfSize 最大直传 PDF 大小
 * @param maxDirectPdfPages 最大直传 PDF 页数
 * @param maxFileSizeMb 最大文件大小，单位 MB
 * @param maxPageCount 最大页数
 * @param notes 能力备注
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public record AdapterCapability(
        String adapterKey,
        List<String> supportedFileTypes,
        boolean supportsDirectPdf,
        boolean supportsAsyncPolling,
        boolean supportsImageOcr,
        boolean supportsTableStructure,
        boolean supportsCoordinates,
        Integer maxDirectPdfSize,
        Integer maxDirectPdfPages,
        Integer maxFileSizeMb,
        Integer maxPageCount,
        String notes
) {
}
