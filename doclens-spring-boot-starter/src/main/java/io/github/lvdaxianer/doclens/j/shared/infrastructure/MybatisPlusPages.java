package io.github.lvdaxianer.doclens.j.shared.infrastructure;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;

/**
 * 仓储使用的 MyBatis-Plus 分页请求工厂。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public final class MybatisPlusPages {

    /**
     * 隐藏工具类构造器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    private MybatisPlusPages() {
    }

    /**
     * 创建限制为单条记录的分页请求。
     *
     * @param <T> 实体类型
     * @return 单条记录分页请求
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public static <T> Page<T> one() {
        return Page.of(DocLensConstants.FIRST_PAGE_NO, DocLensConstants.SINGLE_QUERY_LIMIT);
    }

    /**
     * 创建默认有界列表分页请求。
     *
     * @param <T> 实体类型
     * @return 默认列表分页请求
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public static <T> Page<T> listLimit() {
        return Page.of(DocLensConstants.FIRST_PAGE_NO, DocLensConstants.DEFAULT_QUERY_LIMIT);
    }
}
