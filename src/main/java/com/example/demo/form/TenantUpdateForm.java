package com.example.demo.form;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;

import com.example.demo.entity.Stores;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TenantUpdateForm {

    @Valid
    private List<Stores> stores = new ArrayList<>();
}