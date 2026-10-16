package com.example.demo.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // ==================================================
    // ログイン中の会員ID取得
    // ==================================================

    private Integer getLoginMemberId(Principal principal) {

        if (principal == null) {
            return null;
        }

        Members member =
                membersService.findByLoginId(principal.getName());

        if (member == null) {
            return null;
        }

        return member.getId();
    }

    // ==================================================
    // 管理者判定
    // member_id 10001～20000：会員
    // member_id 20001～30000：管理者
    // ==================================================

    private boolean isAdmin(Integer memberId) {

        return memberId != null
                && memberId >= 20001
                && memberId <= 30000;
    }

    // ==================================================
    // 予約フォームに必要な共通情報
    // ==================================================

    private void prepareReservationForm(
            Model model,
            Stores store,
            Integer shopId,
            Principal principal) {

        Integer memberId = getLoginMemberId(principal);

        model.addAttribute("store", store);
        model.addAttribute("shopId", shopId);
        model.addAttribute("isMember", memberId != null);

        // 会員は予約履歴、ゲストは会員登録画面へ
        model.addAttribute(
                "historyUrl",
                memberId != null
                        ? "/reservations/history"
                        : "/register"
        );
    }

    // ==================================================
    // 予約画面
    // GET /shops/{shopId}/reservations/new
    // ==================================================

    @GetMapping("/shops/{shopId}/reservations/new")
    public String showReservationForm(
            @PathVariable Integer shopId,
            Principal principal,
            Model model) {

        Stores store = storesMapper.selectById(shopId);

        if (store == null) {
            return "redirect:/shops";
        }

        prepareReservationForm(
                model,
                store,
                shopId,
                principal
        );

        return "reservations/newreserve";
    }

    // ==================================================
    // 予約内容確認
    // POST /shops/{shopId}/reservations/confirm
    // ==================================================

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

        Integer memberId = getLoginMemberId(principal);

        // 日付・時間の形式を確認
        LocalDate reservationDate;
        LocalTime reservationTime;

        try {
            reservationDate = LocalDate.parse(date);
            reservationTime = LocalTime.parse(time);

        } catch (Exception e) {

            prepareReservationForm(model, store, shopId, principal);

            model.addAttribute("error", "予約日時が正しくありません。");
            model.addAttribute("name", name);
            model.addAttribute("mail", mail);

            return "reservations/newreserve";
        }

        // 予約日時を作成
        LocalDateTime reservationDateTime =
                LocalDateTime.of(reservationDate, reservationTime);

        // 過去日時の予約は禁止
        if (reservationDateTime.isBefore(LocalDateTime.now())) {

            prepareReservationForm(model, store, shopId, principal);

            model.addAttribute("error", "過去の予約はできません");
            model.addAttribute("name", name);
            model.addAttribute("mail", mail);

            return "reservations/newreserve";
        }
        
        // 【追加】予約時間は 11:00〜21:30 の30分刻みだけ受け付ける
        if (reservationTime.getMinute() % 30 != 0
                || reservationTime.isBefore(LocalTime.of(11, 0))
                || reservationTime.isAfter(LocalTime.of(21, 30))) {

            prepareReservationForm(model, store, shopId, principal);

            model.addAttribute("error", "予約時間は 11:00〜21:30 の間で選択してください。");
            model.addAttribute("name", name);
            model.addAttribute("mail", mail);

            return "reservations/newreserve";
        }

        // 予約人数の確認
        if (number == null || number < 1 || number > 10) {

            prepareReservationForm(model, store, shopId, principal);

            model.addAttribute("error", "予約人数を正しく選択してください。");
            model.addAttribute("name", name);
            model.addAttribute("mail", mail);

            return "reservations/newreserve";
        }

        // ==================================================
        // 会員
        // 氏名・メールアドレスの入力は不要
        // ==================================================

        if (memberId != null) {

            model.addAttribute("memberId", memberId);
            model.addAttribute("userId", null);
        }

        // ==================================================
        // ゲスト
        // 氏名・メールアドレスを必須にする
        // ==================================================

        else {

            if (name == null || name.trim().isEmpty()) {

                prepareReservationForm(model, store, shopId, principal);

                model.addAttribute("error", "氏名を入力してください。");
                model.addAttribute("name", name);
                model.addAttribute("mail", mail);

                return "reservations/newreserve";
            }

            if (mail == null || mail.trim().isEmpty()) {

                prepareReservationForm(model, store, shopId, principal);

                model.addAttribute(
                        "error",
                        "メールアドレスを入力してください。"
                );

                model.addAttribute("name", name);
                model.addAttribute("mail", mail);

                return "reservations/newreserve";
            }

            String trimmedName = name.trim();
            String trimmedMail = mail.trim();

            // メールアドレスで既存のゲスト情報を検索
            Users user = usersMapper.findByMail(trimmedMail);

            // 登録がなければゲスト情報を作成
            if (user == null) {

                user = new Users();
                user.setName(trimmedName);
                user.setMail(trimmedMail);

                usersMapper.insert(user);
            }

            model.addAttribute("userId", user.getId());
            model.addAttribute("memberId", null);
            model.addAttribute("userName", user.getName());
            model.addAttribute("userMail", user.getMail());
        }

        // ==================================================
        // 予約確認画面へ渡す情報
        // ==================================================

        model.addAttribute("store", store);
        model.addAttribute("shopId", shopId);
        model.addAttribute("isMember", memberId != null);

        model.addAttribute("reservationDate", reservationDate);
        model.addAttribute("reservationTime", reservationTime);
        model.addAttribute("reservationDateTime", reservationDateTime);
        model.addAttribute("number", number);

        return "reservations/reservation";
    }

    // ==================================================
    // 予約確定
    // POST /shops/{shopId}/reservations
    // ==================================================

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

        Integer memberId = getLoginMemberId(principal);

        LocalDate reservationDate;
        LocalTime reservationTime;

        try {
            reservationDate = LocalDate.parse(date);
            reservationTime = LocalTime.parse(time);

        } catch (Exception e) {

            prepareReservationForm(model, store, shopId, principal);

            model.addAttribute("error", "予約日時が正しくありません。");
            model.addAttribute("name", name);
            model.addAttribute("mail", mail);

            return "reservations/newreserve";
        }

        LocalDateTime reservationDateTime =
                LocalDateTime.of(reservationDate, reservationTime);

        if (reservationDateTime.isBefore(LocalDateTime.now())) {

            prepareReservationForm(model, store, shopId, principal);

            model.addAttribute("error", "過去の予約はできません");
            model.addAttribute("name", name);
            model.addAttribute("mail", mail);

            return "reservations/newreserve";
        }
        
        // 過去日時の予約は禁止
        if (reservationDateTime.isBefore(LocalDateTime.now())) {

            prepareReservationForm(model, store, shopId, principal);

            model.addAttribute("error", "過去の予約はできません");
            model.addAttribute("name", name);
            model.addAttribute("mail", mail);

            return "reservations/newreserve";
        }
        
        // 【追加】予約時間は 11:00〜21:30 の30分刻みだけ受け付ける
        if (reservationTime.getMinute() % 30 != 0
                || reservationTime.isBefore(LocalTime.of(11, 0))
                || reservationTime.isAfter(LocalTime.of(21, 30))) {

            prepareReservationForm(model, store, shopId, principal);

            model.addAttribute("error", "予約時間は 11:00〜21:30 の間で選択してください。");
            model.addAttribute("name", name);
            model.addAttribute("mail", mail);

            return "reservations/newreserve";
        }

        if (number == null || number < 1 || number > 10) {

            prepareReservationForm(model, store, shopId, principal);

            model.addAttribute("error", "予約人数を正しく選択してください。");
            model.addAttribute("name", name);
            model.addAttribute("mail", mail);

            return "reservations/newreserve";
        }

        Integer userId = null;

        // ==================================================
        // ゲスト情報の取得
        // ==================================================

        if (memberId == null) {

            if (name == null || name.trim().isEmpty()) {

                prepareReservationForm(model, store, shopId, principal);

                model.addAttribute("error", "氏名を入力してください。");
                model.addAttribute("name", name);
                model.addAttribute("mail", mail);

                return "reservations/newreserve";
            }

            if (mail == null || mail.trim().isEmpty()) {

                prepareReservationForm(model, store, shopId, principal);

                model.addAttribute(
                        "error",
                        "メールアドレスを入力してください。"
                );

                model.addAttribute("name", name);
                model.addAttribute("mail", mail);

                return "reservations/newreserve";
            }

            String trimmedName = name.trim();
            String trimmedMail = mail.trim();

            Users user = usersMapper.findByMail(trimmedMail);

            if (user == null) {

                user = new Users();
                user.setName(trimmedName);
                user.setMail(trimmedMail);

                usersMapper.insert(user);
            }

            userId = user.getId();
        }

        // ==================================================
        // 予約データ作成
        // ==================================================

        Reservations reservation = new Reservations();

        reservation.setStoreId(shopId);
        reservation.setReservationDate(reservationDateTime);
        reservation.setNumber(number);
        reservation.setStatus("予約済み");
        reservation.setCreatedAt(LocalDateTime.now());

        if (memberId != null) {

            // 会員予約は member_id のみ設定
            reservation.setMemberId(memberId);
            reservation.setUserId(null);

        } else {

            // ゲスト予約は user_id のみ設定
            reservation.setMemberId(null);
            reservation.setUserId(userId);
        }

        // データベースへ登録
        reservationMapper.insertReservation(reservation);

        // 会員の予約のみポイント加算（管理者は対象外）
        if (memberId != null && !isAdmin(memberId)) {

            membersService.addPoint(
                    memberId,
                    MembersService.RESERVATION_POINT,
                    "RESERVATION"
            );
        }

        model.addAttribute("reservation", reservation);
        model.addAttribute("store", store);
        model.addAttribute("reservationId", reservation.getId());

        // 完了画面を会員・ゲストで振り分け
        if (memberId != null) {
            return "reservations/complete";
        }

        return "reservations/complete_guest";
    }

    // ==================================================
    // 予約詳細
    // GET /reservations/{reservationId}
    // ==================================================

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

        // ゲストは予約詳細を直接閲覧できない
        if (loginMemberId == null) {
            return "redirect:/register";
        }

        // 一般会員は自分の予約だけ閲覧可能
        if (!isAdmin(loginMemberId)) {

            if (reservation.getMemberId() == null
                    || !reservation.getMemberId().equals(loginMemberId)) {

                return "redirect:/reservations/history";
            }
        }

        Stores store = storesMapper.selectById(reservation.getStoreId());

        if (store == null) {
            return "redirect:/shops";
        }

        LocalDateTime reservationDateTime =
                reservation.getReservationDate();

        model.addAttribute("reservation", reservation);
        model.addAttribute("store", store);
        model.addAttribute("shopId", reservation.getStoreId());
        model.addAttribute("reservationDate", reservationDateTime.toLocalDate());
        model.addAttribute("reservationTime", reservationDateTime.toLocalTime());
        model.addAttribute("reservationDateTime", reservationDateTime);
        model.addAttribute("number", reservation.getNumber());
        model.addAttribute("memberId", reservation.getMemberId());
        model.addAttribute("userId", reservation.getUserId());

        // ゲスト予約の詳細情報
        if (reservation.getUserId() != null) {

            Users user = usersMapper.findById(reservation.getUserId());

            if (user != null) {
                model.addAttribute("userName", user.getName());
                model.addAttribute("userMail", user.getMail());
            }
        }
        
        // 【追加】予約履歴の DETAILS から開いたときは「見るだけ」にする（確定ボタンを出さない）
        model.addAttribute("viewOnly", true);

        return "reservations/reservation";
    }

    // ==================================================
    // 予約履歴
    // GET /reservations/history
    // ==================================================

    @GetMapping("/reservations/history")
    public String showHistory(
            Principal principal,
            Model model) {

        Integer memberId = getLoginMemberId(principal);

        // ゲストは会員登録画面へ
        if (memberId == null) {
            return "redirect:/register";
        }

        List<Reservations> reservations;

        // 管理者は全予約を表示
        if (isAdmin(memberId)) {

            reservations = reservationMapper.findAll();
            model.addAttribute("isAdmin", true);

        } else {

            // 一般会員は自分の予約だけ表示
            reservations = reservationMapper.findByMemberId(memberId);
            model.addAttribute("isAdmin", false);
        }

        // 予約ごとの店舗名を取得
        Map<Integer, String> storeNames = new HashMap<>();

        for (Reservations reservation : reservations) {

            if (reservation.getStoreId() == null) {
                continue;
            }

            Stores store =
                    storesMapper.selectById(reservation.getStoreId());

            if (store != null) {
                storeNames.put(reservation.getId(), store.getName());
            }
        }

        model.addAttribute("reservations", reservations);
        model.addAttribute("storeNames", storeNames);

        return "reservations/history";
    }
}