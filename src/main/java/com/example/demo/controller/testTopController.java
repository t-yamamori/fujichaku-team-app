package com.example.demo.controller;
 
import java.security.Principal;
 
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
 
/**
 * 【動作確認用】ログイン後の仮のトップ画面を表示するコントローラー
 * 会員情報（表示・変更・退会）の動作を、自分だけで確認するために使う。
 * 本番のトップ画面（/shops）とは別のURL（/test/top）なので、チームの画面とはぶつからない。
 *
 * ※ 動作確認が終わったら、このクラスと templates/test/top.html は削除する
 */
@Controller
public class testTopController {
 
    @GetMapping("/test/top")
    public String showTestTop(Principal principal, Model model) {
        // ログインしていなければ、ログイン画面へ
        if (principal == null) {
            return "redirect:/login";
        }
        // 画面に「ログイン中の会員ID」を表示するために渡す
        model.addAttribute("loginId", principal.getName());
        return "test/top";
    }
}
 
