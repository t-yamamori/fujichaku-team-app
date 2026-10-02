package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.mapper.ReviewMapper;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/shops")
public class ReviewController {

    private final ReviewMapper reviewMapper;

    /**
     * 口コミ追加
     */
    @PostMapping("/{shopId}/reviews/new")
    public String addReview(
            @PathVariable Integer shopId,
            @RequestParam String content) {

        reviewMapper.addReview(shopId, content);

        return "redirect:/shops/" + shopId;
    }

    /**
     * 口コミ削除
     */
    @PostMapping("/{shopId}/reviews/delete")
    public String deleteReview(
            @PathVariable Integer shopId,
            @RequestParam Integer reviewId) {

        reviewMapper.deleteReview(reviewId, shopId);

        return "redirect:/shops/" + shopId;
    }
    
    
    //口コミ一覧表示
    @
    
    
    
    
    
    
}
