package com.example.demo.controller;

import java.time.LocalDateTime;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.entity.Reservations;
import com.example.demo.entity.Stores;
import com.example.demo.mapper.ReservationMapper;
import com.example.demo.repository.StoresMapper;

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
     * ① 予約画面表示
     *
     * GET
     * /shops/{shopId}/reservations/new
     *
     * 役割：
     * 店舗詳細から予約画面を表示する
     * =========================================================
     */
    @GetMapping("/shops/{shopId}/reservations/new")
    public String showReservationForm(
            @PathVariable("shopId") int shopId,
            Model model) {

        // 店舗IDから店舗情報を取得
        Stores store = storesMapper.selectById(shopId);

        // 店舗が存在しない場合
        if (store == null) {
            return "redirect:/shops";
        }

        // HTMLへ店舗情報を渡す
        model.addAttribute("store", store);

        return "reservations/newreserve";
    }


    /*
     * =========================================================
     * ② 予約登録
     *
     * POST
     * /shops/{shopId}/reservations
     *
     * 役割：
     * 入力された予約内容をDBへ登録する
     * =========================================================
     */
    @PostMapping("/shops/{shopId}/reservations")
    public String reserve(
            @PathVariable("shopId") int shopId,
            @RequestParam("date") String date,
            @RequestParam("time") String time) {

        // 予約情報を作成
        Reservations reservation = new Reservations();

        /*
         * 現在はログイン機能と完全に接続していないため、
         * テスト用として会員ID「1」を使用
         *
         * ログイン機能が完成したら、
         * セッション等から実際のmember_idを取得する
         */
        reservation.setMember_id(1);

        // 店舗ID
        reservation.setStore_id(shopId);

        // 予約日時
        LocalDateTime reservationDate =
                LocalDateTime.parse(date + "T" + time);

        reservation.setReservation_date(reservationDate);

        // 予約状況
        reservation.setStatus("予約確定");

        // 登録日時
        reservation.setCreated_at(LocalDateTime.now());

        // DBへ登録
        reservationMapper.insertReservation(reservation);

        /*
         * useGeneratedKeys=true によって
         * INSERT後に自動採番されたIDが
         * reservation.getId() に入る
         */

        return "redirect:/reservations/" + reservation.getId();
    }


    /*
     * =========================================================
     * ③ 予約確認
     *
     * GET
     * /reservations/{reservationId}
     *
     * 役割：
     * 登録した予約を取得して表示する
     * 予約番号もここで表示する
     * =========================================================
     */
    @GetMapping("/reservations/{reservationId}")
    public String showReservation(
            @PathVariable("reservationId") int reservationId,
            Model model) {

        // 予約番号から予約情報を取得
        Reservations reservation =
                reservationMapper.findById(reservationId);

        // 予約が存在しない場合
        if (reservation == null) {
            return "redirect:/shops";
        }

        // 店舗情報を取得
        Stores store =
                storesMapper.selectById(reservation.getStore_id());

        // HTMLへ渡す
        model.addAttribute("reservation", reservation);
        model.addAttribute("store", store);

        return "reservations/reservation";
    }
}

