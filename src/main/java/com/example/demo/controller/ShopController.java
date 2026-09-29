package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class ShopController {
	
	@GetMapping("/")
	public String showShops() {
		
		return "shops";
	}
	
	

}
