package com.example.demo.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor

public class RegisterForm {
	@NotBlank(message = "名前は必須です。")
    private String name;

    @NotBlank(message = "生年月日は必須です。")
    private String birthDate;

    @NotBlank(message = "メールアドレスは必須です。")
    private String mail;

    @NotBlank(message = "パスワードは必須です。")
    @Size(min = 8, message = "パスワードは8文字以上で入力してください。")
    private String password;
    
    // 【追加】名前の空白（半角スペース・全角スペース）をすべて取りのぞいてから入れる
    //   例）「堺 雅人」「堺　雅人」「 堺雅人 」→「堺雅人」
    //   @Data があっても、自分で書いた setName のほうが使われる（Lombok は setName を作らなくなる）
    public void setName(String name) {
        this.name = (name == null) ? null : name.replaceAll("[\\s\\u3000]", "");
    }


}
