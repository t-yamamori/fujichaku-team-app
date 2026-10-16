package com.example.demo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.entity.AdminReview;

@Mapper
public interface AdminReviewMapper {

    List<AdminReview> findAllActive();

    int update(@Param("review") AdminReview review);

    int deleteByIds(@Param("ids") List<Integer> ids);

    int create(@Param("review") AdminReview review);

    boolean existsActiveStore(@Param("storeId") Integer storeId);

    boolean existsActiveMember(@Param("memberId") Integer memberId);

    String findActiveStoreName(@Param("storeId") Integer storeId);

    String findActiveMemberName(@Param("memberId") Integer memberId);
}