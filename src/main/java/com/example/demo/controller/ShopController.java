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
import com.example.demo.entity.ShopAdmin;
import com.example.demo.entity.Stores;
import com.example.demo.mapper.ReviewsMapper;
import com.example.demo.mapper.ShopAdminMapper;
import com.example.demo.mapper.StoresMapper;
import com.example.demo.service.ReviewService;
import com.example.demo.service.serviceInterface.MembersService;
import com.example.demo.util.ReadingConverter;
import com.example.demo.util.SearchTextUtil;

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

	// 【追加】ログインした人が店舗管理者かどうかを shop_admins テーブルで判定するために使う
	private final ShopAdminMapper shopAdminMapper;

	// ReservationMapper.java に宣言のないSQLを、XMLのIDを指定して直接呼ぶために使う
	private final SqlSession sqlSession;

	// 【追加】店舗検索で、入力された漢字を読みがな（ひらがな）に変換するために使う
	private final ReadingConverter readingConverter;
	
	// 【追加】口コミの平均・件数を出すために使う（ShopDashboardController と同じ使い方）
	private final ReviewsMapper reviewsMapper;

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

	//【追加】ログイン中の店舗管理者を返す。店舗管理者でなければ（未ログイン・会員・モール管理者）null
	//判定は shop_admins テーブルで行う（論理削除済みの店舗管理者は findById の SQL で除外済み）
	private ShopAdmin getLoginShopAdmin() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();

		// 未ログイン（匿名ユーザーも除く）
		if (auth == null || auth instanceof AnonymousAuthenticationToken) {
			return null;
		}

		// ログインIDは数字の文字列。数字以外なら店舗管理者ではない
		// （先に数字かどうかを確かめるので、Integer.parseInt で例外は起きない）
		String name = auth.getName();
		if (name == null || !name.matches("\\d{1,9}")) {
			return null;
		}

		// shop_admins テーブルを ID で検索。見つからなければ null
		return shopAdminMapper.findById(Integer.parseInt(name));
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
	//【変更】ひらがな・カタカナ・漢字・英字のどれで入力しても検索できるようにした
	//  例：「かふぇ」「カフェ」「ｶﾌｪ」「CAFE」「ＣＡＦＥ」→ Cafe Aozora（name_kana = かふぇあおぞら）
	//      「珈琲」「こーひー」→ 珈琲館、コーヒー工房
	@GetMapping(value = "/search", params = "name")
	public String showSearchResults(@RequestParam("name") String name, Model model) {

		// 空白だけの入力は検索しない（結果0件として表示）
		if (name == null || name.isBlank()) {
			model.addAttribute("stores", new ArrayList<Stores>());
			model.addAttribute("name", name);
			return "shops/search";
		}

		// ① 全角・半角を揃えたもの → stores.name（漢字・英字）と比較（ｓ → s、ＣＡＦＥ → CAFE）
		String keyword = SearchTextUtil.escapeLike(SearchTextUtil.normalizeWidth(name));
		// ② ひらがなに揃えたもの → stores.name_kana と比較（カフェ → かふぇ）
		String kana = SearchTextUtil.escapeLike(SearchTextUtil.normalize(name));
		// ③ 漢字を読みに変換     → stores.name_kana と比較（珈琲 → こーひー）
		String reading = SearchTextUtil.escapeLike(readingConverter.toReading(name.trim()));

		List<Stores> st = storesMapper.selectByKeyword(keyword, kana, reading);
		model.addAttribute("stores", st);
		model.addAttribute("name", name);  // 画面には入力そのままを表示する
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
	 *   【追加】ログインしたのが店舗管理者の場合
	 *     → ShopDashboardController（/shop-admin/{storeId}）へ forward する
	 *       forward なのでブラウザのURLは /shops/{shopId} のまま、
	 *       画面は ShopDashboardController が作った「店舗管理者用の店舗詳細」になる
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

		// ==================================================
		// 【追加】店舗管理者：ShopDashboardController へ移動
		// ==================================================
		// 口コミ投稿・削除（POST）より前に判定するので、店舗管理者の操作はすべて
		// ShopDashboardController 側で処理される（会員用の口コミ投稿は使えない）
		ShopAdmin shopAdmin = getLoginShopAdmin();
		if (shopAdmin != null) {

			// 管理対象の店舗が設定されていない店舗管理者（データ不備）
			if (shopAdmin.getStoreId() == null) {
				redirectAttributes.addFlashAttribute("errorMessage", "管理対象の店舗が設定されていません。");
				return "redirect:/shops";
			}

			// 他店舗のURLを直接開いた場合 → 自店舗の店舗詳細へ移動させる
			if (shopId != shopAdmin.getStoreId()) {
				redirectAttributes.addFlashAttribute("errorMessage",
						"他店舗の店舗詳細は表示できません。自店舗の店舗詳細を表示しています。");
				return "redirect:/shops/" + shopAdmin.getStoreId();
			}

			// 自店舗 → ShopDashboardController に処理を任せる
			// （redirect ではなく forward なので、URLは /shops/{shopId} のまま。無限ループにもならない）
			return "forward:/shop-admin/" + shopId;
		}

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
		// 【追加】detail.html は店舗管理者と共用なので、利用者の場合は false を渡しておく
		model.addAttribute("isShopAdmin", false);
		// 自分の口コミにだけ削除ボタンを出す判定に使う
		model.addAttribute("loginId", loginId);
		// 【追加】口コミの平均と件数（会員・非会員どちらも、全部の口コミで計算する）
		model.addAttribute("averageGrade", reviewsMapper.selectAverageGrade(shopId));
		model.addAttribute("reviewCount", reviewsMapper.countByStoreId(shopId));
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
		// ★追加：口コミ投稿のポイントを加算（会員チェックは showDetail で実施済み）
		membersService.addPoint(loginId, MembersService.REVIEW_POINT, "REVIEW");

		redirectAttributes.addFlashAttribute("message",
				"口コミを投稿しました。（+" + MembersService.REVIEW_POINT + "P）");

	}

	//口コミ削除（showDetail から呼び出される。）
	private void deleteReview(int shopId, int loginId, Integer reviewId,
			RedirectAttributes redirectAttributes) {

		// Service を通して削除（自分の口コミのときだけ削除される。）
		boolean deleted = reviewId != null && reviewService.deleteReview(reviewId, shopId, loginId);

		if (deleted) {
			// ★追加：口コミ投稿でもらったポイントを取り消す（マイナスの値を渡すと減る）
			membersService.addPoint(loginId, -MembersService.REVIEW_POINT, "REVIEW");

			redirectAttributes.addFlashAttribute("message",
					"口コミを削除しました。（−" + MembersService.REVIEW_POINT + "P）");
		} else {
			redirectAttributes.addFlashAttribute("errorMessage", "削除できる口コミが見つかりませんでした。");
		}
	}

}