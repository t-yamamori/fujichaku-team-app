package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    /**
     * 管理者トップページ
     */
    @GetMapping("/admin/index")
    public String index() {
        return "admin/index";
    }
}