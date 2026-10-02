package com.example.demo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.entity.Reviews;

@Mapper
public interface ReviewMapper {

    // 口コミ追加
    void addReview(
            @Param("shopId") Integer shopId,
            @Param("content") String content
    );

    // 口コミ削除
    void deleteReview(
            @Param("reviewId") Integer reviewId,
            @Param("shopId") Integer shopId
    );
    
    List<Reviews> selectAllReviews(Integer storeId);
	Reviews selectOneReview(Integer storeId);
}