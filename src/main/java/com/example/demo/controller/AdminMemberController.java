package com.example.demo.controller;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.dao.DuplicateKeyException;
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
				@RequestParam("mail") String mail,
				@RequestParam("point") Integer point,
				RedirectAttributes redirectAttributes) {

			// 自分以外の会員(削除済みを含む)が、同じメールを使っていたらエラー
			if (adminMemberService.existsByMailExceptId(mail, id)) {
				return backToEditWithError(id, name, birthDate, mail, point, redirectAttributes);
			}

			// すれ違いで、データベースが重複を断った場合の備え
			try {
				adminMemberService.update(id, name, birthDate, mail, point);
			} catch (DuplicateKeyException e) {
				return backToEditWithError(id, name, birthDate, mail, point, redirectAttributes);
			}

			return "redirect:/admin/members?updated=true";   // 更新後は一覧を開き直す
		}

		// エラー文と、入力した内容を持って、一覧に戻る(更新モーダルを開き直す)
		private String backToEditWithError(Integer id, String name, LocalDate birthDate,
		                                   String mail, Integer point,
		                                   RedirectAttributes redirectAttributes) {

			redirectAttributes.addFlashAttribute("editErrors", List.of(DUPLICATE_MAIL_MESSAGE));
			redirectAttributes.addFlashAttribute("editId", id);
			redirectAttributes.addFlashAttribute("editName", name);
			redirectAttributes.addFlashAttribute("editBirthDate", birthDate);
			redirectAttributes.addFlashAttribute("editMail", mail);
			redirectAttributes.addFlashAttribute("editPoint", point);

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
		public String createMember(@Valid RegisterForm form,
		                           BindingResult result,
		                           RedirectAttributes redirectAttributes) {

			// メールの入力チェックにエラーが無いときだけ、重複を調べる(削除済みの会員も含む)
			if (!result.hasFieldErrors("mail")
					&& registerService.existsByMail(form.getMail())) {
				result.rejectValue("mail", "duplicate", DUPLICATE_MAIL_MESSAGE);
			}

			// エラーがあれば、モーダルを開き直して表示する
			if (result.hasErrors()) {
				return backWithErrors(form, result, redirectAttributes);
			}

			// すれ違いで、データベースが重複を断った場合の備え
			try {
				registerService.register(form);
			} catch (DuplicateKeyException e) {
				result.rejectValue("mail", "duplicate", DUPLICATE_MAIL_MESSAGE);
				return backWithErrors(form, result, redirectAttributes);
			}

			return "redirect:/admin/members?created=true";
		}

		// エラー文と、入力した内容(パスワード以外)を持って、一覧に戻る
		private String backWithErrors(RegisterForm form,
		                              BindingResult result,
		                              RedirectAttributes redirectAttributes) {

			List<String> errors = result.getAllErrors().stream()
					.map(e -> e.getDefaultMessage())
					.toList();

			redirectAttributes.addFlashAttribute("createErrors", errors);
			redirectAttributes.addFlashAttribute("createName", form.getName());
			redirectAttributes.addFlashAttribute("createBirthDate", form.getBirthDate());
			redirectAttributes.addFlashAttribute("createMail", form.getMail());

			return "redirect:/admin/members";
		}
	

}
