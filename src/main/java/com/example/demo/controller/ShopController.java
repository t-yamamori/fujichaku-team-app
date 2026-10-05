package com.example.demo.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.entity.Reservations;
import com.example.demo.entity.Reviews;
import com.example.demo.entity.Stores;
import com.example.demo.mapper.MembersMapper;
import com.example.demo.mapper.ReviewsMapper;
import com.example.demo.mapper.StoresMapper;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/shops")
public class ShopController {

	//フィールド
	private final StoresMapper storesMapper;
	private final MembersMapper membersMapper;
	private final ReviewsMapper reviewsMapper;
	
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

	//店舗詳細表示  GET /shops/{shopName}
	@GetMapping("/{shopName}")
	public String showDetail(@PathVariable("shopName") String shopName, Model model) {

		Stores st = storesMapper.selectDetailByName(shopName);

		// 店舗が見つからなければ一覧画面へ戻す
		if (st == null) {
			return "redirect:/shops";
		}

		// TODO: ログイン機能ができたらセッションから取得する
		Integer loginId = 123;

		// loginIdが会員(Membersテーブル)に存在するか
		boolean isMember = loginId != null && membersMapper.existsById(loginId);

		// 店舗IDは取得済みの店舗詳細(st)から使う
		int storeId = st.getId();

		List<Reviews> rv = new ArrayList<>();
		List<Reservations> rs = new ArrayList<>();

		if (isMember) {
			// 会員 → 口コミ全件を表示する
			rv = reviewsMapper.selectAllReviews(storeId);
			// 会員 → この店舗に対する自分の予約を取得する（reservations と stores をJOIN）
			// ReservationMapper.java は変更せず、ReservationMapper.xml の
			// selectByStoreIdAndMemberId を「namespace + id」で直接呼び出す
			Map<String, Object> params = new HashMap<>();
			params.put("storeId", storeId);   // XMLの #{storeId} に対応
			params.put("memberId", loginId);  // XMLの #{memberId} に対応
			rs = sqlSession.selectList(
					"com.example.demo.mapper.ReservationMapper.selectByStoreIdAndMemberId",
					params);
		} else {
			// 非会員 → 口コミ1件のみ表示する
			Reviews one = reviewsMapper.selectOneReview(storeId);
			if (one != null) {
				rv.add(one);
			}
		}

		model.addAttribute("store", st);
		model.addAttribute("reviews", rv);
		model.addAttribute("reservations", rs);
		model.addAttribute("isMember", isMember);
		return "shops/detail";
	}

}