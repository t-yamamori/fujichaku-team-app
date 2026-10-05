package com.example.demo.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Reservations {

    // 予約番号
    private int id;

    // 会員ID
    private int member_id;

    // 予約日時
    private LocalDateTime reservation_date;

    // 予約状況
    private String status;

    // 登録日時
    private LocalDateTime created_at;

    // 店舗ID
    private int store_id;
    
    //予約人数
    private int number;
}
