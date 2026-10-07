package com.example.demo.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.demo.entity.Members;
import com.example.demo.service.AdminMemberService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AdminMemberController {
	 private final AdminMemberService adminMemberService;

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
	

}
