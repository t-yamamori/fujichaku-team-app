package com.example.demo.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.entity.Stores;

@Mapper
public interface StoresMapper {

    // 店舗一覧
    List<Stores> selectList();

    // 店舗名検索
    List<Stores> selectByName(String name);

    // 店舗詳細
    Stores selectDetailByName(String name);

    // 店舗IDから取得
    Stores selectById(int id);
}