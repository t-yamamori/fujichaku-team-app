package com.example.demo.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.entity.Reservations;
import com.example.demo.mapper.ReservationMapper;

@Controller
public class ReservationController {

    // ReservationMapperを使えるようにする
    private final ReservationMapper reservationMapper;

    // コンストラクタ
    public ReservationController(ReservationMapper reservationMapper) {
        this.reservationMapper = reservationMapper;
    }


    // ① 予約画面表示
    // ③ 予約確認
    @GetMapping({
        "/shops/{shopId}/reservations/new",
        "/reservations/{reservationId}"
    })
    public String showReservation(
            @PathVariable Map<String, String> pathVariables,
            Model model) {

        // 予約画面
        if (pathVariables.containsKey("shopId")) {

            Long shopId = Long.valueOf(pathVariables.get("shopId"));

            model.addAttribute("shopId", shopId);

            return "reservations/newreserve";
        }

        // 予約確認画面
        Long reservationId =
                Long.valueOf(pathVariables.get("reservationId"));

        model.addAttribute("reservationId", reservationId);

        return "reservations/reservation";
    }


    // ② 店舗予約する
    @PostMapping("/shops/{shopId}/reservations")
    public String reserve(
            @PathVariable Long shopId,
            @RequestParam("date") String date,
            @RequestParam("time") String time,
            @RequestParam("number") Integer number) {

        // 予約情報を作成
        Reservations reservation = new Reservations();

        // 会員ID
        // ※現在はテスト用に1を設定
        reservation.setMember_id(1);

        // 予約日時
        reservation.setReservation_date(
                LocalDateTime.parse(date + "T" + time)
        );

        // 予約状況
        reservation.setStatus("予約済み");

        // 登録時間
        reservation.setCreated_at(LocalDateTime.now());

        // 店舗ID
        reservation.setStore_id(shopId.intValue());


        // DBに予約情報を登録
        reservationMapper.insertReservation(reservation);


        // DB登録後、自動採番された予約IDを取得
        Long reservationId = (long) reservation.getId();


        // 予約確認画面へ
        return "redirect:/reservations/" + reservationId;
    }


    // ④ 予約履歴一覧表示
    @GetMapping("/reservations/history")
    public String showHistory(Model model) {

        // 現在はまだDBから取得していない

        return "reservations/history";
    }
}