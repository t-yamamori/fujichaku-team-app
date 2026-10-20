
package com.example.demo.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.entity.Members;
import com.example.demo.form.RegisterForm;
import com.example.demo.service.AdminMemberService;
import com.example.demo.service.RegisterService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AdminMemberController {

    private final AdminMemberService adminMemberService;
    private final RegisterService registerService;

    private static final String DUPLICATE_MAIL_MESSAGE =
        "このメールアドレスは既に登録されています。（削除済みの会員のメールアドレスも使用できません）";

    // 会員一覧表示
    @GetMapping("/admin/members")
    public String showMembers(Model model) {

        List<Members> members = adminMemberService.findAll();
        model.addAttribute("members", members);

        return "admin/list";
    }

    // 更新する会員情報を取得
    @GetMapping("/admin/members/edit")
    public String editMember(int id, Model model) {

        Members member = adminMemberService.findById(id);
        List<Members> members = adminMemberService.findAll();

        model.addAttribute("member", member);
        model.addAttribute("members", members);

        return "admin/list";
    }

    // 会員情報の更新
    @PostMapping("/admin/members/update")
    public String updateMember(
            @Valid @ModelAttribute("member") Members member,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        Integer id = member.getId();

        // 入力形式に問題がなければメールアドレスの重複を確認
        if (!result.hasFieldErrors("mail")
                && adminMemberService.existsByMailExceptId(
                    member.getMail(), id)) {

            result.rejectValue(
                "mail",
                "duplicate",
                DUPLICATE_MAIL_MESSAGE
            );
        }

        // 入力エラーがあれば、入力内容を保持してモーダルを再表示
        if (result.hasErrors()) {
            return backToEditWithError(member, result, redirectAttributes);
        }

        // DB側で重複が検出された場合にもエラーを表示
        try {
            adminMemberService.update(
                id,
                member.getName(),
                member.getBirthDate(),
                member.getMail(),
                member.getPoint()
            );
        } catch (DuplicateKeyException e) {

            result.rejectValue(
                "mail",
                "duplicate",
                DUPLICATE_MAIL_MESSAGE
            );

            return backToEditWithError(member, result, redirectAttributes);
        }

        return "redirect:/admin/members?updated=true";
    }

    // 更新エラーと入力内容を保持して一覧に戻る
    private String backToEditWithError(
            Members member,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        List<String> errors = result.getAllErrors().stream()
            .map(error -> error.getDefaultMessage())
            .toList();

        redirectAttributes.addFlashAttribute("editErrors", errors);
        redirectAttributes.addFlashAttribute("editId", member.getId());
        redirectAttributes.addFlashAttribute("editName", member.getName());
        redirectAttributes.addFlashAttribute(
            "editBirthDate", member.getBirthDate());
        redirectAttributes.addFlashAttribute("editMail", member.getMail());
        redirectAttributes.addFlashAttribute("editPoint", member.getPoint());

        return "redirect:/admin/members";
    }

    // 会員の削除（論理削除）
    @PostMapping("/admin/members/delete")
    public String deleteMember(@RequestParam("id") Integer id) {

        adminMemberService.delete(id);

        return "redirect:/admin/members?deleted=true";
    }

    // 管理者による新規会員登録
    @PostMapping("/admin/members/create")
    public String createMember(
            @Valid @ModelAttribute RegisterForm form,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        // メールの形式に問題がなければ重複を確認
        if (!result.hasFieldErrors("mail")
                && registerService.existsByMail(form.getMail())) {

            result.rejectValue(
                "mail",
                "duplicate",
                DUPLICATE_MAIL_MESSAGE
            );
        }

        // 入力エラーがあればモーダルを開き直す
        if (result.hasErrors()) {
            return backWithErrors(form, result, redirectAttributes);
        }

        // DB側で重複が検出された場合にもエラーを表示
        try {
            registerService.register(form);
        } catch (DuplicateKeyException e) {

            result.rejectValue(
                "mail",
                "duplicate",
                DUPLICATE_MAIL_MESSAGE
            );

            return backWithErrors(form, result, redirectAttributes);
        }

        return "redirect:/admin/members?created=true";
    }

    // 登録エラーと入力内容を保持して一覧に戻る
    private String backWithErrors(
            RegisterForm form,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        List<String> errors = result.getAllErrors().stream()
            .map(error -> error.getDefaultMessage())
            .toList();

        redirectAttributes.addFlashAttribute("createErrors", errors);
        redirectAttributes.addFlashAttribute("createName", form.getName());
        redirectAttributes.addFlashAttribute(
            "createBirthDate", form.getBirthDate());
        redirectAttributes.addFlashAttribute("createMail", form.getMail());

        return "redirect:/admin/members";
    }
}
