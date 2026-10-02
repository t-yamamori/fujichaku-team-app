package com.example.demo.controller;

import java.util.ArrayList;
import java.util.List;

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
import com.example.demo.mapper.ReservationMapper;
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
	private final ReservationMapper reservationMapper;

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
<<<<<<< HEAD
//>>>>>>> refs/heads/master
=======
>>>>>>> branch 'master' of https://github.com/t-yamamori/fujichaku-team-app.git

	//検索した店舗を表示  GET /shops/search?name=xxx
	//検索キーワード(name)が付いているときだけこちらが呼ばれる
	@GetMapping(value = "/search", params = "name")
	public String showSearchResults(@RequestParam("name") String name, Model model) {
		List<Stores> st = storesMapper.selectByName(name);
		model.addAttribute("stores", st);
		model.addAttribute("name", name);
		return "shops/search";
	}

<<<<<<< HEAD
//<<<<<<< HEAD
    // 検索した店舗を表示
    @GetMapping("/search/{name}")
    public String showSearchResults(
            @PathVariable String name,
            Model model) {
=======
	@GetMapping("/{name}")
	public String showDetail(@PathVariable String name,Model model) {
         
		Stores st = storesMapper.selectDetailByName(name);
		
		  // 店舗が見つからなければ一覧画面へ戻す
	    if (st == null) {
	        return "redirect:/shops";
	    }
		
=======
	//店舗詳細表示  GET /shops/{shopName}
	@GetMapping("/{shopName}")
	public String showDetail(@PathVariable("shopName") String shopName, Model model) {

		Stores st = storesMapper.selectDetailByName(shopName);

		// 店舗が見つからなければ一覧画面へ戻す
		if (st == null) {
			return "redirect:/shops";
		}

>>>>>>> branch 'master' of https://github.com/t-yamamori/fujichaku-team-app.git
		//★★★★★試しに置いているloginIdなので完成までに削除すること★★★★★
		Integer loginId = 123;
<<<<<<< HEAD
		
		// Membersテーブルから全員のIDを取得
		List<Integer> memberIds = membersMapper.selectAllIds();
//>>>>>>> refs/heads/master
=======
>>>>>>> branch 'master' of https://github.com/t-yamamori/fujichaku-team-app.git

<<<<<<< HEAD
//<<<<<<< HEAD
        List<Stores> st = storesMapper.selectByName(name);
//=======
=======
		// loginIdが会員(Membersテーブル)に存在するか
		boolean isMember = loginId != null && membersMapper.existsById(loginId);

>>>>>>> branch 'master' of https://github.com/t-yamamori/fujichaku-team-app.git
		List<Reviews> rv = new ArrayList<>();
		List<Reservations> rs = new ArrayList<>();

		if (isMember) {
			// 会員 → 口コミ全件を表示する
			rv = reviewsMapper.selectAllReviews(st.getId());
			// 会員 → この店舗に対する自分の予約を取得する（reservations と stores をJOIN）
			rs = reservationMapper.selectByStoreIdAndMemberId(st.getId(), loginId);
		} else {
			// 非会員 → 口コミ1件のみ表示する
			Reviews one = reviewsMapper.selectOneReview(st.getId());
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