package com.example.demo.entity;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopAdmin {

    // 店舗管理者ID
    private int id;

    // 管理対象の店舗ID
    @NotNull(message = "店舗IDを入力してください")
    private Integer storeId;

    // 店舗管理者名
    @NotBlank(message = "管理者名を入力してください")
    private String name;

    // メールアドレス
    @NotBlank(message = "メールアドレスを入力してください")
	@Email(
	        regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$",
	        message = "メールアドレスの形式が正しくありません（例：taro@gmail.com）"
	    )
    private String mail;

    // パスワード
    // 更新時には入力不要なので、ここではバリデーションしない
    private String password;

    // 論理削除フラグ
    private Boolean isDeleted;

    // 生年月日
    @NotNull(message = "生年月日を入力してください")
    private LocalDate birthDate;
}