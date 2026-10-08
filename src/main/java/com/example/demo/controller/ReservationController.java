package com.example.demo.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

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

        /*
         * 非会員の場合はPrincipalが存在しない
         */
        if (principal == null) {
            return null;
        }

        String loginId = principal.getName();

        Members member =
                membersService.findByLoginId(loginId);

        if (member == null) {
            return null;
        }

        return member.getId();
    }


    // ==================================================
    // 管理者判定
    //
    // member_id
    // 10001～20000 → 会員
    // 20001～30000 → 管理者
    // ==================================================

    private boolean isAdmin(Integer memberId) {

        if (memberId == null) {
            return false;
        }

        return memberId >= 20001 && memberId <= 30000;
    }


    // ==================================================
    // 予約画面
    // ==================================================

    @GetMapping("/shops/{shopId}/reservations/new")
    public String showReservationForm(
            @PathVariable Integer shopId,
            Model model) {

        Stores store =
                storesMapper.selectById(shopId);

        if (store == null) {
            return "redirect:/shops";
        }

        model.addAttribute(
                "store",
                store
        );

        model.addAttribute(
                "shopId",
                shopId
        );

        return "reservations/newreserve";
    }


    // ==================================================
    // 予約確認
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

        // --------------------------------------------------
        // 店舗取得
        // --------------------------------------------------

        Stores store =
                storesMapper.selectById(shopId);

        if (store == null) {
            return "redirect:/shops";
        }


        // --------------------------------------------------
        // 日付・時間
        // --------------------------------------------------

        LocalDate reservationDate;
        LocalTime reservationTime;

        try {

            reservationDate =
                    LocalDate.parse(date);

            reservationTime =
                    LocalTime.parse(time);

        } catch (Exception e) {

            model.addAttribute(
                    "error",
                    "予約日時が正しくありません。"
            );

            model.addAttribute(
                    "store",
                    store
            );

            model.addAttribute(
                    "shopId",
                    shopId
            );

            return "reservations/newreserve";
        }


        LocalDateTime reservationDateTime =
                LocalDateTime.of(
                        reservationDate,
                        reservationTime
                );


        // --------------------------------------------------
        // 過去日時チェック
        // --------------------------------------------------

        if (reservationDateTime.isBefore(
                LocalDateTime.now())) {

            model.addAttribute(
                    "error",
                    "過去の予約はできません"
            );

            model.addAttribute(
                    "store",
                    store
            );

            model.addAttribute(
                    "shopId",
                    shopId
            );

            return "reservations/newreserve";
        }


        // --------------------------------------------------
        // 会員判定
        // --------------------------------------------------

        Integer memberId =
                getLoginMemberId(principal);


        // ==================================================
        // 会員・管理者
        // ==================================================

        if (memberId != null) {

            model.addAttribute(
                    "memberId",
                    memberId
            );

            model.addAttribute(
                    "userId",
                    null
            );
        }


        // ==================================================
        // 非会員
        // ==================================================

        else {

            // --------------------------------------------------
            // 氏名チェック
            // --------------------------------------------------

            if (name == null
                    || name.trim().isEmpty()) {

                model.addAttribute(
                        "error",
                        "氏名を入力してください。"
                );

                model.addAttribute(
                        "store",
                        store
                );

                model.addAttribute(
                        "shopId",
                        shopId
                );

                return "reservations/newreserve";
            }


            // --------------------------------------------------
            // メールチェック
            // --------------------------------------------------

            if (mail == null
                    || mail.trim().isEmpty()) {

                model.addAttribute(
                        "error",
                        "メールアドレスを入力してください。"
                );

                model.addAttribute(
                        "store",
                        store
                );

                model.addAttribute(
                        "shopId",
                        shopId
                );

                return "reservations/newreserve";
            }


            String trimmedName =
                    name.trim();

            String trimmedMail =
                    mail.trim();


            // --------------------------------------------------
            // users検索
            // --------------------------------------------------

            Users user =
                    usersMapper.findByMail(
                            trimmedMail
                    );


            // --------------------------------------------------
            // 新規非会員
            // --------------------------------------------------

            if (user == null) {

                user = new Users();

                user.setName(
                        trimmedName
                );

                user.setMail(
                        trimmedMail
                );

                usersMapper.insert(user);
            }


            // --------------------------------------------------
            // 非会員情報
            // --------------------------------------------------

            model.addAttribute(
                    "userId",
                    user.getId()
            );

            model.addAttribute(
                    "memberId",
                    null
            );

            model.addAttribute(
                    "userName",
                    user.getName()
            );

            model.addAttribute(
                    "userMail",
                    user.getMail()
            );
        }


        // --------------------------------------------------
        // 確認画面へ渡す
        // --------------------------------------------------

        model.addAttribute(
                "store",
                store
        );

        model.addAttribute(
                "shopId",
                shopId
        );

        model.addAttribute(
                "reservationDate",
                reservationDate
        );

        model.addAttribute(
                "reservationTime",
                reservationTime
        );

        model.addAttribute(
                "reservationDateTime",
                reservationDateTime
        );

        model.addAttribute(
                "number",
                number
        );


        return "reservations/reservation";
    }


    // ==================================================
    // 予約確定
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

        // --------------------------------------------------
        // 店舗取得
        // --------------------------------------------------

        Stores store =
                storesMapper.selectById(shopId);

        if (store == null) {
            return "redirect:/shops";
        }


        // --------------------------------------------------
        // 日付・時間
        // --------------------------------------------------

        LocalDate reservationDate;
        LocalTime reservationTime;

        try {

            reservationDate =
                    LocalDate.parse(date);

            reservationTime =
                    LocalTime.parse(time);

        } catch (Exception e) {

            model.addAttribute(
                    "error",
                    "予約日時が正しくありません。"
            );

            model.addAttribute(
                    "store",
                    store
            );

            model.addAttribute(
                    "shopId",
                    shopId
            );

            return "reservations/newreserve";
        }


        LocalDateTime reservationDateTime =
                LocalDateTime.of(
                        reservationDate,
                        reservationTime
                );


        // --------------------------------------------------
        // 過去日時チェック
        // --------------------------------------------------

        if (reservationDateTime.isBefore(
                LocalDateTime.now())) {

            model.addAttribute(
                    "error",
                    "過去の予約はできません"
            );

            model.addAttribute(
                    "store",
                    store
            );

            model.addAttribute(
                    "shopId",
                    shopId
            );

            return "reservations/newreserve";
        }


        // --------------------------------------------------
        // ログイン会員取得
        // --------------------------------------------------

        Integer memberId =
                getLoginMemberId(principal);

        Integer userId = null;


        // ==================================================
        // 会員・管理者
        // ==================================================

        if (memberId != null) {

            /*
             * 会員・管理者の予約
             *
             * member_idだけ使用
             */
            userId = null;
        }


        // ==================================================
        // 非会員
        // ==================================================

        else {

            // --------------------------------------------------
            // 氏名チェック
            // --------------------------------------------------

            if (name == null
                    || name.trim().isEmpty()) {

                model.addAttribute(
                        "error",
                        "氏名を入力してください。"
                );

                model.addAttribute(
                        "store",
                        store
                );

                model.addAttribute(
                        "shopId",
                        shopId
                );

                return "reservations/newreserve";
            }


            // --------------------------------------------------
            // メールチェック
            // --------------------------------------------------

            if (mail == null
                    || mail.trim().isEmpty()) {

                model.addAttribute(
                        "error",
                        "メールアドレスを入力してください。"
                );

                model.addAttribute(
                        "store",
                        store
                );

                model.addAttribute(
                        "shopId",
                        shopId
                );

                return "reservations/newreserve";
            }


            String trimmedName =
                    name.trim();

            String trimmedMail =
                    mail.trim();


            // --------------------------------------------------
            // users検索
            // --------------------------------------------------

            Users user =
                    usersMapper.findByMail(
                            trimmedMail
                    );


            // --------------------------------------------------
            // usersに存在しない場合
            // --------------------------------------------------

            if (user == null) {

                user = new Users();

                user.setName(
                        trimmedName
                );

                user.setMail(
                        trimmedMail
                );

                usersMapper.insert(user);
            }


            // --------------------------------------------------
            // users.id取得
            // --------------------------------------------------

            userId =
                    user.getId();
        }


        // ==================================================
        // 予約Entity作成
        // ==================================================

        Reservations reservation =
                new Reservations();


        reservation.setStoreId(
                shopId
        );

        reservation.setReservationDate(
                reservationDateTime
        );

        reservation.setNumber(
                number
        );

        reservation.setStatus(
                "予約済み"
        );

        reservation.setCreatedAt(
                LocalDateTime.now()
        );


        // ==================================================
        // 会員・管理者予約
        // ==================================================

        if (memberId != null) {

            reservation.setMemberId(
                    memberId
            );

            reservation.setUserId(
                    null
            );
        }


        // ==================================================
        // 非会員予約
        // ==================================================

        else {

            reservation.setMemberId(
                    null
            );

            reservation.setUserId(
                    userId
            );
        }


        // --------------------------------------------------
        // DB登録
        // --------------------------------------------------

        reservationMapper.insertReservation(
                reservation
        );


        // --------------------------------------------------
        // 完了画面
        // --------------------------------------------------

        model.addAttribute(
                "reservation",
                reservation
        );

        model.addAttribute(
                "store",
                store
        );

        model.addAttribute(
                "reservationId",
                reservation.getId()
        );


        return "reservations/complete";
    }


    // ==================================================
    // 予約詳細
    // ==================================================

    @GetMapping("/reservations/{reservationId}")
    public String showReservation(
            @PathVariable Integer reservationId,
            Principal principal,
            Model model) {

        // --------------------------------------------------
        // 予約取得
        // --------------------------------------------------

        Reservations reservation =
                reservationMapper.findById(
                        reservationId
                );

        if (reservation == null) {
            return "redirect:/shops";
        }


        // --------------------------------------------------
        // 店舗取得
        // --------------------------------------------------

        Stores store =
                storesMapper.selectById(
                        reservation.getStoreId()
                );


        // --------------------------------------------------
        // ログイン会員確認
        // --------------------------------------------------

        Integer loginMemberId =
                getLoginMemberId(principal);


        // ==================================================
        // 会員・管理者
        // ==================================================

        if (loginMemberId != null) {

            // --------------------------------------------------
            // 管理者
            // --------------------------------------------------

            if (isAdmin(loginMemberId)) {

                /*
                 * 管理者は全予約を閲覧可能
                 */

            }

            // --------------------------------------------------
            // 一般会員
            // --------------------------------------------------

            else {

                /*
                 * 一般会員は自分の予約だけ閲覧可能
                 */

                if (reservation.getMemberId() == null
                        || reservation.getMemberId().intValue()
                        != loginMemberId.intValue()) {

                    return "redirect:/reservations/history";
                }
            }
        }


        // ==================================================
        // 非会員
        // ==================================================

        else {

            /*
             * 非会員はURL直接入力による予約詳細閲覧を許可しない。
             *
             * 完了画面からの本人確認については、
             * 必要であれば後でSession等を追加する。
             */

            if (reservation.getUserId() == null) {

                return "redirect:/shops";
            }

            return "redirect:/shops";
        }


        // --------------------------------------------------
        // 日時
        // --------------------------------------------------

        LocalDateTime reservationDateTime =
                reservation.getReservationDate();

        LocalDate reservationDate =
                reservationDateTime.toLocalDate();

        LocalTime reservationTime =
                reservationDateTime.toLocalTime();


        // --------------------------------------------------
        // Model
        // --------------------------------------------------

        model.addAttribute(
                "reservation",
                reservation
        );

        model.addAttribute(
                "store",
                store
        );

        model.addAttribute(
                "shopId",
                reservation.getStoreId()
        );

        model.addAttribute(
                "reservationDate",
                reservationDate
        );

        model.addAttribute(
                "reservationTime",
                reservationTime
        );

        model.addAttribute(
                "reservationDateTime",
                reservationDateTime
        );

        model.addAttribute(
                "number",
                reservation.getNumber()
        );

        model.addAttribute(
                "memberId",
                reservation.getMemberId()
        );

        model.addAttribute(
                "userId",
                reservation.getUserId()
        );


        // --------------------------------------------------
        // 非会員情報
        // --------------------------------------------------

        if (reservation.getUserId() != null) {

            Users user =
                    usersMapper.findById(
                            reservation.getUserId()
                    );

            if (user != null) {

                model.addAttribute(
                        "userName",
                        user.getName()
                );

                model.addAttribute(
                        "userMail",
                        user.getMail()
                );
            }
        }


        return "reservations/reservation";
    }


    // ==================================================
    // 予約履歴
    // ==================================================

    @GetMapping("/reservations/history")
    public String showHistory(
            Principal principal,
            Model model) {

        // --------------------------------------------------
        // ログイン会員取得
        // --------------------------------------------------

        Integer memberId =
                getLoginMemberId(principal);


        // --------------------------------------------------
        // 非ログイン
        // --------------------------------------------------

        if (memberId == null) {

            /*
             * 非会員は予約履歴を表示しない
             */

            return "redirect:/shops";
        }


        // ==================================================
        // 管理者
        // ==================================================

        if (isAdmin(memberId)) {

            /*
             * 管理者は全予約を取得
             */

            List<Reservations> reservations =
                    reservationMapper.findAll();

            model.addAttribute(
                    "reservations",
                    reservations
            );

            model.addAttribute(
                    "isAdmin",
                    true
            );

            return "reservations/history";
        }


        // ==================================================
        // 一般会員
        // ==================================================

        List<Reservations> reservations =
                reservationMapper.findByMemberId(
                        memberId
                );

        model.addAttribute(
                "reservations",
                reservations
        );

        model.addAttribute(
                "isAdmin",
                false
        );


        return "reservations/history";
    }
}