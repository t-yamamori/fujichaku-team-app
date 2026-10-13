package com.example.demo;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {

    public static void main(String[] args) {

        BCryptPasswordEncoder encoder =
                new BCryptPasswordEncoder();

        String rawPassword = "aaaaaaaa";

        String encodedPassword =
                encoder.encode(rawPassword);

        System.out.println(encodedPassword);

        // 生成したハッシュで照合できるか確認
        System.out.println(
            encoder.matches(rawPassword, encodedPassword)
        );
    }
}

