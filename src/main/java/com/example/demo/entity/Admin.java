
package com.example.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Admin {

    // 管理者ID
    private int id;

    // 管理者名
    private String name;

    // メールアドレス
    private String mail;

    // パスワード（BCryptでハッシュ化された値）
    private String password;
}