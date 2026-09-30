package com.example.demo.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reviews {
	
	//口コミID
	private int id;
	
	//メンバーID
	private int storeId;
	
	//メンバーID
	private int member_id;
	
	//コメント
	private String commnet;
	
	//登録時間
	private LocalDateTime createdAt;
	
	//評価
	private int grade;

}
