package com.example.demo.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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

    // メールアドレス：半角英数字とメール用の記号のみ
    @NotBlank(message = "メールアドレスを入力してください")
    @Pattern(
        regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+$",
        message = "メールアドレスは半角英数字と使用可能な記号で入力してください"
    )
    private String mail;

    // パスワード
    // 更新時には入力不要なので、ここではバリデーションしない
    private String password;

    // 代表者：必須、20文字以内
    @NotBlank(message = "代表者名を入力してください")
    @Size(max = 20, message = "代表者名は20文字以内で入力してください")
    private String representative;

    // 削除フラグ
    private Boolean isDeleted;

    // 住所：指定された階数のみ
    @NotBlank(message = "住所を入力してください")
    @Pattern(
        regexp = "^(地下1階|1階|2階)$",
        message = "住所は地下1階、1階、2階のいずれかを選択してください"
    )
    private String address;

    // 説明
    @NotBlank(message = "説明を入力してください")
    private String description;

    // 画像の場所
    @NotBlank(message = "画像URLを入力してください")
    @Pattern(
        regexp = "^.*\\.(jpg|png)$",
        flags = Pattern.Flag.CASE_INSENSITIVE,
        message = "画像URLはjpgまたはpng形式で入力してください"
    )
    private String pictureUrl;

    // 電話番号
    @NotBlank(message = "電話番号を入力してください")
    @Pattern(
        regexp = "^(?:[0-9]{10,11}|(?=[0-9-]{12,13}$)(?:[0-9]+-){2}[0-9]+)$",
        message = "電話番号は数字10桁または11桁で入力してください（ハイフンは区切りとして2つ使用できます）"
    )
    private String phone;

    // 店舗名のふりがな
    // ひらがな、長音記号、半角・全角スペース、指定記号のみ許可
    @NotBlank(message = "ふりがなを入力してください")
    @Pattern(
        regexp = "^[ぁ-ゖー 　：・／＝/=:]+$",
        message = "ふりがなはひらがな、スペース、：・／＝のみで入力してください"
    )
    private String nameKana;

    // 店舗ごとの予約機能付与
    private boolean reservable;
}