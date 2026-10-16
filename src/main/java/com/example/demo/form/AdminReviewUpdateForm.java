package com.example.demo.form;

import java.util.ArrayList;
import java.util.List;

import com.example.demo.entity.AdminReview;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminReviewUpdateForm {

    private List<AdminReview> reviews = new ArrayList<>();
}
