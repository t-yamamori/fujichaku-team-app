package com.example.demo.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.entity.Reservations;
import com.example.demo.entity.ShopAdmin;
import com.example.demo.entity.Stores;
import com.example.demo.entity.Users;
import com.example.demo.mapper.ReservationMapper;
import com.example.demo.mapper.ShopAdminMapper;
import com.example.demo.mapper.StoresMapper;
import com.example.demo.mapper.UsersMapper;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/shop-reservations")
@RequiredArgsConstructor
public class ShopReservationAdminController {

    private final ReservationMapper reservationMapper;
    private final ShopAdminMapper shopAdminMapper;
    private final StoresMapper storesMapper;
    private final UsersMapper usersMapper;

    /**
     * ログイン中の店舗管理者から担当店舗を取得する。
     * URLパラメータの店舗IDは信用しない。
     */
    private ShopAdmin getLoginShopAdmin(Principal principal) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getAuthorities().stream()
                    .noneMatch(a ->
                        "ROLE_SHOP_ADMIN".equals(a.getAuthority()))) {
            return null;
        }

        if (principal == null) {
            return null;
        }

        try {
            int shopAdminId = Integer.parseInt(principal.getName());
            return shopAdminMapper.findById(shopAdminId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 予約一覧
     */
    @GetMapping
    public String index(
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {

        ShopAdmin shopAdmin = getLoginShopAdmin(principal);

        if (shopAdmin == null || shopAdmin.getStoreId() == null) {
            return "redirect:/adminLogin?error";
        }

        Stores store = storesMapper.selectById(shopAdmin.getStoreId());

        if (store == null) {
            return "redirect:/adminLogin?error";
        }

        List<Reservations> reservations =
                reservationMapper.findByStoreId(shopAdmin.getStoreId());

        Map<Integer, Users> usersById = new HashMap<>();

        for (Reservations reservation : reservations) {
            if (reservation.getUserId() != null) {
                Users user =
                        usersMapper.findById(reservation.getUserId());

                if (user != null) {
                    usersById.put(reservation.getId(), user);
                }
            }
        }

        model.addAttribute("store", store);
        model.addAttribute("reservations", reservations);
        model.addAttribute("usersById", usersById);

        return "admin/shop-reservations";
    }

    /**
     * 店舗管理者による予約の新規追加。
     * 今回は非会員の予約として登録する。
     */
    @PostMapping("/create")
    public String create(
            Principal principal,
            @RequestParam String name,
            @RequestParam String mail,
            @RequestParam String reservationDateTime,
            @RequestParam Integer number,
            RedirectAttributes redirectAttributes) {

        ShopAdmin shopAdmin = getLoginShopAdmin(principal);

        if (shopAdmin == null || shopAdmin.getStoreId() == null) {
            return "redirect:/adminLogin?error";
        }

        try {
            String trimmedName = name == null ? "" : name.trim();
            String trimmedMail = mail == null ? "" : mail.trim();

            if (trimmedName.isEmpty() || trimmedMail.isEmpty()) {
                redirectAttributes.addFlashAttribute(
                        "error", "氏名とメールアドレスを入力してください。");
                return "redirect:/admin/shop-reservations";
            }

            if (number == null || number < 1 || number > 10) {
                redirectAttributes.addFlashAttribute(
                        "error", "人数は1～10人で入力してください。");
                return "redirect:/admin/shop-reservations";
            }

            LocalDateTime dateTime =
                    LocalDateTime.parse(reservationDateTime);

            if (dateTime.isBefore(LocalDateTime.now())) {
                redirectAttributes.addFlashAttribute(
                        "error", "過去の日時は登録できません。");
                return "redirect:/admin/shop-reservations";
            }

            Stores store = storesMapper.selectById(shopAdmin.getStoreId());

            if (store == null) {
                return "redirect:/adminLogin?error";
            }

            Users user = usersMapper.findByMail(trimmedMail);

            if (user == null) {
                user = new Users();
                user.setName(trimmedName);
                user.setMail(trimmedMail);
                usersMapper.insert(user);
            }

            Reservations reservation = new Reservations();
            reservation.setStoreId(shopAdmin.getStoreId());
            reservation.setUserId(user.getId());
            reservation.setMemberId(null);
            reservation.setReservationDate(dateTime);
            reservation.setNumber(number);
            reservation.setStatus("予約済み");
            reservation.setCreatedAt(LocalDateTime.now());

            reservationMapper.insertReservation(reservation);

            redirectAttributes.addFlashAttribute(
                    "message", "予約を追加しました。");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "予約を登録できませんでした。入力内容とデータベースを確認してください。");
        }

        return "redirect:/admin/shop-reservations";
    }

    /**
     * 予約日時・人数の変更
     */
    @PostMapping("/update")
    public String update(
            Principal principal,
            @RequestParam Integer reservationId,
            @RequestParam String reservationDateTime,
            @RequestParam Integer number,
            RedirectAttributes redirectAttributes) {

        ShopAdmin shopAdmin = getLoginShopAdmin(principal);

        if (shopAdmin == null || shopAdmin.getStoreId() == null) {
            return "redirect:/adminLogin?error";
        }

        try {
            LocalDateTime dateTime =
                    LocalDateTime.parse(reservationDateTime);

            if (dateTime.isBefore(LocalDateTime.now())) {
                redirectAttributes.addFlashAttribute(
                        "error", "過去の日時には変更できません。");
                return "redirect:/admin/shop-reservations";
            }

            if (number == null || number < 1 || number > 10) {
                redirectAttributes.addFlashAttribute(
                        "error", "人数は1～10人で入力してください。");
                return "redirect:/admin/shop-reservations";
            }

            Reservations reservation =
                    reservationMapper.findById(reservationId);

            if (reservation == null
                    || reservation.getStoreId() == null
                    || !reservation.getStoreId().equals(shopAdmin.getStoreId())) {
                redirectAttributes.addFlashAttribute(
                        "error", "対象の予約が見つかりません。");
                return "redirect:/admin/shop-reservations";
            }

            if ("キャンセル".equals(reservation.getStatus())) {
                redirectAttributes.addFlashAttribute(
                        "error", "キャンセル済みの予約は編集できません。");
                return "redirect:/admin/shop-reservations";
            }

            reservationMapper.updateForStore(
                    reservationId,
                    shopAdmin.getStoreId(),
                    dateTime,
                    number);

            redirectAttributes.addFlashAttribute(
                    "message", "予約を更新しました。");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "予約を更新できませんでした。入力内容とデータベースを確認してください。");
        }

        return "redirect:/admin/shop-reservations";
    }

    /**
     * 予約キャンセル
     */
    @PostMapping("/cancel")
    public String cancel(
            Principal principal,
            @RequestParam Integer reservationId,
            RedirectAttributes redirectAttributes) {

        ShopAdmin shopAdmin = getLoginShopAdmin(principal);

        if (shopAdmin == null || shopAdmin.getStoreId() == null) {
            return "redirect:/adminLogin?error";
        }

        Reservations reservation =
                reservationMapper.findById(reservationId);

        if (reservation == null
                || reservation.getStoreId() == null
                || !reservation.getStoreId().equals(shopAdmin.getStoreId())) {
            redirectAttributes.addFlashAttribute(
                    "error", "対象の予約が見つかりません。");
            return "redirect:/admin/shop-reservations";
        }

        if (!"キャンセル".equals(reservation.getStatus())) {
            reservationMapper.updateStatusForStore(
                    reservationId,
                    shopAdmin.getStoreId(),
                    "キャンセル");
        }

        redirectAttributes.addFlashAttribute(
                "message", "予約をキャンセルしました。");

        return "redirect:/admin/shop-reservations";
    }
}
