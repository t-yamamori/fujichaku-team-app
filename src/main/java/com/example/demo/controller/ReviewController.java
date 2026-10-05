package com.example.demo.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.entity.Reviews;
import com.example.demo.mapper.ReviewsMapper;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewsMapper reviewsMapper;

    /*
     * 口コミ一覧表示
     */
    @GetMapping("/shops/{shopId}/reviews")
    public String showReviews(
            @PathVariable Integer shopId,
            Model model) {

        List<Reviews> reviews = reviewsMapper.selectAllReviews(shopId);

        model.addAttribute("reviews", reviews);
        model.addAttribute("shopId", shopId);

        return "reviews/reviews";
    }
    
    /**
     * 口コミ追加
     */
    @PostMapping("/shops/{shopId}/reviews/new")
    public String addReview(
            @PathVariable Integer shopId,
            @RequestParam String content,
            @RequestParam Integer grade,
            RedirectAttributes redirectAttributes) {

        reviewsMapper.addReview(shopId, content,grade);
        
     // 空の口コミは登録しない
        if (content.isBlank()) {
            redirectAttributes.addFlashAttribute("reviewError", "口コミを入力してください");
            return "redirect:/shops/" + shopId;
        }

        reviewsMapper.addReview(shopId, content, grade);

        return "redirect:/shops/" + shopId;

    }

    /**
     * 口コミ削除
     */
    @PostMapping("/shops/{shopId}/reviews/delete")
    public String deleteReview(
            @PathVariable Integer shopId,
            @RequestParam Integer reviewId) {

        reviewsMapper.deleteReview(reviewId, loginMember.getId());

        return "redirect:/shops/" + shopId;
    }

    
}

