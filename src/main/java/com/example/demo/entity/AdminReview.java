package com.example.demo.entity;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminReview {

    private Integer id;
    private Integer storeId;
    private String storeName;
    private Integer memberId;
    private String memberName;
    @Size(max = 2000, message = "口コミは2000文字以内で入力してください。")
    private String comment;
    private LocalDateTime createdAt;
    private Integer grade;
    private Boolean isDeleted;
}
