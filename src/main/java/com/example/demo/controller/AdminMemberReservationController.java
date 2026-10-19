package com.example.demo.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.entity.Reservations;
import com.example.demo.entity.Stores;
import com.example.demo.mapper.ReservationMapper;
import com.example.demo.mapper.StoresMapper;

@Controller
public class AdminMemberReservationController {

    private final ReservationMapper reservationMapper;
    private final StoresMapper storesMapper;

    public AdminMemberReservationController(
            ReservationMapper reservationMapper,
            StoresMapper storesMapper) {
        this.reservationMapper = reservationMapper;
        this.storesMapper = storesMapper;
    }

    // 会員の予約履歴の表示
    @GetMapping("/admin/members/{memberId}/reservations")
    public String show(
            @PathVariable int memberId,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        requireMallAdmin(authentication);

        if (reservationMapper.countMemberId(memberId) == 0) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "指定された会員が存在しません。");
            return "redirect:/admin/members";
        }

        List<Reservations> reservations =
                reservationMapper.findByMemberId(memberId);

        List<Stores> stores = storesMapper.selectList();
        Map<Integer, String> storeNames = new LinkedHashMap<>();
        for (Stores store : stores) {
            storeNames.put(store.getId(), store.getName());
        }

        model.addAttribute("memberId", memberId);
        model.addAttribute("reservations", reservations);
        model.addAttribute("stores", stores);
        model.addAttribute("storeNames", storeNames);

        return "admin/member-reservations";
    }

    // 会員の予約を追加
    @PostMapping("/admin/members/{memberId}/reservations/add")
    public String add(
            @PathVariable int memberId,
            Authentication authentication,
            @RequestParam int storeId,
            @RequestParam String reservationDateTime,
            @RequestParam int number,
            RedirectAttributes redirectAttributes) {

        requireMallAdmin(authentication);

        try {
            if (reservationMapper.countMemberId(memberId) == 0) {
                throw new IllegalArgumentException("指定された会員が存在しません。");
            }
            if (storesMapper.selectById(storeId) == null) {
                throw new IllegalArgumentException("指定された店舗が存在しません。");
            }
            if (number < 1 || number > 10) {
                throw new IllegalArgumentException("人数は1~10人で入力してください。");
            }

            LocalDateTime dateTime;
            try {
                dateTime = LocalDateTime.parse(reservationDateTime);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("予約日時を正しく入力してください。");
            }
            if (dateTime.isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("新規予約に過去の日時は指定できません。");
            }

            Reservations reservation = new Reservations();
            reservation.setMemberId(memberId);
            reservation.setUserId(null);
            reservation.setStoreId(storeId);
            reservation.setReservationDate(dateTime);
            reservation.setNumber(number);
            reservation.setStatus("予約済み");
            reservation.setCreatedAt(LocalDateTime.now());

            reservationMapper.insertReservation(reservation);

            redirectAttributes.addFlashAttribute(
                    "successMessage", "予約を追加しました。");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());
        }

        return "redirect:/admin/members/" + memberId + "/reservations";
    }

    // モール管理者以外は拒否する
    private void requireMallAdmin(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getAuthorities().stream()
                        .noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            throw new AccessDeniedException("この操作を行う権限がありません。");
        }
    }
}