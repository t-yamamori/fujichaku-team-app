package com.example.demo.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.demo.entity.Stores;
import com.example.demo.repository.MembersMapper;
import com.example.demo.repository.ReviewsMapper;
import com.example.demo.repository.StoresMapper;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/shops")
public class ShopController {

	//フィールド
	private final StoresMapper storesMapper;
	private final MembersMapper membersMapper;
	private final ReviewsMapper reviewsMapper;

	//店舗一覧表示
	@GetMapping("")
	public String showShops(Model model) {
		List<Stores> st = storesMapper.selectList(); 
		model.addAttribute("stores",st);
		return "shops";
	}
	
	//店舗を検索
	@GetMapping("/search/{name}")
	public String searchShops() {
		return "shops/search";
	}

	//検索した店舗を表示
	@GetMapping("/search/{name}")
	public String showSearchResults(@PathVariable String name,Model model) {
		List<Stores> st = storesMapper.selectByName(name);
		model.addAttribute("stores",st);
		model.addAttribute("name", name);
		return "shops/search";
	}

	@GetMapping("/{name}")
	public String showDetail(@PathVariable String name,Model model) {
         
		Stores st = storesMapper.selectDetailByName(name);
		
		//★★★★★試しに置いているloginIdなので完成までに削除すること★★★★★
		int loginId = 123;
		
		// Membersテーブルから全員のIDを取得
		List<Integer> memberIds = membersMapper.selectAllIds();

		List<ReviewsMapper> rv = new ArrayList<>();
		if (loginId != null && memberIds.contains(loginId)) {
			// loginId が Members のIDの中にある → 口コミ全件
			rv = reviewsMapper.selectAllReviews(st.getId());
		} else {
			// ない（または null）→ 口コミ1件
			ReviewsMapper one = reviewsMapper.selectOneReview(st.getId());
			if (one != null) {
				rv.add(one);
			}
		}

		model.addAttribute("stores", st);
		model.addAttribute("reviews", rv);
		return "shops/detail";
	}

}