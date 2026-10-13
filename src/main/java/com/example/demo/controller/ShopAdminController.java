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

import com.example.demo.entity.ShopAdmin;
import com.example.demo.service.ShopAdminService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/shop-admins")
@RequiredArgsConstructor
public class ShopAdminController {

    private final ShopAdminService shopAdminService;

    // 店舗管理者管理画面
    @GetMapping
    public String showShopAdmins(Model model) {

        List<ShopAdmin> shopAdmins = shopAdminService.findAll();

        model.addAttribute("shopAdmins", shopAdmins);
        model.addAttribute("newShopAdmin", new ShopAdmin());

        // 更新時のエラーがない状態
        model.addAttribute(
            "updateErrors",
            new HashMap<String, String>()
        );

        return "admin/shop-admins";
    }

    // 更新
    @PostMapping("/update")
    public String update(
            @Valid @ModelAttribute("shopAdmin") ShopAdmin shopAdmin,
            BindingResult bindingResult,
            Model model) {

        // バリデーションエラーがある場合
        if (bindingResult.hasErrors()) {

            // DBから一覧を取得
            model.addAttribute(
                "shopAdmins",
                shopAdminService.findAll()
            );

            // 新規登録フォーム
            model.addAttribute(
                "newShopAdmin",
                new ShopAdmin()
            );

            // エラーになった管理者を保存
            model.addAttribute(
                "updateErrorShopAdmin",
                shopAdmin
            );

            // フィールドごとのエラーメッセージ
            Map<String, String> updateErrors = new HashMap<>();

            for (FieldError error : bindingResult.getFieldErrors()) {
                updateErrors.put(
                    error.getField(),
                    error.getDefaultMessage()
                );
            }

            model.addAttribute("updateErrors", updateErrors);

            return "admin/shop-admins";
        }

        // エラーがなければ更新
        shopAdminService.update(shopAdmin);

        return "redirect:/admin/shop-admins";
    }

    // 削除
    @PostMapping("/delete")
    public String delete(
            @RequestParam("deleteIds") List<Integer> deleteIds) {

        shopAdminService.deleteByIds(deleteIds);

        return "redirect:/admin/shop-admins";
    }

    // 新規登録
    @PostMapping("/create")
    public String create(
            @Valid @ModelAttribute("newShopAdmin") ShopAdmin shopAdmin,
            BindingResult bindingResult,
            Model model) {

        // パスワードチェック
        if (shopAdmin.getPassword() == null
                || shopAdmin.getPassword().isBlank()) {

            bindingResult.rejectValue(
                "password",
                "password.blank",
                "パスワードを入力してください"
            );

        } else if (shopAdmin.getPassword().length() < 8) {

            bindingResult.rejectValue(
                "password",
                "password.size",
                "パスワードは8文字以上で入力してください"
            );
        }

        // エラーがある場合
        if (bindingResult.hasErrors()) {

            model.addAttribute(
                "shopAdmins",
                shopAdminService.findAll()
            );

            return "admin/shop-admins";
        }

        // エラーがなければ登録
        shopAdminService.create(shopAdmin);

        return "redirect:/admin/shop-admins";
    }
}