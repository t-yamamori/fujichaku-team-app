package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.AdminReview;
import com.example.demo.mapper.AdminReviewMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminReviewService {

    private final AdminReviewMapper adminReviewMapper;

    public List<AdminReview> findAllActive() {
        return adminReviewMapper.findAllActive();
    }

    @Transactional
    public void updateAll(List<AdminReview> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return;
        }

        for (AdminReview review : reviews) {
            if (review == null || review.getId() == null) {
                continue;
            }

            if (review.getStoreId() == null
                    || !existsActiveStore(review.getStoreId())) {
                throw new IllegalArgumentException(
                        "レビューID " + review.getId()
                        + " の店舗IDが存在しないか、削除済みです。");
            }

            if (review.getMemberId() == null
                    || !existsActiveMember(review.getMemberId())) {
                throw new IllegalArgumentException(
                        "レビューID " + review.getId()
                        + " の会員IDが存在しないか、削除済みです。");
            }

            if (review.getComment() == null
                    || review.getComment().isBlank()) {
                throw new IllegalArgumentException(
                        "レビューID " + review.getId()
                        + " のコメントが空欄です。");
            }

            if (review.getComment().length() > 2000) {
                throw new IllegalArgumentException(
                        "レビューID " + review.getId()
                        + " のコメントは2000文字以内で入力してください。");
            }

            if (review.getGrade() == null
                    || review.getGrade() < 1
                    || review.getGrade() > 5) {
                throw new IllegalArgumentException(
                        "レビューID " + review.getId()
                        + " の評価は1～5で入力してください。");
            }

            adminReviewMapper.update(review);
        }
    }

    @Transactional
    public int deleteByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return adminReviewMapper.deleteByIds(ids);
    }

    @Transactional
    public void create(AdminReview review) {
        if (!existsActiveStore(review.getStoreId())) {
            throw new IllegalArgumentException(
                    "指定された店舗IDが存在しないか、削除済みです。");
        }

        if (!existsActiveMember(review.getMemberId())) {
            throw new IllegalArgumentException(
                    "指定された会員IDが存在しないか、削除済みです。");
        }

        adminReviewMapper.create(review);
    }

    public boolean existsActiveStore(Integer storeId) {
        return storeId != null
                && adminReviewMapper.existsActiveStore(storeId);
    }

    public boolean existsActiveMember(Integer memberId) {
        return memberId != null
                && adminReviewMapper.existsActiveMember(memberId);
    }

    public String findActiveStoreName(Integer storeId) {
        if (!existsActiveStore(storeId)) {
            return null;
        }
        return adminReviewMapper.findActiveStoreName(storeId);
    }

    public String findActiveMemberName(Integer memberId) {
        if (!existsActiveMember(memberId)) {
            return null;
        }
        return adminReviewMapper.findActiveMemberName(memberId);
    }
}