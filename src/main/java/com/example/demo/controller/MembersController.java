package com.example.demo.controller;
 
import java.security.Principal;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.entity.Members;
import com.example.demo.form.MemberEditForm;
import com.example.demo.service.serviceInterface.MembersService;

import lombok.RequiredArgsConstructor;
 
/**
 * 会員情報画面（表示・変更・退会）を受け付けるコントローラー
 *
 *   GET  /members/edit    会員情報画面を表示する（トップ画面の「会員情報」ボタン）
 *   POST /members/edit    会員情報を変更する（「変更」ボタン）
 *   POST /members/delete  退会する（確認モーダルの「はい」ボタン）
 *
 * ログインの仕組みは別の担当なので、ここでは作らない。
 * ログイン中の人は、Java標準の Principal で受け取る（ログインID＝会員ID）。
 * /members/** はログインが必要な設定なので、principal は必ず入っている。
 */
@Controller
@RequestMapping("/members")
@RequiredArgsConstructor
public class MembersController {
 
    private final MembersService membersService;
 
    /**
     * 会員情報画面を表示する
     * 今の登録内容を MemberEditForm に詰め替えて、入力欄に入れた状態で表示する
     */
    @GetMapping("/edit")
    public String showEdit(Principal principal, Model model) {
        Members member = membersService.findByLoginId(principal.getName());
        if (member == null) {
            // 退会済みなどで会員が見つからないときは、トップ画面（店舗一覧）へ
            return "redirect:/shops";
        }
        model.addAttribute("memberEditForm", membersService.toForm(member));
        return "members/edit";
    }
 
    /**
     * 会員情報を変更する（「変更」ボタン）
     * 入力ミスがあれば、入力した内容とエラーを残したまま、同じ画面を表示する
     */
    @PostMapping("/edit")
    public String update(@Validated @ModelAttribute("memberEditForm") MemberEditForm editForm,
                         BindingResult result,
                         Principal principal,
                         RedirectAttributes redirectAttributes) {
        Members member = membersService.findByLoginId(principal.getName());
        if (member == null) {
            return "redirect:/shops";
        }
 
        // 入力チェック（@NotBlank など）にひっかかったとき
        if (result.hasErrors()) {
            return "members/edit";
        }
 
        try {
            membersService.update(member.getId(), editForm);
        } catch (DuplicateKeyException e) {
            // 他の会員と同じメールアドレスだったとき（DBの一意インデックスで検出）
            result.rejectValue("mail", "duplicate", "このメールアドレスはすでに使われています");
            return "members/edit";
        }
 
        // 変更後は同じ画面を開き直し、「会員情報を変更しました」と表示する
        redirectAttributes.addFlashAttribute("message", "会員情報を変更しました");
        return "redirect:/members/edit";
    }
 
    /**
     * 退会する（確認モーダルの「はい」ボタン）
     * 名前は delete だが、行は消さず、is_deleted を true にする（論理削除）。
     * そのあとログアウトして、トップ画面（店舗一覧）へ移動する
     */
    @PostMapping("/delete")
    public String delete(Principal principal, HttpServletRequest request) throws ServletException {
        Members member = membersService.findByLoginId(principal.getName());
        if (member != null) {
            membersService.delete(member.getId());
        }
        // Java標準のログアウト命令（ログイン中の状態を終わらせる）
        request.logout();
        return "redirect:/shops";
    }
}
 