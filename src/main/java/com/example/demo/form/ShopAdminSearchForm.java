package com.example.demo.form;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShopAdminSearchForm {

    private String id;
    private String storeId;
    private String name;
    private String mail;
    private LocalDate birthDate;
}