package com.example.demo.controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

public class StroreController {//Mainコントローラーになる
	@GetMapping("/")
    public String index(Model model) {
        model.addAttribute("leaderMsg", "リーダーです！");
        // TODO: ここにメンバーのServiceを後で呼ぶ
        return "stores";
    }
}
