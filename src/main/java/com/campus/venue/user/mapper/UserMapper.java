package com.campus.venue.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.venue.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM `user` WHERE id = #{id} FOR UPDATE")
    User selectByIdForUpdate(Long id);
}
