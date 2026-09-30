package com.example.demo.controller;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ReservationController {

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

        // ここで予約登録処理を行う

        // のちにDBから番号を取るから今は仮の予約番号　あとで消す
        Long reservationId = 1L;

        return "redirect:/reservations/" + reservationId;
    }
    
 // ④ 予約履歴一覧表示
    @GetMapping("/reservations/history")
    public String showHistory(Model model) {

        // 仮の予約履歴データ
        // DB連携する場合は後でRepositoryから取得する

        return "reservations/history";
    }
}