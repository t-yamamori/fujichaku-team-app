package com.example.demo.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.entity.Reservations;
import com.example.demo.entity.Reviews;
import com.example.demo.entity.ShopAdmin;
import com.example.demo.entity.Stores;
import com.example.demo.entity.Users;
import com.example.demo.mapper.ReservationMapper;
import com.example.demo.mapper.ReviewsMapper;
import com.example.demo.mapper.ShopAdminMapper;
import com.example.demo.mapper.StoresMapper;
import com.example.demo.mapper.UsersMapper;
import com.example.demo.service.ReviewService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ShopDashboardController {

    private final StoresMapper storesMapper;
    private final ReviewService reviewService;
    private final ReviewsMapper reviewsMapper;
    private final ReservationMapper reservationMapper;
    private final ShopAdminMapper shopAdminMapper;
    private final UsersMapper usersMapper;

    private static final LocalTime RESERVE_FIRST = LocalTime.of(11, 0);
    private static final LocalTime RESERVE_LAST = LocalTime.of(21, 30);

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private static final String STATUS_RESERVED = "予約済み";

    /*
     * 店舗管理者向け店舗詳細
     *
     * GET：店舗詳細・口コミ・予約履歴を表示
     * POST action=delete：口コミ削除
     * POST action=updateReservation：既存予約編集
     */
    @RequestMapping(
            value = "/shop-admin/{storeId:\\d+}",
            method = { RequestMethod.GET, RequestMethod.POST })
    public String dashboard(
            @PathVariable("storeId") int storeId,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "reviewId", required = false) Integer reviewId,
            @RequestParam(value = "reservationId", required = false) Integer reservationId,
            @RequestParam(value = "reservationDay", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate reservationDay,
            @RequestParam(value = "reservationTime", required = false)
            @DateTimeFormat(pattern = "HH:mm") LocalTime reservationTime,
            @RequestParam(value = "number", required = false) Integer number,
            @RequestParam(value = "status", required = false) String status,
            HttpMethod method,
            Model model,
            RedirectAttributes redirectAttributes) {

        ShopAdmin shopAdmin = getLoginShopAdmin();

        if (shopAdmin == null
                || shopAdmin.getStoreId() == null
                || shopAdmin.getStoreId() != storeId) {
            return "redirect:/shops";
        }

        Stores store = storesMapper.selectById(storeId);

        if (store == null) {
            return "redirect:/shops";
        }

        if (method == HttpMethod.POST) {
            if ("delete".equals(action)) {
                deleteReviewByShopAdmin(
                        storeId, reviewId, redirectAttributes);

            } else if ("updateReservation".equals(action)) {
                LocalDateTime reservationDate =
                        reservationDay != null && reservationTime != null
                                ? LocalDateTime.of(
                                        reservationDay, reservationTime)
                                : null;

                updateReservation(
                        storeId,
                        reservationId,
                        reservationDate,
                        number,
                        status,
                        redirectAttributes);

            } else {
                redirectAttributes.addFlashAttribute(
                        "errorMessage", "不正な操作です。");
            }

            String anchor =
                    "updateReservation".equals(action) ? "#rsv" : "";

            return "redirect:/shop-admin/" + storeId + anchor;
        }

        List<Reviews> reviews =
                reviewService.findAllByStoreId(storeId);

        List<Reservations> storeReservations =
                reservationMapper.findByStoreId(storeId);

        model.addAttribute("store", store);
        model.addAttribute("reviews", reviews);
        model.addAttribute(
                "averageGrade", reviewsMapper.selectAverageGrade(storeId));
        model.addAttribute(
                "reviewCount", reviewsMapper.countByStoreId(storeId));
        model.addAttribute("storeReservations", storeReservations);

        model.addAttribute("isMember", false);
        model.addAttribute("isShopAdmin", true);
        model.addAttribute("loginId", null);
        model.addAttribute("reservations", List.of());

        return "shops/detail";
    }

    /*
     * 店舗管理者：店舗詳細画面から予約を追加
     *
     * モール管理者の createReservation と同じ登録ルールを適用する。
     * ゲスト氏名・メールアドレスが入力された場合は、
     * 既存ユーザーを検索し、存在しなければ新規登録する。
     */
    @PostMapping("/shop-admin/{storeId}/reservations/add-detail")
    @Transactional
    public String addReservationFromDetail(
            @PathVariable int storeId,
            @RequestParam String reservationDateTime,
            @RequestParam(required = false) Integer number,
            @RequestParam(required = false) Integer memberId,
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) String guestName,
            @RequestParam(required = false) String guestMail,
            RedirectAttributes redirectAttributes) {

        ShopAdmin shopAdmin = getLoginShopAdmin();

        if (shopAdmin == null
                || shopAdmin.getStoreId() == null
                || shopAdmin.getStoreId() != storeId) {
            return "redirect:/shops";
        }

        try {
            createReservation(
                    storeId,
                    reservationDateTime,
                    number,
                    memberId,
                    userId,
                    guestName,
                    guestMail);

            redirectAttributes.addFlashAttribute(
                    "rsvMessage", "予約を追加しました。");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "rsvErrorMessage", e.getMessage());
        }

        return "redirect:/shop-admin/" + storeId + "#rsv";
    }

    /*
     * 店舗管理者のログアウト
     */
    @PostMapping("/shop-admin/logout")
    public String logout(
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response,
            RedirectAttributes redirectAttributes) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        new org.springframework.security.web.authentication.logout
                .SecurityContextLogoutHandler()
                .logout(request, response, authentication);

        redirectAttributes.addFlashAttribute(
                "message", "ログアウトしました。");

        return "redirect:/shops";
    }

    /*
     * 店舗管理者：口コミ削除
     */
    private void deleteReviewByShopAdmin(
            int storeId,
            Integer reviewId,
            RedirectAttributes redirectAttributes) {

        int count = reviewId != null
                ? reviewsMapper.deleteByIdAndStoreId(reviewId, storeId)
                : 0;

        if (count > 0) {
            redirectAttributes.addFlashAttribute(
                    "message", "口コミを削除しました");
        } else {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "削除できる口コミが見つかりませんでした。");
        }
    }

    /*
     * 店舗管理者：既存予約の編集
     */
    private void updateReservation(
            int storeId,
            Integer reservationId,
            LocalDateTime reservationDate,
            Integer number,
            String status,
            RedirectAttributes redirectAttributes) {

        if (reservationId == null || reservationDate == null) {
            redirectAttributes.addFlashAttribute(
                    "rsvErrorMessage", "予約日時を入力してください。");
            return;
        }

        if (number == null || number < 1) {
            redirectAttributes.addFlashAttribute(
                    "rsvErrorMessage", "人数は1名以上で入力してください。");
            return;
        }

        if (status == null || status.isBlank()) {
            redirectAttributes.addFlashAttribute(
                    "rsvErrorMessage", "予約状況を選択してください。");
            return;
        }

        LocalTime time = reservationDate.toLocalTime();

        if (time.getMinute() % 30 != 0) {
            redirectAttributes.addFlashAttribute(
                    "rsvErrorMessage",
                    "予約時刻は30分単位で選択してください。");
            return;
        }

        if (time.isBefore(RESERVE_FIRST) || time.isAfter(RESERVE_LAST)) {
            redirectAttributes.addFlashAttribute(
                    "rsvErrorMessage",
                    "予約時刻は11:00〜21:30の間で選択してください。");
            return;
        }

        int statusCount = reservationMapper.updateStatusForStore(
                reservationId, storeId, status);

        int dateCount = reservationMapper.updateForStore(
                reservationId, storeId, reservationDate, number);

        if (statusCount == 0) {
            redirectAttributes.addFlashAttribute(
                    "rsvErrorMessage",
                    "変更できる予約が見つかりませんでした。");
        } else if (dateCount == 0) {
            redirectAttributes.addFlashAttribute(
                    "rsvMessage",
                    "予約状況を変更しました。（キャンセルの予約は、日時・人数を変更できません）");
        } else {
            redirectAttributes.addFlashAttribute(
                    "rsvMessage", "予約内容を変更しました。");
        }
    }

    /*
     * 共通：予約追加
     *
     * ReservationManagementController の createReservation と同じ考え方で、
     * ユーザー確認・ゲスト登録・予約登録を行う。
     */
    private void createReservation(
            int storeId,
            String reservationDateTime,
            Integer number,
            Integer memberId,
            Integer userId,
            String guestName,
            String guestMail) {

        Stores store = storesMapper.selectById(storeId);

        if (store == null) {
            throw new IllegalArgumentException(
                    "指定された店舗が存在しないか、利用できません。");
        }

        if (memberId != null && userId != null) {
            throw new IllegalArgumentException(
                    "会員IDとゲストユーザーIDは同時に指定できません。");
        }

        // 会員IDの存在確認・退会確認
        if (memberId != null) {
            if (reservationMapper.countMemberId(memberId) == 0) {
                throw new IllegalArgumentException(
                        "指定された会員IDが存在しません。");
            }

            if (reservationMapper.countDeletedMemberId(memberId) > 0) {
                throw new IllegalArgumentException(
                        "退会済みです。予約できません。");
            }
        }

        // ゲストユーザーIDが直接指定された場合の存在確認
        if (userId != null && reservationMapper.countUserId(userId) == 0) {
            throw new IllegalArgumentException(
                    "指定されたゲストユーザーIDが存在しません。");
        }

        LocalDateTime dateTime;

        try {
            dateTime = LocalDateTime.parse(reservationDateTime);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException(
                    "予約日時を正しく入力してください。");
        }

        validateNewReservation(dateTime, number);

        // 会員ID・ゲストユーザーIDが未入力なら、ゲスト氏名とメールから登録
        if (memberId == null && userId == null) {
            String normalizedGuestName =
                    guestName == null ? "" : guestName.trim();

            String normalizedGuestMail =
                    guestMail == null ? "" : guestMail.trim();

            if (normalizedGuestName.isEmpty()) {
                throw new IllegalArgumentException(
                        "ゲストの氏名を入力してください。");
            }

            if (normalizedGuestMail.isEmpty()) {
                throw new IllegalArgumentException(
                        "ゲストのメールアドレスを入力してください。");
            }

            if (normalizedGuestMail.length() > 255) {
                throw new IllegalArgumentException(
                        "メールアドレスは255文字以内で入力してください。");
            }

            if (!EMAIL_PATTERN.matcher(normalizedGuestMail).matches()) {
                throw new IllegalArgumentException(
                        "メールアドレスの形式が正しくありません。");
            }

            // 同じメールアドレスのユーザーが存在する場合はIDを再利用
            Users existingUser = usersMapper.findByMail(normalizedGuestMail);

            if (existingUser != null) {
                userId = existingUser.getId();
            } else {
                // 未登録のメールアドレスならゲストユーザーを新規登録
                Users newUser = new Users();
                newUser.setName(normalizedGuestName);
                newUser.setMail(normalizedGuestMail);

                usersMapper.insert(newUser);

                userId = newUser.getId();

                if (userId == null || userId <= 0) {
                    throw new IllegalStateException(
                            "ゲストユーザーIDを取得できませんでした。");
                }
            }
        }

        Reservations reservation = new Reservations();
        reservation.setMemberId(memberId);
        reservation.setUserId(userId);
        reservation.setStoreId(storeId);
        reservation.setReservationDate(dateTime);
        reservation.setNumber(number);
        reservation.setStatus(STATUS_RESERVED);
        reservation.setCreatedAt(LocalDateTime.now());

        reservationMapper.insertReservation(reservation);
    }

    /*
     * 新規予約用：過去の日時は登録できない
     */
    private void validateNewReservation(
            LocalDateTime reservationDate,
            Integer number) {

        if (reservationDate == null) {
            throw new IllegalArgumentException(
                    "予約日と予約時刻を入力してください。");
        }

        if (number == null || number < 1 || number > 10) {
            throw new IllegalArgumentException(
                    "人数は1～10人で入力してください。");
        }

        if (reservationDate.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "新規予約に過去の日時は指定できません。");
        }
    }

    /*
     * ログイン中の店舗管理者を取得
     */
    private ShopAdmin getLoginShopAdmin() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        String loginId = authentication.getName();

        if (loginId == null || !loginId.matches("\\d{1,9}")) {
            return null;
        }

        return shopAdminMapper.findById(Integer.parseInt(loginId));
    }
}