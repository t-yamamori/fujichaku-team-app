package com.example.demo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.entity.Reviews;

@Mapper
public interface ReviewsMapper {

	// 店舗の口コミ全件（会員向け）
	List<Reviews> selectAllReviews(Integer storeId);

	// 店舗の最新口コミ1件（非会員向け）
	Reviews selectOneReview(Integer storeId);

	// 口コミ追加
	// 「自分の口コミだけ削除」できるように、会員ID(memberId)も一緒に登録する
	// Reviews の storeId / memberId / comment / grade を使う
	void addReview(Reviews review);

	// 口コミ削除（自分の口コミのみ）
	// 口コミID・店舗ID・会員IDがすべて一致したときだけ削除する
	// 戻り値：削除した件数（0なら削除できる口コミがなかった）
	int deleteReview(
			@Param("id") Integer id,
			@Param("storeId") Integer storeId,
			@Param("memberId") Integer memberId
	);

}

