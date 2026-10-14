
package com.example.demo.controller;

import java.time.LocalDate;
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
import com.example.demo.entity.ShopAdmin;
import com.example.demo.entity.Stores;
import com.example.demo.mapper.ReservationMapper;
import com.example.demo.mapper.ShopAdminMapper;
import com.example.demo.mapper.StoresMapper;

@Controller
public class ReservationManagementController {

    private final ReservationMapper reservationMapper;
    private final StoresMapper storesMapper;
    private final ShopAdminMapper shopAdminMapper;

    private static final List<String> ALLOWED_STATUSES =
            List.of("予約済み", "来店済み", "キャンセル");

    public ReservationManagementController(
            ReservationMapper reservationMapper,
            StoresMapper storesMapper,
            ShopAdminMapper shopAdminMapper) {

        this.reservationMapper = reservationMapper;
        this.storesMapper = storesMapper;
        this.shopAdminMapper = shopAdminMapper;
    }

    // =====================================================
    // モール管理者：予約一覧・検索
    // GET /admin/reservations
    // =====================================================
    @GetMapping("/admin/reservations")
    public String mallAdminReservations(
            Authentication authentication,
            @RequestParam(required = false) Integer reservationId,
            @RequestParam(required = false) String reservationDate,
            Model model) {

        requireRole(authentication, "ROLE_ADMIN");

        LocalDate searchDate = parseSearchDate(reservationDate);

        List<Reservations> reservations =
                reservationMapper.searchAllForAdmin(
                        reservationId, searchDate);

        prepareModel(
                model,
                reservations,
                "/admin/reservations",
                true,
                null);

        model.addAttribute("stores", storesMapper.selectList());
        model.addAttribute("searchReservationId", reservationId);
        model.addAttribute("searchReservationDate", reservationDate);

        return "admin/reservations";
    }

    // =====================================================
    // 店舗管理者：自店舗の予約一覧・検索
    // GET /shop-admin/{storeId}/reservations
    // =====================================================
    @GetMapping("/shop-admin/{storeId}/reservations")
    public String shopAdminReservations(
            @PathVariable int storeId,
            Authentication authentication,
            @RequestParam(required = false) Integer reservationId,
            @RequestParam(required = false) String reservationDate,
            Model model) {

        int assignedStoreId =
                requireShopAdminStoreId(authentication);

        if (assignedStoreId != storeId) {
            throw new AccessDeniedException(
                    "担当店舗以外にはアクセスできません。");
        }

        Stores store = storesMapper.selectById(assignedStoreId);

        if (store == null) {
            throw new AccessDeniedException(
                    "担当店舗が見つかりません。");
        }

        LocalDate searchDate = parseSearchDate(reservationDate);

        List<Reservations> reservations =
                reservationMapper.searchForStoreAdmin(
                        assignedStoreId,
                        reservationId,
                        searchDate);

        prepareModel(
                model,
                reservations,
                "/shop-admin/" + assignedStoreId + "/reservations",
                false,
                store.getName());

        model.addAttribute("stores", List.of(store));
        model.addAttribute("searchReservationId", reservationId);
        model.addAttribute("searchReservationDate", reservationDate);

        return "admin/reservations";
    }

    // =====================================================
    // モール管理者：予約追加
    // =====================================================
    @PostMapping("/admin/reservations/add")
    public String addReservationByMallAdmin(
            Authentication authentication,
            @RequestParam int storeId,
            @RequestParam String reservationDateTime,
            @RequestParam int number,
            @RequestParam(required = false) Integer memberId,
            @RequestParam(required = false) Integer userId,
            RedirectAttributes redirectAttributes) {

        requireRole(authentication, "ROLE_ADMIN");

        try {
            createReservation(
                    storeId,
                    reservationDateTime,
                    number,
                    memberId,
                    userId);

            redirectAttributes.addFlashAttribute(
                    "successMessage", "予約を追加しました。");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());
        }

