package com.example.demo.entity;

import java.time.LocalDateTime;

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
    private String comment;
    private LocalDateTime createdAt;
    private Integer grade;
    private Boolean isDeleted;
}
