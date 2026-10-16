package com.example.demo.controller;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.mapper.ReservationMapper;

@Controller
public class AdminReservationDeleteController {   

    private final ReservationMapper reservationMapper;

    public AdminReservationDeleteController(      
            ReservationMapper reservationMapper) {
        this.reservationMapper = reservationMapper;
    }

    // モール管理者:予約の削除(論理削除)
    @PostMapping("/admin/reservations/delete")
    public String delete(
            Authentication authentication,
            @RequestParam int reservationId,
            @RequestParam(required = false) Integer memberId,
            RedirectAttributes redirectAttributes) {

        requireMallAdmin(authentication);

        int deleted = reservationMapper.logicalDeleteForAdmin(reservationId);

        if (deleted == 0) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "対象の予約が見つからないか、すでに削除されています。");
        } else {
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "予約ID：" + reservationId + " を削除しました。");
        }

        // memberId が付いていれば、その会員の履歴画面へ戻る
        if (memberId != null) {
            return "redirect:/admin/members/" + memberId + "/reservations";
        }
        return "redirect:/admin/reservations";
    }

    // 権限チェック:モール管理者以外は拒否する
    private void requireMallAdmin(Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getAuthorities().stream()
                        .noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            throw new AccessDeniedException(
                    "この操作を行う権限がありません。");
        }
    }
}