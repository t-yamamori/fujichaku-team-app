package com.example.demo.controller;

import java.time.LocalDateTime;
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

@Controller
public class ReservationController {

    private final ReservationMapper reservationMapper;
    private final StoresMapper storesMapper;

    public ReservationController(
            ReservationMapper reservationMapper,
            StoresMapper storesMapper) {

        this.reservationMapper = reservationMapper;
        this.storesMapper = storesMapper;
    }

    /*
     * =========================================================
     * ① 予約入力画面
     *
     * GET
     * /shops/{shopId}/reservations/new
     *
     * 店舗情報を取得して予約画面を表示する
     * =========================================================
     */
    @GetMapping("/shops/{shopId}/reservations/new")
    public String showReservation(

            @PathVariable("shopId") Integer shopId,
            Model model) {

        // 店舗情報をDBから取得
        Stores store = storesMapper.selectById(shopId);

        // 店舗が存在しない場合
        if (store == null) {
            return "redirect:/shops";
        }

        // 画面に店舗情報を渡す
        model.addAttribute("store", store);

        // shopIdも画面に渡す
        model.addAttribute("shopId", shopId);

        return "reservations/newreserve";
    }


    /*
     * =========================================================
     * ② 予約登録
     *
     * POST
     * /shops/{shopId}/reservations
     *
     * 予約内容をDBへ登録する
     * =========================================================
     */
    @PostMapping("/shops/{shopId}/reservations")
    public String reserve(
            @PathVariable("shopId") Integer shopId,
            @RequestParam("date") String date,
            @RequestParam("time") String time,
            @RequestParam("number") Integer number) {

        /*
         * date
         * 例：2026-10-05
         *
         * time
         * 例：18:00
         *
         * ↓
         *
         * 2026-10-05T18:00
         *
         * LocalDateTimeに変換
         */
        LocalDateTime reservationDate =
                LocalDateTime.parse(date + "T" + time);


        /*
         * Reservationsオブジェクトを作成
         */
        Reservations reservation = new Reservations();


        /*
         * 会員ID
         *
         * 現在はログイン機能が完成していないため
         * 仮に123を使用
         */
        reservation.setMember_id(123);


        /*
         * 予約日時
         */
        reservation.setReservation_date(reservationDate);


        /*
         * 予約状況
         */
        reservation.setStatus("予約済み");


        /*
         * 登録日時
         */
        reservation.setCreated_at(LocalDateTime.now());


        /*
         * 店舗ID
         */
        reservation.setStore_id(shopId);


        /*
         * 予約人数
         */
        reservation.setNumber(number);


        /*
         * DBへ登録
         *
         * INSERT後、
         * 自動採番されたidが
         * reservation.getId()
         * で取得できる
         */
        reservationMapper.insertReservation(reservation);


        /*
         * 登録された予約番号を取得
         */
        Integer reservationId = reservation.getId();


        /*
         * 予約確認画面へ移動
         *
         * GET
         * /reservations/{reservationId}
         */
        return "redirect:/reservations/" + reservationId;
    }


    /*
     * =========================================================
     * ③ 予約確認画面
     *
     * GET
     * /reservations/{reservationId}
     *
     * DBから予約情報を取得して表示する
     * =========================================================
     */
    @GetMapping("/reservations/{reservationId}")
    public String showReserved(
            @PathVariable("reservationId") Integer reservationId,
            Model model) {

        /*
         * 予約番号から予約情報を取得
         */
        Reservations reservation =
                reservationMapper.findById(reservationId);


        /*
         * 予約が存在しない場合
         */
        if (reservation == null) {
            return "redirect:/shops";
        }


        /*
         * 予約に入っているstore_idから
         * 店舗情報を取得
         */
        Stores store =
                storesMapper.selectById(reservation.getStore_id());


        /*
         * 予約情報を画面へ渡す
         */
        model.addAttribute("reservation", reservation);


        /*
         * 店舗情報を画面へ渡す
         */
        model.addAttribute("store", store);


        return "reservations/reservation";
    }


    /*
     * =========================================================
     * ④ 予約履歴
     *
     * GET
     * /reservations/history
     *
     * ログイン中の会員の予約履歴を表示する
     * =========================================================
     */
    @GetMapping("/reservations/history")
    public String showHistory(Model model) {

        /*
         * 現在はログイン機能が完成していないため
         * 仮に会員ID「123」を使用
         */
        Integer memberId = 123;


        /*
         * 会員IDから予約履歴を取得
         */
        List<Reservations> reservations =
                reservationMapper.findByMemberId(memberId);


        /*
         * 予約履歴を画面へ渡す
         */
        model.addAttribute("reservations", reservations);


        return "reservations/history";
    }
}
