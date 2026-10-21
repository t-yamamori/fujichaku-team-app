
package com.example.demo.entity;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Members {

    private int id;

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

    private String password;

    // ポイント：0～10億
    @Min(value = 0, message = "ポイントは0以上で入力してください")
    @Max(value = 1_000_000_000,
         message = "ポイントは10億以下で入力してください")
    private int point;

    private Boolean isDeleted;

    private LocalDate birthDate;
}
