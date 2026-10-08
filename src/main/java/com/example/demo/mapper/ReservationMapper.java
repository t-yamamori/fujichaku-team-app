package com.example.demo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.entity.Reservations;

@Mapper
public interface ReservationMapper {

    void insertReservation(Reservations reservation);

    Reservations findById(@Param("reservationId") int reservationId);

    List<Reservations> findByMemberId(@Param("memberId") int memberId);

    List<Reservations> findByUserId(@Param("userId") int userId);

    List<Reservations> findAll();

    List<Reservations> selectByStoreIdAndMemberId(
            @Param("storeId") int storeId,
            @Param("memberId") int memberId);
}