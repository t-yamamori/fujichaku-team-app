package com.example.demo.form;

import java.time.LocalDate;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MemberEditForm {//会員情報更新フォーム
	@NotBlank(message = "名前を入力してください")
	@Size(max = 255, message = "名前は255文字以内で入力してください")
    private String name;

	@NotBlank(message = "メールアドレスを入力してください")
	@Email(
	        regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$",
	        message = "メールアドレスの形式が正しくありません（例：taro@gmail.com）"
	    )
    @Size(max = 255, message = "メールアドレスは255文字以内で入力してください")
    private String mail;

	 // 空欄なら「パスワードは変更しない」、入力したなら8文字以上
    @Pattern(regexp = "^$|^.{8,255}$", message = "パスワードは8文字以上で入力してください")
    private String password;
 
    // <input type="date"> から送られる「2000-01-01」の形を受け取る
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Past(message = "生年月日は今日より前の日付を入力してください")
    private LocalDate birthDate;
    
 // 1900年1月1日より前を禁止する（空欄のときは、このチェックをしない）
    @AssertTrue(message = "生年月日は1900年1月1日以降の日付を入力してください")
    public boolean isBirthDateAfter1900() {
        return birthDate == null || !birthDate.isBefore(LocalDate.of(1900, 1, 1));
    }
}
