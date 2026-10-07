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

import com.example.demo.config.SecurityConfig;
import com.example.demo.entity.Members;
import com.example.demo.entity.Reservations;
import com.example.demo.entity.Stores;
import com.example.demo.mapper.ReservationMapper;
import com.example.demo.mapper.StoresMapper;
import com.example.demo.service.serviceInterface.MembersService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationMapper reservationMapper;

    private final StoresMapper storesMapper;

    private final MembersService membersService;


    /*
     * =========================================================
     * ログイン中の会員IDを取得
     * =========================================================
     *
     * SecurityConfig.TEST_MODE によって切り替える。
     *
     * true
     *  → testユーザー
     *  → member_id = 10000
     *
     * false
     *  → 本番ログイン
     *  → Principalから会員IDを取得
     *  → DBのmembersを検索
     *
     */
    private Integer getLoginMemberId(Principal principal) {

        if (principal == null) {

            throw new IllegalArgumentException(
                "ログイン情報を取得できません。"
            );
        }


        /*
         * =====================================================
         * テストモード
         * =====================================================
         */
        if (SecurityConfig.TEST_MODE) {

            String username = principal.getName();


            /*
             * テスト用ログイン
             *
             * test / test123
             */
            if ("test".equals(username)) {

                /*
                 * membersテーブルに存在する
                 * テスト用会員
                 */
                return 10000;
            }


            throw new IllegalArgumentException(
                "現在のテスト環境では「test」ユーザーのみ使用できます。"
            );
        }


        /*
         * =====================================================
         * 本番モード
         * =====================================================
         *
         * Principalに入っているログインIDから
         * membersテーブルの会員を取得する。
         *
         */
        String loginId = principal.getName();

        Members member =
                membersService.findByLoginId(loginId);


        if (member == null) {

            throw new IllegalArgumentException(
                "ログイン中の会員情報が見つかりません。"
            );
        }


        return member.getId();
    }


    /*
     * =========================================================
     * 予約入力画面
     *
     * GET /shops/{shopId}/reservations/new
     * =========================================================
     */
    @GetMapping("/shops/{shopId}/reservations/new")
    public String showReservationForm(
            @PathVariable Integer shopId,
            Model model) {

        // 店舗情報取得
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


    /*
     * =========================================================
     * 予約確認画面
     *
     * POST /shops/{shopId}/reservations/confirm
     * =========================================================
     */
    @PostMapping("/shops/{shopId}/reservations/confirm")
    public String confirmReservation(
            @PathVariable Integer shopId,
            @RequestParam String date,
            @RequestParam String time,
            @RequestParam Integer number,
            Principal principal,
            Model model) {


        /*
         * ログイン中の会員IDを取得
         *
         * TEST_MODE = true
         * → 10000
         *
         * TEST_MODE = false
         * → DBから取得
         */
        Integer memberId =
                getLoginMemberId(principal);


        // 店舗取得
        Stores store =
                storesMapper.selectById(shopId);


        if (store == null) {

            return "redirect:/shops";
        }


        // 日付変換
        LocalDate reservationDate =
                LocalDate.parse(date);


        // 時間変換
        LocalTime reservationTime =
                LocalTime.parse(time);


        // 日時作成
        LocalDateTime reservationDateTime =
                LocalDateTime.of(
                    reservationDate,
                    reservationTime
                );


        /*
         * =====================================================
         * 確認画面へデータを渡す
         * =====================================================
         */
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

        model.addAttribute(
            "memberId",
            memberId
        );

        model.addAttribute(
            "userId",
            null
        );


        return "reservations/reservation";
    }


    /*
     * =========================================================
     * 予約登録
     *
     * POST /shops/{shopId}/reservations
     * =========================================================
     */
    @PostMapping("/shops/{shopId}/reservations")
    public String reserve(
            @PathVariable Integer shopId,
            @RequestParam String date,
            @RequestParam String time,
            @RequestParam Integer number,
            Principal principal,
            Model model) {


        /*
         * ログイン中の会員IDを取得
         */
        Integer memberId =
                getLoginMemberId(principal);


        // 店舗取得
        Stores store =
                storesMapper.selectById(shopId);


        if (store == null) {

            return "redirect:/shops";
        }


        // 日付
        LocalDate reservationDate =
                LocalDate.parse(date);


        // 時間
        LocalTime reservationTime =
                LocalTime.parse(time);


        // 予約日時
        LocalDateTime reservationDateTime =
                LocalDateTime.of(
                    reservationDate,
                    reservationTime
                );


        /*
         * =====================================================
         * Reservation作成
         * =====================================================
         */
        Reservations reservation =
                new Reservations();


        reservation.setStoreId(
            shopId
        );


        // ログイン中の会員
        reservation.setMemberId(
            memberId
        );


        // 現在はuserIdを使用しない
        reservation.setUserId(
            null
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


        /*
         * =====================================================
         * DB登録
         * =====================================================
         */
        reservationMapper.insertReservation(
            reservation
        );


        /*
         * =====================================================
         * 完了画面
         * =====================================================
         */
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


    /*
     * =========================================================
     * 予約詳細・確認
     *
     * GET /reservations/{reservationId}
     * =========================================================
     */
    @GetMapping("/reservations/{reservationId}")
    public String showReservation(
            @PathVariable Integer reservationId,
            Model model) {


        // 予約情報取得
        Reservations reservation =
                reservationMapper.findById(
                    reservationId
                );


        if (reservation == null) {

            return "redirect:/shops";
        }


        // 店舗情報取得
        Stores store =
                storesMapper.selectById(
                    reservation.getStoreId()
                );


        // 予約日時
        LocalDateTime reservationDateTime =
                reservation.getReservationDate();


        LocalDate reservationDate =
                reservationDateTime.toLocalDate();


        LocalTime reservationTime =
                reservationDateTime.toLocalTime();


        /*
         * =====================================================
         * Viewへ渡す
         * =====================================================
         */
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


        return "reservations/reservation";
    }


    /*
     * =========================================================
     * 予約履歴
     *
     * GET /reservations/history
     * =========================================================
     */
    @GetMapping("/reservations/history")
    public String showHistory(
            Principal principal,
            Model model) {


        /*
         * ログイン中の会員IDを取得
         *
         * TEST_MODE = true
         * → 10000
         *
         * TEST_MODE = false
         * → DBの会員ID
         */
        Integer memberId =
                getLoginMemberId(principal);


        /*
         * 会員の予約履歴取得
         */
        List<Reservations> reservations =
                reservationMapper.findByMemberId(
                    memberId
                );


        model.addAttribute(
            "reservations",
            reservations
        );


        return "reservations/history";
    }
}
