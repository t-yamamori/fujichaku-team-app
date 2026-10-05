package com.example.demo.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.entity.Reservations;
import com.example.demo.entity.Reviews;
import com.example.demo.entity.Stores;
import com.example.demo.mapper.MembersMapper;
import com.example.demo.mapper.StoresMapper;
import com.example.demo.service.ReviewService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/shops")
public class ShopController {

	//フィールド
	private final StoresMapper storesMapper;
	private final MembersMapper membersMapper;

	// 口コミ(reviews)へのアクセスは必ず Service を通す
	private final ReviewService reviewService;

	// ReservationMapper.java に宣言のないSQLを、XMLのIDを指定して直接呼ぶために使う
	private final SqlSession sqlSession;

	//店舗一覧表示  GET /shops
	@GetMapping("")
	public String showShops(Model model) {
		List<Stores> st = storesMapper.selectList();
		model.addAttribute("stores", st);
		return "shops";
	}

	//店舗を検索（検索画面表示）  GET /shops/search
	@GetMapping("/search")
	public String searchShops() {
		return "shops/search";
	}

	//検索した店舗を表示  GET /shops/search?name=xxx
	//検索キーワード(name)が付いているときだけこちらが呼ばれる
	@GetMapping(value = "/search", params = "name")
	public String showSearchResults(@RequestParam("name") String name, Model model) {
		List<Stores> st = storesMapper.selectByName(name);
		model.addAttribute("stores", st);
		model.addAttribute("name", name);
		return "shops/search";
	}

	/*
	 * 店舗詳細表示・口コミ追加・口コミ削除
	 *
	 *   GET  /shops/{shopId}                              → 店舗詳細表示
	 *   POST /shops/{shopId}  action=add,    comment, grade → addReview() を呼ぶ
	 *   POST /shops/{shopId}  action=delete, reviewId       → deleteReview() を呼ぶ
	 *
	 * addReview / deleteReview は同じクラス内の private メソッドで、
	 * そこから ReviewService → ReviewsMapper → reviews テーブル の順にアクセスする。
	 *
	 * {shopId:\\d+} で数字のときだけこのメソッドに来るようにしている
	 * （/shops/abc のような不正なURLで型変換エラーにならないようにするため）
	 */
	@RequestMapping(value = "/{shopId:\\d+}", method = { RequestMethod.GET, RequestMethod.POST })
	public String showDetail(
			@PathVariable("shopId") int shopId,
			@RequestParam(value = "action", required = false) String action,
			@RequestParam(value = "comment", required = false) String comment,
			@RequestParam(value = "grade", required = false) Integer grade,
			@RequestParam(value = "reviewId", required = false) Integer reviewId,
			HttpMethod method,
			Model model,
			RedirectAttributes redirectAttributes) {

		// 店舗IDから店舗情報を取得
		Stores st = storesMapper.selectById(shopId);

		// 店舗が見つからなければ一覧画面へ戻す
		if (st == null) {
			return "redirect:/shops";
		}

		// TODO: ログイン機能ができたらセッションから取得する
		Integer loginId = 123;

		// loginIdが会員(Membersテーブル)に存在するか
		boolean isMember = loginId != null && membersMapper.existsById(loginId);

		// ==================================================
		// POST：口コミ追加・口コミ削除
		// ==================================================
		if (method == HttpMethod.POST) {

			if (!isMember) {
				// 口コミの追加・削除は会員のみ
				redirectAttributes.addFlashAttribute("errorMessage", "口コミの投稿・削除は会員のみ利用できます。");
			} else if ("add".equals(action)) {
				addReview(shopId, loginId, comment, grade, redirectAttributes);
			} else if ("delete".equals(action)) {
				deleteReview(shopId, loginId, reviewId, redirectAttributes);
			} else {
				redirectAttributes.addFlashAttribute("errorMessage", "不正な操作です。");
			}

			// 処理後は詳細画面(GET)へ戻す（再読み込みでの二重投稿を防ぐ）
			return "redirect:/shops/" + shopId;
		}

		// ==================================================
		// GET：店舗詳細表示
		// ==================================================
		List<Reviews> rv = new ArrayList<>();
		List<Reservations> rs = new ArrayList<>();

		if (isMember) {
			// 会員 → 口コミ全件を表示する
			rv = reviewService.findAllByStoreId(shopId);

			// 会員 → この店舗に対する自分の予約を取得する（reservations と stores をJOIN）
			// ReservationMapper.java は変更せず、ReservationMapper.xml の
			// selectByStoreIdAndMemberId を「namespace + id」で直接呼び出す
			Map<String, Object> params = new HashMap<>();
			params.put("storeId", shopId);    // XMLの #{storeId} に対応
			params.put("memberId", loginId);  // XMLの #{memberId} に対応
			rs = sqlSession.selectList(
					"com.example.demo.mapper.ReservationMapper.selectByStoreIdAndMemberId",
					params);
		} else {
			// 非会員 → 口コミ1件のみ表示する
			Reviews one = reviewService.findLatestByStoreId(shopId);
			if (one != null) {
				rv.add(one);
			}
		}

		model.addAttribute("store", st);
		model.addAttribute("reviews", rv);
		model.addAttribute("reservations", rs);
		model.addAttribute("isMember", isMember);
		// 自分の口コミにだけ削除ボタンを出す判定に使う
		model.addAttribute("loginId", loginId);
		return "shops/detail";
	}

	//口コミ追加（showDetail から呼び出される）
	private void addReview(int shopId, int loginId, String comment, Integer grade,
			RedirectAttributes redirectAttributes) {

		// 入力チェック
		if (comment == null || comment.isBlank()) {
			redirectAttributes.addFlashAttribute("errorMessage", "口コミを入力してください。");
			return;
		}
		if (grade == null || grade < 1 || grade > 5) {
			redirectAttributes.addFlashAttribute("errorMessage", "評価は1～5で選択してください。");
			return;
		}

		// Service を通して登録
		reviewService.addReview(shopId, loginId, comment.trim(), grade);
		redirectAttributes.addFlashAttribute("message", "口コミを投稿しました。");
	}

	//口コミ削除（showDetail から呼び出される）
	private void deleteReview(int shopId, int loginId, Integer reviewId,
			RedirectAttributes redirectAttributes) {

		// Service を通して削除（自分の口コミのときだけ削除される）
		boolean deleted = reviewId != null && reviewService.deleteReview(reviewId, shopId, loginId);

		if (deleted) {
			redirectAttributes.addFlashAttribute("message", "口コミを削除しました。");
		} else {
			redirectAttributes.addFlashAttribute("errorMessage", "削除できる口コミが見つかりませんでした。");
		}
	}

}