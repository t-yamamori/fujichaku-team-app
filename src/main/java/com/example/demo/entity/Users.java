package com.example.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Users {

    /*
     * =========================================================
     * ユーザーID
     * =========================================================
     *
     * 非会員予約の場合、
     *
     * reservations.member_id = NULL
     * reservations.user_id   = このID
     *
     * として使用する。
     */
    private int id;


    /*
     * =========================================================
     * ユーザー名
     * =========================================================
     */
    private String name;


    /*
     * =========================================================
     * メールアドレス
     * =========================================================
     */
    private String mail;

}