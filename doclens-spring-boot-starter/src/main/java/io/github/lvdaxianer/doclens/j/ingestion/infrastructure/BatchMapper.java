package io.github.lvdaxianer.doclens.j.ingestion.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * OCR 批次的 MyBatis-Plus Mapper。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@Mapper
public interface BatchMapper extends BaseMapper<BatchEntity> {
}
