package com.example.demo.entity;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reviews {
	
	//口コミID
	private int id;
	
	//店舗ID
	private int storeId;
	
	//メンバーID
	private int memberId;
	
	//コメント
	@Size(max = 255, message = "口コミは255文字以内で入力してください。")
	private String comment;
	
	//登録時間
	private LocalDateTime createdAt;
	
	//評価
	private int grade;
}
