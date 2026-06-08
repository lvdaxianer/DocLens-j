package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * OCR 文档任务的 MyBatis-Plus Mapper。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@Mapper
public interface DocumentJobMapper extends BaseMapper<DocumentJobEntity> {
}
