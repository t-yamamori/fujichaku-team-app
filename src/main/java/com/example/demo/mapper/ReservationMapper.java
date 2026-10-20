package com.example.demo.mapper;

import java.time.LocalDate;
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
            @Param("reservationId") int reservationId);

    // 会員IDから予約取得
    List<Reservations> findByMemberId(
            @Param("memberId") int memberId);

    // ゲストユーザーIDから予約取得
    List<Reservations> findByUserId(
            @Param("userId") int userId);

    // 全予約取得
    List<Reservations> findAll();

    // 店舗ID + 会員IDから予約取得
    List<Reservations> selectByStoreIdAndMemberId(
            @Param("storeId") int storeId,
            @Param("memberId") int memberId);

    // 店舗IDから予約一覧取得
    List<Reservations> findByStoreId(
            @Param("storeId") int storeId);

    // 店舗管理者：日時・人数を変更
    int updateForStore(
            @Param("reservationId") int reservationId,
            @Param("storeId") int storeId,
            @Param("reservationDate") LocalDateTime reservationDate,
            @Param("number") int number);

    // 店舗管理者：ステータスを変更
    int updateStatusForStore(
            @Param("reservationId") int reservationId,
            @Param("storeId") int storeId,
            @Param("status") String status);

    // モール管理者：全店舗を対象に予約検索
    List<Reservations> searchAllForAdmin(
            @Param("reservationId") Integer reservationId,
            @Param("reservationDate") LocalDate reservationDate);

    // 店舗管理者：担当店舗内で予約検索
    List<Reservations> searchForStoreAdmin(
            @Param("storeId") int storeId,
            @Param("reservationId") Integer reservationId,
            @Param("reservationDate") LocalDate reservationDate);

    // モール管理者：日時・人数を変更
    int updateForAdmin(
            @Param("reservationId") int reservationId,
            @Param("reservationDate") LocalDateTime reservationDate,
            @Param("number") int number);

    // モール管理者：ステータスを変更
    int updateStatusForAdmin(
            @Param("reservationId") int reservationId,
            @Param("status") String status);

    // 論理削除（モール管理者：全店舗が対象）
    int logicalDeleteForAdmin(
            @Param("reservationId") int reservationId);

    // 論理削除（店舗管理者：担当店舗だけ）
    int logicalDeleteForStore(
            @Param("reservationId") int reservationId,
            @Param("storeId") int storeId);

    // 会員IDの存在確認
    int countMemberId(
            @Param("memberId") int memberId);

    // ゲストユーザーIDの存在確認
    int countUserId(
            @Param("userId") int userId);

    // 会員が退会済みか確認
    int countDeletedMemberId(
            @Param("memberId") int memberId);
}