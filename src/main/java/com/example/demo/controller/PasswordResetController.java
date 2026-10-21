package com.example.demo.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.entity.Members;
import com.example.demo.form.PasswordResetVerifyForm;
import com.example.demo.service.PasswordResetService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    // 本人確認画面を表示
    @GetMapping("/password/reset/verify")
    public String showVerify(Model model) {
        model.addAttribute("form", new PasswordResetVerifyForm());
        return "password/verify";
    }

    // 本人確認を実行
    @PostMapping("/password/reset/verify")
    public String verify(
            @Valid @ModelAttribute("form") PasswordResetVerifyForm form,
            BindingResult result,
            HttpSession session,
            Model model) {

        // 【変更】名前の空白（半角スペース・全角スペース）をすべて取りのぞく
        //   「堺 雅人」「堺　雅人」「堺雅人」→ どれも「堺雅人」にして、DBの名前と比べる
        String name = form.getName();
        if (name != null) {
            name = name.replaceAll("[\\s\\u3000]", "");
        }

        // 名前が空欄でない場合だけチェックする
        if (name != null && !name.isBlank()) {

            // 半角の中点・ハイフンが含まれているか確認
            if (name.contains("･") || name.contains("-")) {
                result.rejectValue(
                        "name",
                        "name.connector",
                        "中点、ハイフンなど接続文字は全角で入れてください");
            }
        }

        // 未入力などの入力エラーを確認
        if (result.hasErrors()) {
            return "password/verify";
        }

        // 名前と生年月日で会員を検索（空白を消した name を渡す）
        Members member = passwordResetService.verifyMember(
                name,
                form.getBirthDate());

        // 会員が見つからない場合
        if (member == null) {
            model.addAttribute(
                    "errorMessage",
                    "入力された情報を確認できませんでした。");
            return "password/verify";
        }

        // 本人確認できた会員のIDをセッションに保存
        session.setAttribute("passwordResetMemberId", member.getId());

        // パスワード再設定画面へ
        return "password/reset";
    }

    // パスワード再設定を実行（元のまま）
    @PostMapping("/password/reset")
    public String resetPassword(
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            HttpSession session,
            Model model) {

        // 本人確認済みの会員IDを取得
        Integer memberId =
                (Integer) session.getAttribute("passwordResetMemberId");

        // 本人確認をしていない場合は本人確認画面へ戻す
        if (memberId == null) {
            return "redirect:/password/reset/verify";
        }

        // パスワードが一致しない場合
        if (!password.equals(confirmPassword)) {
            model.addAttribute(
                    "errorMessage",
                    "パスワードが一致しません。もう一度入力してください。");
            return "password/reset";
        }

        // パスワードが8文字未満の場合
        if (password.length() < 8) {
            model.addAttribute(
                    "errorMessage",
                    "パスワードは8文字以上で入力してください。");
            return "password/reset";
        }
        
        // パスワードが64文字を超える場合
        if (password.length() > 64) {
            model.addAttribute(
                    "errorMessage",
                    "パスワードは64文字以内で入力してください。");
            return "password/reset";
        }

        // パスワードを更新
        passwordResetService.resetPassword(memberId, password);

        // 本人確認情報をセッションから削除
        session.removeAttribute("passwordResetMemberId");

        // 完了画面へ
        return "password/complete";
    }
}