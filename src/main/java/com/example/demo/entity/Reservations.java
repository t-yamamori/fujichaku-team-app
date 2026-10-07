package com.example.demo.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Reservations {

    /*
     * 予約ID
     */
    private Integer id;

    /*
     * 会員ID
     *
     * 会員予約の場合のみ使用
     *
     * 非会員予約の場合はNULL
     */
    private Integer memberId;

    /*
     * ユーザーID
     *
     * 非会員予約の場合に使用
     *
     * 会員予約の場合はNULL
     */
    private Integer userId;

    /*
     * 予約日時
     */
    private LocalDateTime reservationDate;

    /*
     * 予約ステータス
     */
    private String status;

    /*
     * 予約作成日時
     */
    private LocalDateTime createdAt;

    /*
     * 店舗ID
     */
    private Integer storeId;

    /*
     * 予約人数
     */
    private Integer number;
}