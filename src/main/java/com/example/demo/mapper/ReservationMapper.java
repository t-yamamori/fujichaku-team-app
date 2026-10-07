package com.example.demo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.entity.Reservations;

@Mapper
public interface ReservationMapper {

    /*
     * =========================================================
     * 予約登録
     * =========================================================
     *
     * 会員予約
     * memberId = 値
     * userId   = null
     *
     * 非会員予約
     * memberId = null
     * userId   = 値
     */
    void insertReservation(Reservations reservation);


    /*
     * =========================================================
     * 予約IDから予約を取得
     * =========================================================
     */
    Reservations findById(
            @Param("reservationId") int reservationId
    );


    /*
     * =========================================================
     * 会員IDから予約履歴を取得
     * =========================================================
     */
    List<Reservations> findByMemberId(
            @Param("memberId") int memberId
    );


    /*
     * =========================================================
     * ユーザーIDから予約履歴を取得
     * =========================================================
     */
    List<Reservations> findByUserId(
            @Param("userId") int userId
    );

}