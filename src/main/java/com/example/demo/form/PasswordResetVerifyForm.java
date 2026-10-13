package com.example.demo.form;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

@Data
public class PasswordResetVerifyForm {

    @NotBlank(message = "名前を入力してください。")
    private String name;

    @NotNull(message = "生年月日を入力してください。")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;
}