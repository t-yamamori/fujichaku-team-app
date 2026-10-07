package com.example.demo.controller;

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

import com.example.demo.entity.Reservations;
import com.example.demo.entity.Stores;
import com.example.demo.mapper.ReservationMapper;
import com.example.demo.mapper.StoresMapper;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationMapper reservationMapper;

    private final StoresMapper storesMapper;


    /*
     * =========================================================
     * ① 予約入力画面
     *
     * GET
     * /shops/{shopId}/reservations/new
     * =========================================================
     */
    @GetMapping("/shops/{shopId}/reservations/new")
    public String showReservationForm(
            @PathVariable Integer shopId,
            Model model) {

        Stores store = storesMapper.selectById(shopId);

        if (store == null) {
            return "redirect:/shops";
        }

        model.addAttribute("store", store);
        model.addAttribute("shopId", shopId);

        return "reservations/newreserve";
    }


    /*
     * =========================================================
     * ② 予約確認画面
     *
     * POST
     * /shops/{shopId}/reservations/confirm
     * =========================================================
     */
    @PostMapping("/shops/{shopId}/reservations/confirm")
    public String confirmReservation(
            @PathVariable Integer shopId,
            @RequestParam String date,
            @RequestParam String time,
            @RequestParam Integer number,
            @RequestParam(required = false) Integer memberId,
            @RequestParam(required = false) Integer userId,
            Model model) {

        /*
         * 店舗取得
         */
        Stores store = storesMapper.selectById(shopId);

        if (store == null) {
            return "redirect:/shops";
        }


        /*
         * 会員・非会員チェック
         *
         * どちらか一方だけ入っている必要がある
         */
        if (memberId != null && userId != null) {

            throw new IllegalArgumentException(
                    "memberId と userId を同時に指定することはできません。"
            );
        }

        if (memberId == null && userId == null) {

            throw new IllegalArgumentException(
                    "memberId または userId が必要です。"
            );
        }


        /*
         * 日付
         */
        LocalDate reservationDate =
                LocalDate.parse(date);


        /*
         * 時間
         */
        LocalTime reservationTime =
                LocalTime.parse(time);


        /*
         * 日付＋時間
         */
        LocalDateTime reservationDateTime =
                LocalDateTime.of(
                        reservationDate,
                        reservationTime
                );


        /*
         * 画面へ渡す
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
                userId
        );


        return "reservations/reservation";
    }


    /*
     * =========================================================
     * ③ 予約登録
     *
     * POST
     * /shops/{shopId}/reservations
     * =========================================================
     */
    @PostMapping("/shops/{shopId}/reservations")
    public String reserve(
            @PathVariable Integer shopId,
            @RequestParam String date,
            @RequestParam String time,
            @RequestParam Integer number,
            @RequestParam(required = false) Integer memberId,
            @RequestParam(required = false) Integer userId,
            Model model) {

        /*
         * =====================================================
         * 会員・非会員チェック
         * =====================================================
         */

        if (memberId != null && userId != null) {

            throw new IllegalArgumentException(
                    "memberId と userId を同時に指定することはできません。"
            );
        }

        if (memberId == null && userId == null) {

            throw new IllegalArgumentException(
                    "memberId または userId が必要です。"
            );
        }


        /*
         * =====================================================
         * 店舗取得
         * =====================================================
         */

        Stores store = storesMapper.selectById(shopId);

        if (store == null) {
            return "redirect:/shops";
        }


        /*
         * =====================================================
         * 予約日時
         * =====================================================
         */

        LocalDate reservationDate =
                LocalDate.parse(date);

        LocalTime reservationTime =
                LocalTime.parse(time);

        LocalDateTime reservationDateTime =
                LocalDateTime.of(
                        reservationDate,
                        reservationTime
                );


        /*
         * =====================================================
         * Reservations作成
         * =====================================================
         */

        Reservations reservation =
                new Reservations();


        /*
         * 店舗ID
         */
        reservation.setStoreId(shopId);


        /*
         * 予約日時
         */
        reservation.setReservationDate(
                reservationDateTime
        );


        /*
         * 予約人数
         */
        reservation.setNumber(number);


        /*
         * ステータス
         */
        reservation.setStatus("予約済み");


        /*
         * 作成日時
         */
        reservation.setCreatedAt(
                LocalDateTime.now()
        );


        /*
         * =====================================================
         * 会員・非会員の振り分け
         * =====================================================
         */

        if (memberId != null) {

            /*
             * 会員予約
             *
             * member_id = memberId
             * user_id   = NULL
             */

            reservation.setMemberId(memberId);
            reservation.setUserId(null);

        } else {

            /*
             * 非会員予約
             *
             * member_id = NULL
             * user_id   = userId
             */

            reservation.setMemberId(null);
            reservation.setUserId(userId);
        }


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
         * 完了画面へ
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
     * ④ 予約確認
     *
     * GET
     * /reservations/{reservationId}
     * =========================================================
     */
    @GetMapping("/reservations/{reservationId}")
    public String showReservation(
            @PathVariable Integer reservationId,
            Model model) {

        /*
         * 予約取得
         */
        Reservations reservation =
                reservationMapper.findById(
                        reservationId
                );

        if (reservation == null) {
            return "redirect:/shops";
        }


        /*
         * 店舗取得
         */
        Stores store =
                storesMapper.selectById(
                        reservation.getStoreId()
                );


        /*
         * 予約日時から日付・時間を取得
         */
        LocalDateTime reservationDateTime =
                reservation.getReservationDate();

        LocalDate reservationDate =
                reservationDateTime.toLocalDate();

        LocalTime reservationTime =
                reservationDateTime.toLocalTime();


        /*
         * 画面へ渡す
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
     * ⑤ 予約履歴
     *
     * GET
     * /reservations/history
     * =========================================================
     */
    @GetMapping("/reservations/history")
    public String showHistory(
            @RequestParam(required = false) Integer memberId,
            @RequestParam(required = false) Integer userId,
            Model model) {

        List<Reservations> reservations;


        /*
         * 会員の場合
         */
        if (memberId != null) {

            reservations =
                    reservationMapper.findByMemberId(
                            memberId
                    );


        /*
         * 非会員の場合
         */
        } else if (userId != null) {

            reservations =
                    reservationMapper.findByUserId(
                            userId
                    );


        /*
         * IDがない場合
         */
        } else {

            reservations = List.of();
        }


        model.addAttribute(
                "reservations",
                reservations
        );


        return "reservations/history";
    }

}