        return "redirect:/admin/reservations";
    }

    // =====================================================
    // 店舗管理者：自店舗の予約追加
    // =====================================================
    @PostMapping("/shop-admin/{storeId}/reservations/add")
    public String addReservationByShopAdmin(
            @PathVariable int storeId,
            Authentication authentication,
            @RequestParam String reservationDateTime,
            @RequestParam int number,
            @RequestParam(required = false) Integer memberId,
            @RequestParam(required = false) Integer userId,
            RedirectAttributes redirectAttributes) {

        int assignedStoreId =
                requireShopAdminStoreId(authentication);

        if (assignedStoreId != storeId) {
            throw new AccessDeniedException(
                    "担当店舗以外には予約を追加できません。");
        }

        try {
            createReservation(
                    assignedStoreId,
                    reservationDateTime,
                    number,
                    memberId,
                    userId);

            redirectAttributes.addFlashAttribute(
                    "successMessage", "予約を追加しました。");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());
        }

        return "redirect:/shop-admin/" + assignedStoreId
                + "/reservations";
    }

    // =====================================================
    // モール管理者：予約日時・人数の編集
    // =====================================================
    @PostMapping("/admin/reservations/edit")
    public String editReservationByMallAdmin(
            Authentication authentication,
            @RequestParam int reservationId,
            @RequestParam String reservationDateTime,
            @RequestParam int number,
            RedirectAttributes redirectAttributes) {

        requireRole(authentication, "ROLE_ADMIN");

        try {
            LocalDateTime dateTime =
                    parseReservationDateTime(reservationDateTime);

            validateReservation(dateTime, number);

            int updated = reservationMapper.updateForAdmin(
                    reservationId, dateTime, number);

            if (updated == 0) {
                throw new IllegalArgumentException(
                        "予約が存在しないか、キャンセル済みです。");
            }

            redirectAttributes.addFlashAttribute(
                    "successMessage", "予約内容を更新しました。");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());
        }

        return "redirect:/admin/reservations";
    }

    // =====================================================
    // 店舗管理者：自店舗の予約日時・人数の編集
    // =====================================================
    @PostMapping("/shop-admin/{storeId}/reservations/edit")
    public String editReservationByShopAdmin(
            @PathVariable int storeId,
            Authentication authentication,
            @RequestParam int reservationId,
            @RequestParam String reservationDateTime,
            @RequestParam int number,
            RedirectAttributes redirectAttributes) {

        int assignedStoreId =
                requireShopAdminStoreId(authentication);

        if (assignedStoreId != storeId) {
            throw new AccessDeniedException(
                    "担当店舗以外の予約は編集できません。");
        }

        try {
            LocalDateTime dateTime =
                    parseReservationDateTime(reservationDateTime);

            validateReservation(dateTime, number);

            int updated = reservationMapper.updateForStore(
                    reservationId,
                    assignedStoreId,
                    dateTime,
                    number);

            if (updated == 0) {
                throw new IllegalArgumentException(
                        "予約が存在しない、担当店舗外、またはキャンセル済みです。");
            }

            redirectAttributes.addFlashAttribute(
                    "successMessage", "予約内容を更新しました。");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());
        }

        return "redirect:/shop-admin/" + assignedStoreId
                + "/reservations";
    }

    // =====================================================
    // モール管理者：ステータス変更
    // =====================================================
    @PostMapping("/admin/reservations/status")
    public String changeStatusByMallAdmin(
            Authentication authentication,
            @RequestParam int reservationId,
            @RequestParam String status,
            RedirectAttributes redirectAttributes) {

        requireRole(authentication, "ROLE_ADMIN");

        try {
            validateStatus(status);

            int updated = reservationMapper.updateStatusForAdmin(
                    reservationId, status);

            if (updated == 0) {
                throw new IllegalArgumentException(
                        "対象の予約が見つかりません。");
            }

            redirectAttributes.addFlashAttribute(
                    "successMessage", "ステータスを変更しました。");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());
        }

        return "redirect:/admin/reservations";
    }

    // =====================================================
    // 店舗管理者：自店舗のステータス変更
    // =====================================================
    @PostMapping("/shop-admin/{storeId}/reservations/status")
    public String changeStatusByShopAdmin(
            @PathVariable int storeId,
            Authentication authentication,
            @RequestParam int reservationId,
            @RequestParam String status,
            RedirectAttributes redirectAttributes) {

        int assignedStoreId =
                requireShopAdminStoreId(authentication);

        if (assignedStoreId != storeId) {
            throw new AccessDeniedException(
                    "担当店舗以外の予約は変更できません。");
        }

        try {
            validateStatus(status);

            int updated = reservationMapper.updateStatusForStore(
                    reservationId,
                    assignedStoreId,
                    status);

            if (updated == 0) {
                throw new IllegalArgumentException(
                        "担当店舗の対象予約が見つかりません。");
            }

            redirectAttributes.addFlashAttribute(
                    "successMessage", "ステータスを変更しました。");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());
        }

        return "redirect:/shop-admin/" + assignedStoreId
                + "/reservations";
    }

    // =====================================================
    // 共通：予約追加
    // =====================================================
    private void createReservation(
            int storeId,
            String reservationDateTime,
            int number,
            Integer memberId,
            Integer userId) {

        Stores store = storesMapper.selectById(storeId);

        if (store == null) {
            throw new IllegalArgumentException(
                    "指定された店舗が存在しないか、利用できません。");
        }

        // 会員IDとゲストユーザーIDは同時に指定できない
        if (memberId != null && userId != null) {
            throw new IllegalArgumentException(
                    "会員IDとゲストユーザーIDは同時に指定できません。");
        }

        // 会員IDが入力されている場合は存在確認
        if (memberId != null
                && reservationMapper.countMemberId(memberId) == 0) {
            throw new IllegalArgumentException(
                    "指定された会員IDが存在しません。");
        }

        // ゲストユーザーIDが入力されている場合は存在確認
        if (userId != null
                && reservationMapper.countUserId(userId) == 0) {
            throw new IllegalArgumentException(
                    "指定されたゲストユーザーIDが存在しません。");
        }

        LocalDateTime dateTime =
                parseReservationDateTime(reservationDateTime);

        validateReservation(dateTime, number);

        Reservations reservation = new Reservations();
        reservation.setMemberId(memberId);
        reservation.setUserId(userId);
        reservation.setStoreId(storeId);
        reservation.setReservationDate(dateTime);
        reservation.setNumber(number);
        reservation.setStatus("予約済み");
        reservation.setCreatedAt(LocalDateTime.now());

        reservationMapper.insertReservation(reservation);
    }

    // =====================================================
    // 共通：画面表示用データ
    // =====================================================
    private void prepareModel(
            Model model,
            List<Reservations> reservations,
            String basePath,
            boolean mallAdmin,
            String currentStoreName) {

        List<Stores> stores = storesMapper.selectList();

        Map<Integer, String> storeNames = new LinkedHashMap<>();

        for (Stores store : stores) {
            storeNames.put(store.getId(), store.getName());
        }

        model.addAttribute("reservations", reservations);
        model.addAttribute("storeNames", storeNames);
        model.addAttribute("basePath", basePath);
        model.addAttribute("mallAdmin", mallAdmin);
        model.addAttribute("currentStoreName", currentStoreName);
        model.addAttribute("reservationStatuses", ALLOWED_STATUSES);
    }

    // =====================================================
    // 共通：権限チェック
    // =====================================================
    private void requireRole(
            Authentication authentication,
            String requiredRole) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getAuthorities().stream()
                    .noneMatch(a -> requiredRole.equals(
                            a.getAuthority()))) {

            throw new AccessDeniedException(
                    "この操作を行う権限がありません。");
        }
    }

    // =====================================================
    // 店舗管理者のログインIDから担当店舗IDを取得
    // Authentication.getName() が shop_admins.id である前提
    // =====================================================
    private int requireShopAdminStoreId(
            Authentication authentication) {

        requireRole(authentication, "ROLE_SHOP_ADMIN");

        final int shopAdminId;

        try {
            shopAdminId =
                    Integer.parseInt(authentication.getName());

        } catch (NumberFormatException
                | NullPointerException e) {
            throw new AccessDeniedException(
                    "店舗管理者のログインIDを確認できません。");
        }

        ShopAdmin shopAdmin =
                shopAdminMapper.findById(shopAdminId);

        if (shopAdmin == null) {
            throw new AccessDeniedException(
                    "店舗管理者が見つかりません。");
        }

        // getStoreId() が Integer 型でも int 型でも扱えるようにする
        Integer assignedStoreId = shopAdmin.getStoreId();

        if (assignedStoreId == null || assignedStoreId <= 0) {
            throw new AccessDeniedException(
                    "担当店舗が設定されていません。");
        }

        return assignedStoreId;
    }

    // =====================================================
    // 共通：検索日チェック
    // =====================================================
    private LocalDate parseSearchDate(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(value);

        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "検索日はYYYY-MM-DD形式で指定してください。");
        }
    }

    // =====================================================
    // 共通：予約日時チェック
    // =====================================================
    private LocalDateTime parseReservationDateTime(String value) {

        try {
            return LocalDateTime.parse(value);

        } catch (DateTimeParseException
                | NullPointerException e) {
            throw new IllegalArgumentException(
                    "予約日時を正しく入力してください。");
        }
    }

    // =====================================================
    // 共通：予約内容チェック
    // =====================================================
    private void validateReservation(
            LocalDateTime reservationDateTime,
            int number) {

        if (reservationDateTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "過去の予約はできません。");
        }

        if (number < 1 || number > 10) {
            throw new IllegalArgumentException(
                    "予約人数は1人から10人まで指定できます。");
        }
    }

    // =====================================================
    // 共通：ステータスチェック
    // =====================================================
    private void validateStatus(String status) {

        if (!ALLOWED_STATUSES.contains(status)) {
            throw new IllegalArgumentException(
                    "指定されたステータスは使用できません。");
        }
    }
}
