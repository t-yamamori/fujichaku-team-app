package com.example.demo.repository;

import java.util.List;

public interface ReviewsMapper {
	
	List<ReviewsMapper> selectAllReviews(Integer storeId);
	ReviewsMapper selectOneReview(Integer storeId);

}