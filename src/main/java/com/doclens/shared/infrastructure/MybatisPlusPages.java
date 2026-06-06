package com.doclens.shared.infrastructure;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.doclens.shared.domain.DocLensConstants;

/**
 * Factory for MyBatis-Plus page requests used by repositories.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public final class MybatisPlusPages {

    /**
     * Hides utility constructor.
     *
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    private MybatisPlusPages() {
    }

    /**
     * Creates a page request limited to a single record.
     *
     * @param <T> entity type
     * @return single-record page request
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public static <T> Page<T> one() {
        return Page.of(DocLensConstants.FIRST_PAGE_NO, DocLensConstants.SINGLE_QUERY_LIMIT);
    }

    /**
     * Creates a default bounded list page request.
     *
     * @param <T> entity type
     * @return default list page request
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public static <T> Page<T> listLimit() {
        return Page.of(DocLensConstants.FIRST_PAGE_NO, DocLensConstants.DEFAULT_QUERY_LIMIT);
    }
}
