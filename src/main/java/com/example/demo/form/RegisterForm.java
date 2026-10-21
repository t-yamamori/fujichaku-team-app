
package com.example.demo.form;

import java.time.LocalDate;
import java.time.Period;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterForm {

    /*
     * 名前：必須、20文字以内
     */
    @NotBlank(message = "名前は必須です。")
    @Size(max = 20, message = "名前は20文字以内で入力してください。")
    private String name;

    /*
     * 生年月日：必須、未来の日付は不可
     */
    @NotNull(message = "生年月日は必須です。")
    @Past(message = "生年月日は過去の日付を入力してください。")
    private LocalDate birthDate;

    /*
     * 年齢：15歳以上、120歳未満
     */
    @AssertTrue(message = "年齢は15歳以上120歳未満で入力してください。")
    public boolean isBirthDateAgeValid() {

        if (birthDate == null) {
            return true;
        }

        int age = Period.between(
            birthDate,
            LocalDate.now()
        ).getYears();

        return age >= 15 && age < 120;
    }

    /*
     * メールアドレス：必須、形式チェック、255文字以内
     */
    @NotBlank(message = "メールアドレスは必須です。")
    @Email(
        regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{3}$",
        message = "メールアドレスはXXX@yyy.zzzの形式で入力してください（末尾は英字3文字）。"
    )
    @Size(max = 255, message = "メールアドレスは255文字以内で入力してください。")
    private String mail;

    /*
     * パスワード：必須、8文字以上64文字以内
     */
    @NotBlank(message = "パスワードは必須です。")
    @Size(min = 8, max = 64, message = "パスワードは8文字以上64文字以内で入力してください。")
    private String password;
    /*
     * 名前の半角・全角スペースなどを除去
     */
    public void setName(String name) {
        this.name = (name == null)
            ? null
            : name.replaceAll("[\\s\\u3000]", "");
    }
}
