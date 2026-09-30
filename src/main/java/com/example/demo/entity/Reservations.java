package com.example.demo.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Reservations {
	//予約ID
	private int id;
	//会員ID
	private int member_id;
	//予約日時
	private LocalDateTime reservation_date;
	//予約状況
	private String status;
	//登録時間
	private LocalDateTime created_at;
	//店ID
	private int store_id;
}
