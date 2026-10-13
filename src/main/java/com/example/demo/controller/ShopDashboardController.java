package com.example.demo.controller;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
    private final ReservationMapper reservationMapper;

    // ログイン中の店舗管理者を shop_admins テーブルで確認するために使う
    private final ShopAdminMapper shopAdminMapper;

    /*
     * 店舗管理者向けの自店舗の店舗詳細
     *
     *   【変更】URLを /shops/{storeId} → /shop-admin/{storeId} に変更
     *     変更前は ShopController の店舗詳細（/shops/{shopId}）と同じURLで、
     *     利用者が店舗一覧から店舗をタップしたときにこちらが呼ばれてしまっていた。
     *
     *   ShopController.showDetail から forward で呼ばれる（ブラウザのURLは /shops/{storeId} のまま）
     *     GET  → 店舗詳細（利用者の会員と同じ内容 ＋ 予約履歴 ＋ 口コミ削除ボタン）を表示
     *     POST action=delete, reviewId → 自店舗の口コミを削除し、/shops/{storeId} へ戻す
     *
     *   戻り値について：
     *     ここで "redirect:/shops/{storeId}" を返すと、showDetail → ここ → showDetail … と
     *     無限ループになるため、GET では店舗詳細のテンプレート（shops/detail）をそのまま返す。
     *     URLは /shops/{storeId} のままなので、見た目は「店舗管理者用の /shops/{id}」になる。
     */
    @RequestMapping(value = "/shop-admin/{storeId:\\d+}", method = { RequestMethod.GET, RequestMethod.POST })
    public String dashboard(
            @PathVariable("storeId") int storeId,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "reviewId", required = false) Integer reviewId,
            HttpMethod method,
            Model model,
            RedirectAttributes redirectAttributes) {

        // ==================================================
        // 本人確認：自店舗の店舗管理者だけが使える
        // （/shop-admin/{storeId} を直接開かれても、他人・他店舗には見せない）
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
        // POST：口コミ削除（店舗管理者は自店舗の口コミなら誰の口コミでも削除できる）
        // ==================================================
        if (method == HttpMethod.POST) {

            if ("delete".equals(action)) {
                deleteReviewByShopAdmin(storeId, reviewId, redirectAttributes);
            } else {
                // 店舗管理者は口コミ投稿などはできない
                redirectAttributes.addFlashAttribute("errorMessage", "不正な操作です。");
            }

            // 処理後は店舗詳細(GET)へ戻す（再読み込みでの二重送信を防ぐ）
            return "redirect:/shops/" + storeId;
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
     * 【追加】店舗管理者のログアウト  POST /shop-admin/logout
     *
     *   店舗詳細画面（detail.html）の「ログアウト」ボタンから呼ばれる（店舗管理者のみ表示）。
     *   ログイン情報とセッションを破棄して、トップ画面（/shops）へリダイレクトする。
     *   ログアウト後は未ログイン扱いなので、shops.html は「会員登録／ログイン」側の表示になる。
     *
     *   利用者（会員）のログアウト（ShopController の POST /shops/logout）とは分けてあるので、
     *   利用者側の処理には影響しない。
     *   URLの "logout" は数字ではないので、/shop-admin/{storeId:\\d+} とはぶつからない。
     */
    @PostMapping("/shop-admin/logout")
    public String logout(
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {

        // Spring Security 標準のログアウト処理
        // （ログイン情報(SecurityContext)のクリアと、セッションの破棄をまとめて行う）
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        new SecurityContextLogoutHandler().logout(request, response, auth);

        // トップ画面に「ログアウトしました。」を表示する（shops.html の成功メッセージ欄）
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