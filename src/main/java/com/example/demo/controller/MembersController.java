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

@Controller
@RequestMapping("/members")
@RequiredArgsConstructor
public class MembersController {

    private final MembersService membersService;

    /** 会員情報画面を表示する */
    @GetMapping("/showEdit")
    public String showEdit(Principal principal, Model model) {
        Members member = membersService.findByLoginId(principal.getName());
        
        if (member == null) {
            return "redirect:/shops";
        }
        
        model.addAttribute("memberEditForm", membersService.toForm(member));
        addPointInfo(member, model);   // ★追加：ポイントカード用の情報を渡す
        return "members/showEdit";
    }
    

    /** 会員情報を変更する（「変更」ボタン） */
    @PostMapping("/showEdit")
    public String update(@Validated @ModelAttribute("memberEditForm") MemberEditForm editForm,
                         BindingResult result,
                         Principal principal,
                         Model model,                         // ★追加：引数に Model
                         RedirectAttributes redirectAttributes) {
        
    	Members member = membersService.findByLoginId(principal.getName());
        
    	if (member == null) {
            return "redirect:/shops";
        }

        if (result.hasErrors()) {
            addPointInfo(member, model);   // ★追加：エラーで同じ画面を出すときも必要
            return "members/showEdit";
        }

        try {
            membersService.update(member.getId(), editForm);
        } catch (DuplicateKeyException e) {
            result.rejectValue("mail", "duplicate", "このメールアドレスはすでに使われています");
            addPointInfo(member, model);   // ★追加
            return "members/showEdit";
        }

        redirectAttributes.addFlashAttribute("message", "会員情報を変更しました");
        
        return "redirect:/members/showEdit";
    }

    /** 退会する（確認モーダルの「はい」ボタン） */
    @PostMapping("/delete")
    public String delete(Principal principal, HttpServletRequest request) throws ServletException {
        
    	Members member = membersService.findByLoginId(principal.getName());
        
    	if (member != null) {
            membersService.delete(member.getId());
        }
    	
        request.logout();
        
        return "redirect:/shops";
    }

    /**
     * ★追加：ガチャを回す（モーダルの「ガチャを回す」ボタン）
     * 結果はフラッシュ属性で会員情報画面に渡し、画面側で結果モーダルを開く
     */
    @PostMapping("/gacha")
    public String gacha(Principal principal, RedirectAttributes redirectAttributes) {
        Members member = membersService.findByLoginId(principal.getName());
        
        if (member == null) {
            return "redirect:/shops";
        }

        int rank = membersService.playGacha(member.getId());

        if (rank == 0) {
            // 今日の上限に達していた
            redirectAttributes.addFlashAttribute("gachaError", "今日はもうガチャを回せません。また明日遊んでください");
        } else {
            redirectAttributes.addFlashAttribute("gachaResultRank", rank);
            redirectAttributes.addFlashAttribute("gachaResultPoint", membersService.getGachaPoint(rank));
        }
        return "redirect:/members/showEdit";
    }

    /**
     * ★追加：ポイントカードに表示する情報を Model に入れる
     * （showEdit を表示する3か所で使うので、1つのメソッドにまとめた）
     */
    private void addPointInfo(Members member, Model model) {
        Integer memberId = member.getId();
        int todayCount = membersService.getTodayGachaCount(memberId);

        model.addAttribute("totalPoint", member.getPoint());                                     // 合計
        model.addAttribute("gachaTotal", membersService.getPointByReason(memberId, "GACHA"));       // 内訳：ゲーム
        model.addAttribute("reviewTotal", membersService.getPointByReason(memberId, "REVIEW"));     // 内訳：口コミ
        model.addAttribute("reservationTotal", membersService.getPointByReason(memberId, "RESERVATION")); // 内訳：予約
        model.addAttribute("gachaRemaining", MembersService.GACHA_LIMIT_PER_DAY - todayCount);    // 今日あと何回
        model.addAttribute("gachaLimit", MembersService.GACHA_LIMIT_PER_DAY); // 1日の上限
        
    }
}