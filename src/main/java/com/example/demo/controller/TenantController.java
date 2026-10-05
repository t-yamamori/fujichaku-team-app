package com.example.demo.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.entity.Stores;
import com.example.demo.service.TenantService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    // テナント管理画面
    @GetMapping
    public String showTenants(Model model) {
        List<Stores> stores = tenantService.findAll();
        model.addAttribute("stores", stores);
        // 新規登録用の空フォーム
        model.addAttribute("newStore", new Stores());
        return "admin/tenants";
    }

    // 更新
    @PostMapping("/update")
    public String update(@ModelAttribute Stores store) {
        tenantService.update(store);
        return "redirect:/admin/tenants";
    }

    // 削除
    @PostMapping("/delete")
    public String delete(@RequestParam("deleteIds") List<Integer> deleteIds) {
        tenantService.deleteByIds(deleteIds);
        return "redirect:/admin/tenants";
    }

    // 新規登録
    @PostMapping("/create")
    public String create(@ModelAttribute("newStore") Stores store) {
        tenantService.create(store);
        return "redirect:/admin/tenants";
    }
}