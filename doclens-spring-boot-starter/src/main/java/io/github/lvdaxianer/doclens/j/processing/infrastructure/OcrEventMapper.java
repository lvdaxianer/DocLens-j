package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * OCR 生命周期事件的 MyBatis-Plus Mapper。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@Mapper
public interface OcrEventMapper extends BaseMapper<OcrEventEntity> {
}
