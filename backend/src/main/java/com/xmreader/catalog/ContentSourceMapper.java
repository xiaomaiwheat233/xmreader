package com.xmreader.catalog;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ContentSourceMapper extends BaseMapper<ContentSourceEntity> {

    @Select("SELECT * FROM content_sources WHERE source_key = #{sourceKey} LIMIT 1")
    ContentSourceEntity findBySourceKey(@Param("sourceKey") String sourceKey);
}
