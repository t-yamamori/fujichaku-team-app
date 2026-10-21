package com.example.demo.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.entity.Members;
import com.example.demo.entity.Reservations;
import com.example.demo.entity.Stores;
import com.example.demo.entity.Users;
import com.example.demo.mapper.ReservationMapper;
import com.example.demo.mapper.StoresMapper;
import com.example.demo.mapper.UsersMapper;
import com.example.demo.service.serviceInterface.MembersService;

import lombok.RequiredArgsConstructor;


@Controller
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationMapper reservationMapper;
    private final StoresMapper storesMapper;
    private final MembersService membersService;
    private final UsersMapper usersMapper;

    /**
     * メールアドレスの形式チェック
     */
    private boolean isValidEmail(String mail) {
        if (mail == null || mail.isBlank()) {
            return false;
        }

        if (mail.length() > 255) {
            return false;
        }

        return mail.matches(
                "^[A-Za-z0-9_%+\\-]+(?:\\.[A-Za-z0-9_%+\\-]+)*"
                + "@[A-Za-z0-9](?:[A-Za-z0-9\\-]{0,61}[A-Za-z0-9])?"
                + "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9\\-]{0,61}[A-Za-z0-9])?)*"
                + "\\.[A-Za-z]{2,}$"
        );
    }

    private Members getLoginMember(Principal principal) {
        if (principal == null) {
            return null;
        }

        return membersService.findByLoginId(principal.getName());
    }

    private Integer getLoginMemberId(Principal principal) {
        Members member = getLoginMember(principal);
        return member != null ? member.getId() : null;
    }

    private boolean isAdmin(Integer memberId) {
        return memberId != null
                && memberId >= 20001
                && memberId <= 30000;
    }

    private void prepareReservationForm(
            Model model,
            Stores store,
            Integer shopId,
            Principal principal) {

        Members member = getLoginMember(principal);

        model.addAttribute("store", store);
        model.addAttribute("shopId", shopId);
        model.addAttribute("isMember", member != null);
        model.addAttribute(
                "memberName",
                member != null ? member.getName() : null);
        model.addAttribute(
                "memberId",
                member != null ? member.getId() : null);
        model.addAttribute(
                "historyUrl",
                member != null
                        ? "/reservations/history"
                        : "/register");
    }

    /**
     * 予約フォーム表示
     */
    @GetMapping("/shops/{shopId}/reservations/new")
    public String showReservationForm(
            @PathVariable Integer shopId,
            Principal principal,
            Model model,
            CsrfToken csrfToken) {

        // Thymeleafで画面を描画する前にCSRFトークンを確定させる
        csrfToken.getToken();

        Stores store = storesMapper.selectById(shopId);

        if (store == null) {
            return "redirect:/shops";
        }

        prepareReservationForm(model, store, shopId, principal);

        return "reservations/newreserve";
    }
    /**
     * 予約内容の確認
     */
    @PostMapping("/shops/{shopId}/reservations/confirm")
    public String confirmReservation(
            @PathVariable Integer shopId,
            @RequestParam String date,
            @RequestParam String time,
            @RequestParam Integer number,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String mail,
            Principal principal,
            Model model) {

        Stores store = storesMapper.selectById(shopId);

        if (store == null) {
            return "redirect:/shops";
        }

        Members loginMember = getLoginMember(principal);

        LocalDate reservationDate;
        LocalTime reservationTime;

        try {
            reservationDate = LocalDate.parse(date);
            reservationTime = LocalTime.parse(time);
        } catch (Exception e) {
            return returnToForm(
                    model, store, shopId, principal,
                    "予約日時が正しくありません。",
                    name, mail, date, time, number);
        }

        LocalDateTime reservationDateTime =
                LocalDateTime.of(reservationDate, reservationTime);

        if (reservationDateTime.isBefore(LocalDateTime.now())) {
            return returnToForm(
                    model, store, shopId, principal,
                    "過去の予約はできません。",
                    name, mail, date, time, number);
        }

        if (reservationTime.getMinute() % 30 != 0
                || reservationTime.isBefore(LocalTime.of(11, 0))
                || reservationTime.isAfter(LocalTime.of(21, 30))
                || reservationTime.getSecond() != 0
                || reservationTime.getNano() != 0) {

            return returnToForm(
                    model, store, shopId, principal,
                    "予約時間は11:00～21:30の30分刻みで選択してください。",
                    name, mail, date, time, number);
        }

        if (number == null || number < 1 || number > 10) {
            return returnToForm(
                    model, store, shopId, principal,
                    "予約人数を正しく選択してください。",
                    name, mail, date, time, number);
        }

        String trimmedName = name == null ? "" : name.trim();
        String trimmedMail = mail == null ? "" : mail.trim();

        if (loginMember != null) {
            model.addAttribute("memberId", loginMember.getId());
            model.addAttribute("userId", null);
            model.addAttribute("memberName", loginMember.getName());
            model.addAttribute("isMember", true);
        } else {
            if (trimmedName.isEmpty()) {
                return returnToForm(
                        model, store, shopId, principal,
                        "氏名を入力してください。",
                        name, mail, date, time, number);
            }

            if (trimmedMail.isEmpty()) {
                return returnToForm(
                        model, store, shopId, principal,
                        "メールアドレスを入力してください。",
                        name, mail, date, time, number);
            }

            if (!isValidEmail(trimmedMail)) {
                return returnToForm(
                        model, store, shopId, principal,
                        "メールアドレスの形式が正しくありません。"
                                + "（例：taro@gmail.com）",
                        name, mail, date, time, number);
            }

            Users user = usersMapper.findByMail(trimmedMail);

            if (user == null) {
                user = new Users();
                user.setName(trimmedName);
                user.setMail(trimmedMail);
                usersMapper.insert(user);
            }

            model.addAttribute("userId", user.getId());
            model.addAttribute("memberId", null);
            model.addAttribute("userName", trimmedName);
            model.addAttribute("userMail", trimmedMail);
            model.addAttribute("isMember", false);
        }

        model.addAttribute("store", store);
        model.addAttribute("shopId", shopId);
        model.addAttribute("date", reservationDate.toString());
        model.addAttribute("time", reservationTime.toString());
        model.addAttribute("reservationDate", reservationDate);
        model.addAttribute("reservationTime", reservationTime);
        model.addAttribute("reservationDateTime", reservationDateTime);
        model.addAttribute("number", number);

        model.addAttribute(
                "name",
                loginMember == null
                        ? trimmedName
                        : loginMember.getName());

        // 会員はDBに登録されているメールアドレス、
        // 非会員は入力されたメールアドレスを表示する。
        model.addAttribute(
                "mail",
                loginMember != null
                        ? loginMember.getMail()
                        : trimmedMail);

        return "reservations/reservation";
    }

    /**
     * 入力エラー時にフォームを再表示
     */
    private String returnToForm(
            Model model,
            Stores store,
            Integer shopId,
            Principal principal,
            String error,
            String name,
            String mail,
            String date,
            String time,
            Integer number) {

        prepareReservationForm(model, store, shopId, principal);

        model.addAttribute("error", error);
        model.addAttribute("name", name);
        model.addAttribute("mail", mail);
        model.addAttribute("date", date);
        model.addAttribute("time", time);
        model.addAttribute("number", number);

        return "reservations/newreserve";
    }

    /**
     * 予約確定
     */
    @PostMapping("/shops/{shopId}/reservations")
    public String reserve(
            @PathVariable Integer shopId,
            @RequestParam String date,
            @RequestParam String time,
            @RequestParam Integer number,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String mail,
            Principal principal,
            Model model) {

        Stores store = storesMapper.selectById(shopId);

        if (store == null) {
            return "redirect:/shops";
        }

        Members loginMember = getLoginMember(principal);

        LocalDate reservationDate;
        LocalTime reservationTime;

        try {
            reservationDate = LocalDate.parse(date);
            reservationTime = LocalTime.parse(time);
        } catch (Exception e) {
            return returnToForm(
                    model, store, shopId, principal,
                    "予約日時が正しくありません。",
                    name, mail, date, time, number);
        }

        LocalDateTime reservationDateTime =
                LocalDateTime.of(reservationDate, reservationTime);

        if (reservationDateTime.isBefore(LocalDateTime.now())) {
            return returnToForm(
                    model, store, shopId, principal,
                    "過去の予約はできません。",
                    name, mail, date, time, number);
        }

        if (reservationTime.getMinute() % 30 != 0
                || reservationTime.isBefore(LocalTime.of(11, 0))
                || reservationTime.isAfter(LocalTime.of(21, 30))
                || reservationTime.getSecond() != 0
                || reservationTime.getNano() != 0) {

            return returnToForm(
                    model, store, shopId, principal,
                    "予約時間は11:00～21:30の30分刻みで選択してください。",
                    name, mail, date, time, number);
        }

        if (number == null || number < 1 || number > 10) {
            return returnToForm(
                    model, store, shopId, principal,
                    "予約人数を正しく選択してください。",
                    name, mail, date, time, number);
        }

        Integer userId = null;

        // 完了画面に表示する予約者情報
        String reservationName;
        String reservationMail;

        if (loginMember == null) {
            String trimmedName = name == null ? "" : name.trim();
            String trimmedMail = mail == null ? "" : mail.trim();

            if (trimmedName.isEmpty()) {
                return returnToForm(
                        model, store, shopId, principal,
                        "氏名を入力してください。",
                        name, mail, date, time, number);
            }

            if (trimmedMail.isEmpty()) {
                return returnToForm(
                        model, store, shopId, principal,
                        "メールアドレスを入力してください。",
                        name, mail, date, time, number);
            }

            if (!isValidEmail(trimmedMail)) {
                return returnToForm(
                        model, store, shopId, principal,
                        "メールアドレスの形式が正しくありません。"
                                + "（例：taro@gmail.com）",
                        name, mail, date, time, number);
            }

            Users user = usersMapper.findByMail(trimmedMail);

            if (user == null) {
                user = new Users();
                user.setName(trimmedName);
                user.setMail(trimmedMail);
                usersMapper.insert(user);
            }

            userId = user.getId();

            // 予約に紐づくUsersの登録情報を表示する
            reservationName = user.getName();
            reservationMail = user.getMail();

        } else {
            // ログイン中の会員情報を表示する
            reservationName = loginMember.getName();
            reservationMail = loginMember.getMail();
        }

        Reservations reservation = new Reservations();

        reservation.setStoreId(shopId);
        reservation.setReservationDate(reservationDateTime);
        reservation.setNumber(number);
        reservation.setStatus("予約済み");
        reservation.setCreatedAt(LocalDateTime.now());

        if (loginMember != null) {
            reservation.setMemberId(loginMember.getId());
            reservation.setUserId(null);
        } else {
            reservation.setMemberId(null);
            reservation.setUserId(userId);
        }

        reservationMapper.insertReservation(reservation);

        Integer memberId =
                loginMember != null ? loginMember.getId() : null;

        if (memberId != null && !isAdmin(memberId)) {
            membersService.addPoint(
                    memberId,
                    MembersService.RESERVATION_POINT,
                    "RESERVATION");
        }

        model.addAttribute("reservation", reservation);
        model.addAttribute("store", store);
        model.addAttribute("reservationId", reservation.getId());

        // 会員・非会員共通の予約者情報
        model.addAttribute("reservationName", reservationName);
        model.addAttribute("reservationMail", reservationMail);

        if (loginMember != null) {
            return "reservations/complete";
        }

        return "reservations/complete_guest";
    }

    /**
     * 予約詳細
     */
    @GetMapping("/reservations/{reservationId}")
    public String showReservation(
            @PathVariable Integer reservationId,
            Principal principal,
            Model model) {

        Reservations reservation =
                reservationMapper.findById(reservationId);

        if (reservation == null) {
            return "redirect:/shops";
        }

        Integer loginMemberId = getLoginMemberId(principal);

        if (loginMemberId == null) {
            return "redirect:/register";
        }

        if (!isAdmin(loginMemberId)) {
            if (reservation.getMemberId() == null
                    || !reservation.getMemberId().equals(loginMemberId)) {
                return "redirect:/reservations/history";
            }
        }

        Stores store =
                storesMapper.selectById(reservation.getStoreId());

        if (store == null) {
            return "redirect:/shops";
        }

        LocalDateTime reservationDateTime =
                reservation.getReservationDate();

        model.addAttribute("reservation", reservation);
        model.addAttribute("store", store);
        model.addAttribute("shopId", reservation.getStoreId());
        model.addAttribute(
                "reservationDate", reservationDateTime.toLocalDate());
        model.addAttribute(
                "reservationTime", reservationDateTime.toLocalTime());
        model.addAttribute("reservationDateTime", reservationDateTime);
        model.addAttribute("number", reservation.getNumber());
        model.addAttribute("memberId", reservation.getMemberId());
        model.addAttribute("userId", reservation.getUserId());

        Members member = getLoginMember(principal);

        if (member != null) {
            model.addAttribute("memberName", member.getName());
        }

        if (reservation.getUserId() != null) {
            Users user =
                    usersMapper.findById(reservation.getUserId());

            if (user != null) {
                model.addAttribute("userName", user.getName());
                model.addAttribute("userMail", user.getMail());
            }
        }

        model.addAttribute("viewOnly", true);

        return "reservations/reservation";
    }

    /**
     * 予約履歴
     */
    @GetMapping("/reservations/history")
    public String showHistory(
            Principal principal,
            Model model) {

        Integer memberId = getLoginMemberId(principal);

        if (memberId == null) {
            return "redirect:/register";
        }

        List<Reservations> reservations;

        if (isAdmin(memberId)) {
            reservations = reservationMapper.findAll();
            model.addAttribute("isAdmin", true);
        } else {
            reservations = reservationMapper.findByMemberId(memberId);
            model.addAttribute("isAdmin", false);
        }

        Map<Integer, String> storeNames = new HashMap<>();

        for (Reservations reservation : reservations) {
            if (reservation.getStoreId() == null) {
                continue;
            }

            Stores reservationStore =
                    storesMapper.selectById(reservation.getStoreId());

            if (reservationStore != null) {
                storeNames.put(
                        reservation.getId(),
                        reservationStore.getName());
            }
        }

        model.addAttribute("reservations", reservations);
        model.addAttribute("storeNames", storeNames);

        return "reservations/history";
    }
}
