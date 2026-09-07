package com.campus.venue.venue.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.venue.venue.entity.Venue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface VenueMapper extends BaseMapper<Venue> {

    @Select("SELECT * FROM venue WHERE id = #{id} FOR UPDATE")
    Venue selectByIdForUpdate(Long id);
}
