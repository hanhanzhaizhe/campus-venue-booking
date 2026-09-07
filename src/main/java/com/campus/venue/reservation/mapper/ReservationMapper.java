package com.campus.venue.reservation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.venue.reservation.entity.Reservation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReservationMapper extends BaseMapper<Reservation> {
}
