package com.example.demo.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Path IMAGE_DIR = Paths.get(
        "Z:/304教室/202604クラス/Aチーム_砂漠の不時着/#_提出用/プレゼン資料/デモ用画像置場"
    );

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        String location = IMAGE_DIR.toUri().toString();

        registry.addResourceHandler("/images/**")
                .addResourceLocations(location);
        
        
//        System.out.println("画像フォルダの絶対パス: "
//                + IMAGE_DIR.toAbsolutePath());
//
//        System.out.println("画像フォルダが存在する: "
//                + java.nio.file.Files.isDirectory(IMAGE_DIR));
//
//        System.out.println("対象画像が存在する: "
//                + java.nio.file.Files.exists(
//                    IMAGE_DIR.resolve("bakeryKomugi-top.jpg")));
//        
    }
}
