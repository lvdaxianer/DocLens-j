package io.github.lvdaxianer.doclens.j.api;

import java.util.List;

/**
 * Public OCR adapter capability description.
 *
 * @param adapterKey adapter key
 * @param supportedFileTypes supported file types
 * @param supportsDirectPdf whether direct PDF OCR is supported
 * @param supportsAsyncPolling whether vendor async polling is supported
 * @param supportsImageOcr whether image OCR is supported
 * @param supportsTableStructure whether table extraction is supported
 * @param supportsCoordinates whether coordinates are supported
 * @param maxDirectPdfSize max direct PDF size
 * @param maxDirectPdfPages max direct PDF pages
 * @param maxFileSizeMb max file size in MB
 * @param maxPageCount max page count
 * @param notes capability notes
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
