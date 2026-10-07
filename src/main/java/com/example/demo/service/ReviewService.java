package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Reviews;
import com.example.demo.mapper.ReviewsMapper;

import lombok.RequiredArgsConstructor;

/*
 * 口コミ(reviews)に関する処理をまとめた Service
 *
 *   ShopController → ReviewService → ReviewsMapper(.java / .xml) → reviews テーブル
 *
 * Controller は ReviewsMapper を直接使わず、必ずこの Service を通してアクセスする。
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

	private final ReviewsMapper reviewsMapper;

	// 店舗の口コミ全件・新しい順（会員向け）
	// 店舗詳細画面(shops/detail.html)と口コミ一覧画面(reviews/reviews.html)の両方で使う
	public List<Reviews> findAllByStoreId(int storeId) {
		return reviewsMapper.selectAllReviews(storeId);
	}

	// 店舗の最新口コミ1件（非会員向け）。なければ null
	public Reviews findLatestByStoreId(int storeId) {
		return reviewsMapper.selectOneReview(storeId);
	}

	// 口コミ追加
	@Transactional
	public void addReview(int storeId, int memberId, String comment, int grade) {
		Reviews review = new Reviews();
		review.setStoreId(storeId);
		review.setMemberId(memberId);
		review.setComment(comment);
		review.setGrade(grade);
		reviewsMapper.addReview(review);
	}

	// 口コミ削除（自分の口コミのみ）
	// 口コミID・店舗ID・会員IDがすべて一致したときだけ削除される
	// 戻り値：削除できたら true、該当する口コミがなければ false
	@Transactional
	public boolean deleteReview(int reviewId, int storeId, int memberId) {
		return reviewsMapper.deleteReview(reviewId, storeId, memberId) > 0;
	}

}