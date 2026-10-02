package com.example.demo.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.entity.Reservations;

@Mapper
public interface ReservationMapper {

    // 予約登録
    void insertReservation(Reservations reservation);

    // 予約番号から予約取得
    Reservations findById(int reservationId);
    
}