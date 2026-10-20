package com.example.demo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.entity.Reviews;

@Mapper
public interface ReviewsMapper {

    // 店舗の口コミ全件（会員向け・論理削除済みを除外）
    List<Reviews> selectAllReviews(Integer storeId);

    // 店舗の最新口コミ1件（非会員向け・論理削除済みを除外）
    Reviews selectOneReview(Integer storeId);

    // 口コミ追加
    void addReview(Reviews review);

    // 口コミ削除（会員本人の口コミのみ・論理削除）
    // 戻り値：更新した件数
    int deleteReview(
            @Param("id") Integer id,
            @Param("storeId") Integer storeId,
            @Param("memberId") Integer memberId
    );

    // 口コミ削除（店舗管理者用・論理削除）
    // 戻り値：更新した件数
    int deleteByIdAndStoreId(
            @Param("id") Integer id,
            @Param("storeId") Integer storeId
    );

    // 口コミを1件取得（論理削除済みを除外）
    Reviews selectReviewById(Integer id);

    // 店舗の口コミ平均（論理削除済みを除外）
    Double selectAverageGrade(
            @Param("storeId") Integer storeId
    );

    // 店舗の口コミ件数（論理削除済みを除外）
    int countByStoreId(
            @Param("storeId") Integer storeId
    );
}