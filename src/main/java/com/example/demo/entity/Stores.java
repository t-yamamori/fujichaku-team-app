package com.example.demo.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Stores {

    // 店舗ID
    private int id;

    // 店舗の名前
    @NotBlank(message = "店舗名を入力してください")
    private String name;

    // メールアドレス
    @NotBlank(message = "メールアドレスを入力してください")
    @Pattern(
        regexp = "^$|^[^@]+@[^@]+\\.[^@]+$",
        message = "メールアドレスはxxx@yyy.zzzの形式で入力してください"
    )
    private String mail;

    // パスワード
    // 更新時には入力不要なので、ここではバリデーションしない
    private String password;

    // 代表者
    @NotBlank(message = "代表者名を入力してください")
    private String representative;

    // 過去フラグ
    private Boolean isDeleted;

    // 住所
    @NotBlank(message = "住所を入力してください")
    private String address;

    // 説明
    @NotBlank(message = "説明を入力してください")
    private String description;

    // 画像の場所
    @NotBlank(message = "画像URLを入力してください")
    @Pattern(
        regexp = "^$|^.*\\.(jpg|png)$",
        message = "画像URLはjpgまたはpng形式で入力してください"
    )
    private String pictureUrl;

    // 電話番号
    @NotBlank(message = "電話番号を入力してください")
    @Pattern(
        regexp = "^$|^[0-9]{11}$",
        message = "電話番号は数字11桁で入力してください"
    )
    private String phone;
}