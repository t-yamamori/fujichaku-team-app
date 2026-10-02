package com.example.demo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.entity.Reviews;

@Mapper
public interface ReviewsMapper {
	
	List<Reviews> selectAllReviews(Integer storeId);
	Reviews selectOneReview(Integer storeId);
	
	   // 口コミ追加
    void addReview(
            @Param("id") Integer id,
            @Param("comment") String content
    );

    // 口コミ削除
    void deleteReview(
            @Param("id") Integer id
    );

    
}