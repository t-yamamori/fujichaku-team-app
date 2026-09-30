package com.example.demo.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}