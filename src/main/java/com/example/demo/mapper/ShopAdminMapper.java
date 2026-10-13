package com.example.demo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.entity.ShopAdmin;

@Mapper
public interface ShopAdminMapper {

    // IDから店舗管理者を取得
    ShopAdmin findById(int id);

    // 店舗管理者一覧取得
    List<ShopAdmin> findAll();

    // 店舗管理者更新
    void update(ShopAdmin shopAdmin);

    // 店舗管理者論理削除
    void deleteByIds(List<Integer> ids);

    // 店舗管理者新規登録
    void create(ShopAdmin shopAdmin);
}