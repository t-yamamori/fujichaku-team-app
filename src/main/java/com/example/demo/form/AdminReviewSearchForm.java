package com.example.demo.form;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminReviewSearchForm {

    private String id;
    private String storeId;
    private String storeName;
    private String memberId;
    private String memberName;
    private String comment;
    private String grade;
    private String createdAt;
}
