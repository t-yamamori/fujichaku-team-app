package com.example.demo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
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
        model.addAttribute("newStore", new Stores());

        // 更新時のエラーがない状態
        model.addAttribute("updateErrors", new HashMap<String, String>());

        return "admin/tenants";
    }

    // 更新
    @PostMapping("/update")
    public String update(
            @Valid @ModelAttribute("store") Stores store,
            BindingResult bindingResult,
            Model model) {

        // バリデーションエラーがある場合
        if (bindingResult.hasErrors()) {

            // DBから一覧を取得
            model.addAttribute("stores", tenantService.findAll());

            // 新規登録フォーム
            model.addAttribute("newStore", new Stores());

            // エラーになった店舗を保存
            model.addAttribute("updateErrorStore", store);

            // フィールドごとのエラーメッセージを保存
            Map<String, String> updateErrors = new HashMap<>();

            for (FieldError error : bindingResult.getFieldErrors()) {
                updateErrors.put(error.getField(), error.getDefaultMessage());
            }

            model.addAttribute("updateErrors", updateErrors);

            return "admin/tenants";
        }

        // エラーがなければ更新
        tenantService.update(store);

        return "redirect:/admin/tenants";
    }

    // 削除
    @PostMapping("/delete")
    public String delete(
            @RequestParam("deleteIds") List<Integer> deleteIds) {

        tenantService.deleteByIds(deleteIds);

        return "redirect:/admin/tenants";
    }

    // 新規登録
    @PostMapping("/create")
    public String create(
            @Valid @ModelAttribute("newStore") Stores store,
            BindingResult bindingResult,
            Model model) {

        // パスワードチェック
        if (store.getPassword() == null
                || store.getPassword().isBlank()) {

            bindingResult.rejectValue(
                "password",
                "password.blank",
                "パスワードを入力してください"
            );

        } else if (store.getPassword().length() < 8) {

            bindingResult.rejectValue(
                "password",
                "password.size",
                "パスワードは8文字以上で入力してください"
            );
        }

        // エラーがある場合
        if (bindingResult.hasErrors()) {

            model.addAttribute("stores", tenantService.findAll());

            return "admin/tenants";
        }

        // エラーがなければ登録
        tenantService.create(store);

        return "redirect:/admin/tenants";
    }
}