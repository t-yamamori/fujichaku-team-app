package com.example.demo.controller;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.entity.Reservations;
import com.example.demo.entity.Reviews;
import com.example.demo.entity.ShopAdmin;
import com.example.demo.entity.Stores;
import com.example.demo.mapper.ReservationMapper;
import com.example.demo.mapper.ReviewsMapper;
import com.example.demo.mapper.ShopAdminMapper;
import com.example.demo.mapper.StoresMapper;
import com.example.demo.service.ReviewService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ShopDashboardController {

    //フィールド
    private final StoresMapper storesMapper;

    // 口コミ(reviews)の取得は Service を通す（会員の店舗詳細と同じメソッドを使う）
    private final ReviewService reviewService;

    // 店舗管理者用の口コミ削除（ReviewsMapper に追加した deleteByIdAndStoreId を使う）
    private final ReviewsMapper reviewsMapper;

    // 店舗の予約一覧（既存の findByStoreId を使う。会員・非会員とも全件）
    // 【追加】予約の編集（既存の updateForStore / updateStatusForStore を使う）
    private final ReservationMapper reservationMapper;

    // ログイン中の店舗管理者を shop_admins テーブルで確認するために使う
    private final ShopAdminMapper shopAdminMapper;

    /*
     * 店舗管理者向けの自店舗の店舗詳細
     *
     *   ShopController.showDetail から forward で呼ばれる（ブラウザのURLは /shops/{storeId} のまま）
     *     GET  → 店舗詳細（利用者の会員と同じ内容 ＋ 予約履歴 ＋ 口コミ削除ボタン）を表示
     *     POST action=delete, reviewId → 自店舗の口コミを削除し、/shops/{storeId} へ戻す
     *     【追加】POST action=updateReservation → 自店舗の予約を編集し、/shops/{storeId} へ戻す
     *            （detail.html の予約履歴の「保存」から、/shop-admin/{storeId} に直接送られてくる）
     *
     *   戻り値について：
     *     ここで "redirect:/shops/{storeId}" を返すと、showDetail → ここ → showDetail … と
     *     無限ループになるため、GET では店舗詳細のテンプレート（shops/detail）をそのまま返す。
     */
    @RequestMapping(value = "/shop-admin/{storeId:\\d+}", method = { RequestMethod.GET, RequestMethod.POST })
    public String dashboard(
            @PathVariable("storeId") int storeId,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "reviewId", required = false) Integer reviewId,
            // 【追加】予約の編集で使う値
            @RequestParam(value = "reservationId", required = false) Integer reservationId,
            @RequestParam(value = "reservationDate", required = false)
                @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime reservationDate,
            @RequestParam(value = "number", required = false) Integer number,
            @RequestParam(value = "status", required = false) String status,
            HttpMethod method,
            Model model,
            RedirectAttributes redirectAttributes) {

        // ==================================================
        // 本人確認：自店舗の店舗管理者だけが使える
        // ==================================================
        ShopAdmin shopAdmin = getLoginShopAdmin();
        if (shopAdmin == null || shopAdmin.getStoreId() == null
                || shopAdmin.getStoreId() != storeId) {
            return "redirect:/shops";
        }

        // 店舗情報を取得。見つからなければ一覧へ
        Stores st = storesMapper.selectById(storeId);
        if (st == null) {
            return "redirect:/shops";
        }

        // ==================================================
        // POST：口コミ削除・【追加】予約の編集
        // ==================================================
        if (method == HttpMethod.POST) {

            if ("delete".equals(action)) {
                deleteReviewByShopAdmin(storeId, reviewId, redirectAttributes);
            } else if ("updateReservation".equals(action)) {
                // 【追加】予約の編集
                updateReservation(storeId, reservationId, reservationDate, number, status, redirectAttributes);
            } else {
                // 店舗管理者は口コミ投稿などはできない
                redirectAttributes.addFlashAttribute("errorMessage", "不正な操作です。");
            }
            // 処理後は店舗詳細(GET)へ戻す（再読み込みでの二重送信を防ぐ）
            // 【変更】予約の編集のときは、予約履歴の位置（#rsv）まで自動でスクロールさせる
            String anchor = "updateReservation".equals(action) ? "#rsv" : "";
            return "redirect:/shop-admin/" + storeId + anchor;
        }

        // ==================================================
        // GET：店舗管理者用の店舗詳細
        // ==================================================

        // 口コミ：会員と同じく全件表示（新しい順）
        List<Reviews> rv = reviewService.findAllByStoreId(storeId);

        // 予約履歴：この店舗の予約を全件（会員・非会員とも、予約日時の新しい順）
        List<Reservations> storeReservations = reservationMapper.findByStoreId(storeId);

        model.addAttribute("store", st);
        model.addAttribute("reviews", rv);
        model.addAttribute("storeReservations", storeReservations);

        // detail.html の表示切り替え用
        model.addAttribute("isMember", false);    // 会員用の口コミ投稿・「あなたの予約」は出さない
        model.addAttribute("isShopAdmin", true);  // 店舗管理者用の予約履歴・削除ボタンを出す
        model.addAttribute("loginId", null);
        model.addAttribute("reservations", List.of()); // 会員用の項目（未使用だが空で渡しておく）

        return "shops/detail";
    }

    /*
     * 店舗管理者のログアウト  POST /shop-admin/logout
     */
    @PostMapping("/shop-admin/logout")
    public String logout(
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {

        // Spring Security 標準のログアウト処理
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        new SecurityContextLogoutHandler().logout(request, response, auth);

        redirectAttributes.addFlashAttribute("message", "ログアウトしました。");
        return "redirect:/shops";
    }

    //口コミ削除（店舗管理者用）
    //口コミID・店舗IDが一致したときだけ削除される（他店舗の口コミは消せない）
    private void deleteReviewByShopAdmin(int storeId, Integer reviewId,
            RedirectAttributes redirectAttributes) {

        int count = (reviewId != null) ? reviewsMapper.deleteByIdAndStoreId(reviewId, storeId) : 0;

        if (count > 0) {
            redirectAttributes.addFlashAttribute("message", "口コミを削除しました");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "削除できる口コミが見つかりませんでした。");
        }
    }

    //【追加】予約の編集（店舗管理者用）
    //既存の updateForStore（日時・人数）と updateStatusForStore（予約状況）を使う
    //どちらも店舗IDを条件に入れているので、他店舗の予約は変更できない
    private void updateReservation(int storeId, Integer reservationId, LocalDateTime reservationDate,
            Integer number, String status, RedirectAttributes redirectAttributes) {

        // 入力チェック
        if (reservationId == null || reservationDate == null) {
            redirectAttributes.addFlashAttribute("rsvErrorMessage", "予約日時を入力してください。");
            return;
        }
        if (number == null || number < 1) {
            redirectAttributes.addFlashAttribute("rsvErrorMessage", "人数は1名以上で入力してください。");
            return;
        }
        if (status == null || status.isBlank()) {
            redirectAttributes.addFlashAttribute("rsvErrorMessage", "予約状況を選択してください。");
            return;
        }

        // ① 日時・人数を変更（キャンセル済みの予約は SQL 側で変更されない）
        reservationMapper.updateForStore(reservationId, storeId, reservationDate, number);

        // ② 予約状況を変更
        int count = reservationMapper.updateStatusForStore(reservationId, storeId, status);

        if (count > 0) {
            redirectAttributes.addFlashAttribute("rsvMessage", "予約内容を変更しました。");
        } else {
            redirectAttributes.addFlashAttribute("rsvErrorMessage", "変更できる予約が見つかりませんでした。");
        }
    }

    //ログイン中の店舗管理者を返す。店舗管理者でなければ null
    //（ShopController の getLoginShopAdmin と同じ処理）
    private ShopAdmin getLoginShopAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }

        String name = auth.getName();
        if (name == null || !name.matches("\\d{1,9}")) {
            return null;
        }

        return shopAdminMapper.findById(Integer.parseInt(name));
    }
}