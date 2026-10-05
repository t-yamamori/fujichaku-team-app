package com.example.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Stores {

	//店舗ID
	private int id;
	
	//店舗の名前
	private String name;
	
	//メールアドレス
	private String mail;
	
	//パスワード
	private String password;
	
	//代表者
	private String representative;
	
	//過去フラグ
	private Boolean isDeleted;
	
	//住所
	private String address;
	
	//説明
	private String description;
	
	//画像の場所
	private String pictureUrl;

	//電話番号
	private String phone;
}