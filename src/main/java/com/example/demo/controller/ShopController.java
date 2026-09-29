package com.example.demo.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.demo.entity.NewsDomestic;


@Controller
@RequestMapping("/shops")
public class ShopController {
	
	public String showShops() {
		List<Stores> 
		
		List<NewsDomestic> ndm = newsDomesticMapper.selectAll();
		model.addAttribute("news",ndm);
		return "shops";
	}
	
	

}
