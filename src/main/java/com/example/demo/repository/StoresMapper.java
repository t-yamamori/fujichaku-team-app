package com.example.demo.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.entity.Stores;

@Mapper
public interface StoresMapper {

	 //店舗一覧表示
     List<Stores> selectList();
     
     //店舗を名前で検索
     List<Stores> selectByName(String name);
     
     //店舗を名前で検索して詳細表示
     Stores selectDetailByName(String name);
}