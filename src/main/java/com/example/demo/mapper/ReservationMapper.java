package com.example.demo.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.entity.Reservations;

@Mapper
public interface ReservationMapper {

    // 予約登録
    void insertReservation(Reservations reservation);

    // 予約IDから1件取得
    Reservations findById(
        @Param("reservationId") int reservationId
    );

    // 会員IDから予約取得
    List<Reservations> findByMemberId(
        @Param("memberId") int memberId
    );

    // ユーザーIDから予約取得
    List<Reservations> findByUserId(
        @Param("userId") int userId
    );

    // 全予約取得
    List<Reservations> findAll();

    // 店舗ID + 会員IDから予約取得
    List<Reservations> selectByStoreIdAndMemberId(
        @Param("storeId") int storeId,
        @Param("memberId") int memberId
    );

    // 店舗IDから予約一覧取得
    List<Reservations> findByStoreId(
        @Param("storeId") int storeId
    );

    // 店舗予約の日時・人数を変更
    int updateForStore(
        @Param("reservationId") int reservationId,
        @Param("storeId") int storeId,
        @Param("reservationDate") LocalDateTime reservationDate,
        @Param("number") int number
    );

    // 店舗予約のステータスを変更
    int updateStatusForStore(
        @Param("reservationId") int reservationId,
        @Param("storeId") int storeId,
        @Param("status") String status
    );
}