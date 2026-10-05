package com.example.demo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.entity.Reviews;

@Mapper
public interface ReviewsMapper {
	
	//店舗の口コミ全件（新しい順）
	List<Reviews> selectAllReviews(@Param("storeId")Integer storeId);
	
	//店舗の最新口コミ1件取得
	Reviews selectOneReview(@Param("storeId")Integer storeId);
	
	// 口コミ追加
	void addReview(
            @Param("storeId") Integer storeId,
            @Param("memberId") Integer memberId,
            @Param("comment") String content,
            @Param("grade") Integer grade);

    // 口コミ削除
	void deleteReview(
            @Param("id") Integer id,
            @Param("memberId") Integer memberId);

    
}