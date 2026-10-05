package com.example.demo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.entity.Stores;

@Mapper
public interface TenantMapper {

    // 全テナント取得
    List<Stores> findAll();

    // テナント更新
    void update(Stores store);

    // テナント論理削除
    void deleteByIds(List<Integer> ids);

    // テナント新規登録
    void create(Stores store);
}