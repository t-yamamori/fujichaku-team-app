package com.example.demo.controller;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
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
	    	
	    	// 選択した会員を取得
	        Members member = adminMemberService.findById(id);
	        
	        // 会員一覧を取得
	        List<Members> members = adminMemberService.findAll();

	        model.addAttribute("member", member);
	        model.addAttribute("members", members);

	        return "admin/list";
	    }
	    
	    // 会員情報の更新
	    @PostMapping("/admin/members/update")
	    public String updateMember(
	            @RequestParam("id") Integer id,
	            @RequestParam("name") String name,
	            @RequestParam("birthDate")
	            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birthDate,
	            @RequestParam("mail") String mail) {

	        adminMemberService.update(id, name, birthDate, mail);

	        return "redirect:/admin/members";   // 更新後は一覧を開き直す
	    }
	    
	    // 会員の削除（論理削除）
	    @PostMapping("/admin/members/delete")
	    public String deleteMember(@RequestParam("id") Integer id) {

	        adminMemberService.delete(id);

	        return "redirect:/admin/members";
	    }
	    
	 // 管理者による新規会員登録
	    @PostMapping("/admin/members/create")
	    public String createMember(@Valid RegisterForm form,
	                               BindingResult result,
	                               RedirectAttributes redirectAttributes) {

	        // 入力チェックでエラーがあれば、メッセージを付けて一覧に戻す
	        if (result.hasErrors()) {
	            redirectAttributes.addFlashAttribute("errorMessage",
	                    "入力内容を確認してください。（氏名・生年月日・メール・パスワードは必須です）");
	            return "redirect:/admin/members";
	        }

	        registerService.register(form);

	        return "redirect:/admin/members";
	    }
	

}
