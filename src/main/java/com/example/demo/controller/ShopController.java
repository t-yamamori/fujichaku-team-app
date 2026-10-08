package com.example.demo.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.apache.ibatis.session.SqlSession;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.entity.Members;
import com.example.demo.entity.Reservations;
import com.example.demo.entity.Reviews;
import com.example.demo.entity.Stores;
import com.example.demo.mapper.StoresMapper;
import com.example.demo.service.ReviewService;
import com.example.demo.service.serviceInterface.MembersService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/shops")
public class ShopController {

	//フィールド
	private final StoresMapper storesMapper;

	// 会員(members)へのアクセスは必ず Service を通す
	// 型はインターフェース(MembersService)にしておき、実体は Spring が MembersServiceImpl を入れてくれる
	private final MembersService membersService;

	// 口コミ(reviews)へのアクセスは必ず Service を通す
	private final ReviewService reviewService;

	// ReservationMapper.java に宣言のないSQLを、XMLのIDを指定して直接呼ぶために使う
	private final SqlSession sqlSession;

	//店舗一覧表示（トップ画面）  GET /shops
	//同じ shops.html の中で「未ログイン」と「ログイン済み」の表示を切り替える
	@GetMapping("")
	public String showShops(Model model) {
		List<Stores> st = storesMapper.selectList();
		model.addAttribute("stores", st);

		// ログイン状態を画面に渡す（右上のボタン切り替えに使う）
		Integer loginId = getLoginId();
		model.addAttribute("isMember", isMember(loginId));
		model.addAttribute("loginId", loginId);
		return "shops";
	}

	//ログアウト  POST /shops/logout
	//セッションを破棄して、トップ画面(shops.html)へリダイレクトする
	@PostMapping("/logout")
	public String logout(HttpSession session, RedirectAttributes redirectAttributes) {

		// セッションに保存したログイン情報をまとめて破棄する
		session.invalidate();

		redirectAttributes.addFlashAttribute("message", "ログアウトしました。");
		return "redirect:/shops";
	}

	//ログイン中の会員IDを返す。未ログインなら null
	//会員の探し方は MembersService の findByLoginId にまとめてある（try 文はそちらだけ）
	private Integer getLoginId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();

		// 未ログイン（ログインしていない人は「匿名ユーザー」として扱われるので、それも除く）
		if (auth == null || auth instanceof AnonymousAuthenticationToken) {
			return null;
		}

		// ログインID（会員IDの文字列）で会員を探す。見つからなければ null
		Members member = membersService.findByLoginId(auth.getName());
		return member != null ? member.getId() : null;
	}

	//会員かどうかを判定する
	//getLoginId() の中で MembersService 経由で会員を検索済み（退会済みは除外済み）なので、
	//loginId が null でなければ「有効な会員」と判断できる（Mapper を再度呼ぶ必要はない）
	private boolean isMember(Integer loginId) {
		return loginId != null;
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
	 * 店舗詳細表示・口コミ追加・口コミ削除・口コミ履歴一覧
	 *
	 *   GET  /shops/{shopId}                              → 店舗詳細表示
	 *   GET  /shops/{shopId}  action=history              → showReviewHistory() を呼ぶ（口コミ履歴一覧）
	 *   POST /shops/{shopId}  action=add,    comment, grade → addReview() を呼ぶ
	 *   POST /shops/{shopId}  action=delete, reviewId       → deleteReview() を呼ぶ
	 *
	 * addReview / deleteReview / showReviewHistory は同じクラス内の private メソッドで、
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

		// ログイン中の会員ID（未ログインなら null）と、会員かどうか
		Integer loginId = getLoginId();
		boolean isMember = isMember(loginId);

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
		// GET：口コミ履歴一覧（会員のみ利用可）
		// ==================================================
		if ("history".equals(action)) {
			if (!isMember) {
				// 非会員がURL直接入力などで来た場合は詳細画面へ戻す
				redirectAttributes.addFlashAttribute("errorMessage", "会員の方は口コミ履歴一覧を閲覧できます。");
				return "redirect:/shops/" + shopId;
			}
			return showReviewHistory(st, model);
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

	//口コミ履歴一覧（showDetail から呼び出される）
	//会員のみ。表示中の店舗の口コミをすべて表示する（会員チェックは showDetail で実施済み）
	private String showReviewHistory(Stores st, Model model) {

		// Service を通して、この店舗の口コミ（全件・新しい順）を取得
		// ReviewController の口コミ一覧と同じメソッドを使い、取得方法をそろえる
		List<Reviews> history = reviewService.findAllByStoreId(st.getId());

		// 既存テンプレート templates/reviews/reviews.html に合わせた属性名
		//   reviews : 口コミのリスト（th:each="review : ${reviews}"）
		//   shopId  : 「店舗詳細に戻る」リンク（@{/shops/{id}(id=${shopId})}）
		model.addAttribute("reviews", history);
		model.addAttribute("shopId", st.getId());
		return "reviews/reviews";
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

	//口コミ削除（showDetail から呼び出される。）
	private void deleteReview(int shopId, int loginId, Integer reviewId,
			RedirectAttributes redirectAttributes) {

		// Service を通して削除（自分の口コミのときだけ削除される。）
		boolean deleted = reviewId != null && reviewService.deleteReview(reviewId, shopId, loginId);

		if (deleted) {
			redirectAttributes.addFlashAttribute("message", "口コミを削除しました");
		} else {
			redirectAttributes.addFlashAttribute("errorMessage", "削除できる口コミが見つかりませんでした。");
		}
	}

		
}