
package com.example.demo.entity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Users {

    /*
     * ユーザーID
     *
     * 非会員予約では、
     * reservations.member_id = NULL
     * reservations.user_id   = このID
     */
    private int id;

    /*
     * ユーザー名
     */
    private String name;

    /*
     * メールアドレス
     */
    @Email(
        regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$",
        message = "メールアドレスの形式が正しくありません（例：taro@gmail.com）"
    )
    @Size(
        max = 255,
        message = "メールアドレスは255文字以内で入力してください"
    )
    private String mail;
}
