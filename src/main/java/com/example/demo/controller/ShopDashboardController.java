package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ShopDashboardController {

    /**
     * 店舗管理者向けの自店舗トップページ
     */
    @GetMapping("/shops/{storeId}")
    public String dashboard(
            @PathVariable("storeId") int storeId,
            Model model) {

        model.addAttribute("storeId", storeId);

        return "shop-dashboard/index";
    }
}