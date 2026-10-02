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
     * =========================================
     * ① 予約画面表示
     *
     * GET
     * /shops/{shopId}/reservations/new
     * =========================================
     */

    @GetMapping("/shops/{shopId}/reservations/new")
    public String showReservation(
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


        // 予約画面
        return "reservations/newreserve";
    }



    /*
     * =========================================
     * ② 予約登録
     *
     * POST
     * /shops/{shopId}/reservations
     * =========================================
     */

    @PostMapping("/shops/{shopId}/reservations")
    public String reserve(
            @PathVariable("shopId") int shopId,
            @RequestParam("date") String date,
            @RequestParam("time") String time) {


        // 予約情報を作成
        Reservations reservation = new Reservations();


        /*
         * 現在はログイン機能と接続していないため
         * テスト用として会員ID「1」を使用
         */
        reservation.setMember_id(1);


        // 店舗ID
        reservation.setStore_id(shopId);


        // 予約日時
        LocalDateTime reservationDate =
                LocalDateTime.parse(date + "T" + time);

        reservation.setReservation_date(reservationDate);


        // 予約状態
        reservation.setStatus("予約確定");


        // 登録日時
        reservation.setCreated_at(LocalDateTime.now());


        // DBへ登録
        reservationMapper.insertReservation(reservation);


        /*
         * DB登録後に自動採番された
         * 予約番号を使って予約確認画面へ移動
         */

        return "redirect:/reservations/" + reservation.getId();
    }



    /*
     * =========================================
     * ③ 予約確認
     *
     * GET
     * /reservations/{reservationId}
     * =========================================
     */

    @GetMapping("/reservations/{reservationId}")
    public String showReservation(
            @PathVariable("reservationId") int reservationId,
            Model model) {


        // 予約番号から予約情報取得
        Reservations reservation =
                reservationMapper.findById(reservationId);


        // 予約が存在しない場合
        if (reservation == null) {

            return "redirect:/shops";
        }


        // 予約に紐づく店舗を取得
        Stores store =
                storesMapper.selectById(
                        reservation.getStore_id()
                );


        // HTMLへ渡す
        model.addAttribute(
                "reservation",
                reservation
        );

        model.addAttribute(
                "store",
                store
        );


        // 予約確認画面
        return "reservations/reservation";
    }
}