package com.example.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Members {

	private int id;
	private String name;
	private String mail;
    private String password;
    private int point;
    private boolean is_deleted;

} 