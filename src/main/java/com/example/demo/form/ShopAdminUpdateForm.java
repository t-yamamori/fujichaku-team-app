package com.example.demo.form;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;

import com.example.demo.entity.ShopAdmin;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShopAdminUpdateForm {

    @Valid
    private List<ShopAdmin> shopAdmins = new ArrayList<>();
}